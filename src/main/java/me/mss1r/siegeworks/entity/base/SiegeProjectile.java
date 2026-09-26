package me.mss1r.siegeworks.entity.base;

import me.mss1r.siegeworks.gameplay.damage.SiegeProjectileCombat;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileSweep;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.axiomata.collision.CollidableStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class SiegeProjectile extends ThrowableItemProjectile {
    private static final double RENDER_DISTANCE = 160.0D;

    private static final String TAG_BASE_DAMAGE = "BaseDamage";
    private static final String TAG_IN_GROUND = "InGround";
    private static final String TAG_SHAKE_TIME = "ShakeTime";

    private boolean applyingPredictedPhysics;
    private boolean hitEntityThisTick;
    protected boolean inGround;
    protected int shakeTime;
    private double baseDamage = 2.0D;
    private SoundEvent hitSound = SoundEvents.ARROW_HIT;

    public SiegeProjectile(EntityType<? extends SiegeProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        double range = RENDER_DISTANCE * getViewScale();
        return distanceSquared < range * range;
    }

    public SiegeProjectile(EntityType<? extends SiegeProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STONE;
    }

    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble(TAG_BASE_DAMAGE, baseDamage);
        tag.putBoolean(TAG_IN_GROUND, inGround);
        tag.putInt(TAG_SHAKE_TIME, shakeTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_BASE_DAMAGE)) {
            baseDamage = tag.getDouble(TAG_BASE_DAMAGE);
        }
        if (tag.contains(TAG_IN_GROUND)) {
            inGround = tag.getBoolean(TAG_IN_GROUND);
        }
        if (tag.contains(TAG_SHAKE_TIME)) {
            shakeTime = tag.getInt(TAG_SHAKE_TIME);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!SiegeProjectileCombat.mayHit(this, target)) {
            return false;
        }
        if (target instanceof CollidableStructure structure && !structure.collisionGroups().isEmpty()) {
            return true;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            hitEntityThisTick = true;
        }
        super.onHit(hitResult);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (entityHitResult.getEntity().level().isClientSide()) return;
        if (entityHitResult.getEntity() instanceof LivingEntity target) {
            damageLivingTarget(target, (float) this.getBaseDamage());
        }
        setDeltaMovement(getDeltaMovement().scale(-0.9));
        setBaseDamage(getBaseDamage() * 0.9);
        if (getBaseDamage() <= 1) this.discard();
    }

    @Override
    public void tick() {
        Vec3 previousPos = position();
        Vec3 previousMovement = getDeltaMovement();
        hitEntityThisTick = false;

        if (this.level().isClientSide && !shouldPredictMotionOnClient()) {
            this.xo = getX();
            this.yo = getY();
            this.zo = getZ();
            super.baseTick();
            return;
        }

        applyingPredictedPhysics = true;
        try {
            super.tick();
            if (!this.inGround && !this.isRemoved()) {
                applyProjectileDrag();
            }
        } finally {
            applyingPredictedPhysics = false;
        }

        if (!this.inGround && !this.isRemoved() && this.level() instanceof ServerLevel serverLevel) {
            sweepMissedEntityHit(serverLevel, previousPos, position(), previousMovement);
        }
    }

    protected boolean shouldPredictMotionOnClient() {
        return false;
    }

    private void applyProjectileDrag() {
        double drag = getPhysicsProfile().drag();
        if (drag <= 0.0) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() <= 1.0E-7D) {
            return;
        }

        super.setDeltaMovement(movement.scale(Math.max(0.0, 1.0 - drag)));
    }

    @Override
    public void setDeltaMovement(Vec3 deltaMovement) {
        super.setDeltaMovement(deltaMovement);
        if (!level().isClientSide && tickCount > 0 && !applyingPredictedPhysics) {
            markMotionDirty();
        }
    }

    protected void markMotionDirty() {
        this.hasImpulse = true;
    }

    protected boolean sweepMissedEntityHit(ServerLevel serverLevel, Vec3 previousPos, Vec3 currentPos, Vec3 previousMovement) {
        if (hitEntityThisTick || !shouldSupplementalEntitySweep()) {
            return false;
        }

        EntityHitResult hit = ProjectileSweep.findFirstEntityHit(serverLevel, this, previousPos, currentPos,
                previousMovement, this::canHitEntity, getSupplementalEntitySweepPadding());
        if (hit == null) {
            return false;
        }

        hitEntityThisTick = true;
        onHitEntity(hit);
        return true;
    }

    protected boolean shouldSupplementalEntitySweep() {
        return true;
    }

    protected double getSupplementalEntitySweepPadding() {
        return Math.max(0.35D, getBbWidth() * 0.5D);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    protected void setImpactSound(SoundEvent soundEvent) {
        this.hitSound = soundEvent;
    }

    protected SoundEvent getImpactSound() {
        return hitSound != null ? hitSound : SoundEvents.ANVIL_LAND;
    }

    protected ProjectilePhysicsProfile getPhysicsProfile() {
        return SiegeProfileCatalogs.PROJECTILES.forEntity(this.getType());
    }

    protected boolean damageLivingTarget(LivingEntity target, float damage) {
        return damageLivingTarget(target, damage, true);
    }

    protected boolean damageLivingTarget(LivingEntity target, float damage, boolean breakShield) {
        return SiegeProjectileCombat.damage(this, target, damage, breakShield);
    }
}
