package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** The load of a mangonel or trebuchet: a stone, or a fire pot when its profile carries fire. */
public class TrebuchetProjectile extends SiegeProjectile {
    private static final String TAG_TEXTURE_NAME = "TextureName";
    private static final int BURN_SECONDS = 12;

    protected static final EntityDataAccessor<String> TEXTURE_NAME;

    static {
        TEXTURE_NAME = SynchedEntityData.defineId(TrebuchetProjectile.class, EntityDataSerializers.STRING);
    }

    @Override
    //? if forge {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(TEXTURE_NAME, "");
    }

    public void setTextureName(String textureName) {
        this.entityData.set(TEXTURE_NAME, textureName);
    }

    public String getTextureName() {
        return this.entityData.get(TEXTURE_NAME);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_TEXTURE_NAME, getTextureName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_TEXTURE_NAME)) {
            setTextureName(tag.getString(TAG_TEXTURE_NAME));
        }
    }

    public TrebuchetProjectile(EntityType<? extends TrebuchetProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    public TrebuchetProjectile(EntityType<TrebuchetProjectile> cannonProjectile, LivingEntity shooter, Level level) {
        super(cannonProjectile, shooter, level);
    }

    private boolean isIncendiary() {
        return getPhysicsProfile().fire().radius() > 0.0D;
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isIncendiary()) {
            burst(serverLevel, blockHitResult.getLocation(), getDeltaMovement().length());
            return;
        }

        ProjectileImpacts.Drive drive = driveInto(serverLevel, blockHitResult);
        if (drive.passedThrough()) {
            ProjectileImpactEffects.playPenetrationReport(serverLevel, blockHitResult.getLocation(), 2.0f, 1.1f);
            ProjectileImpactEffects.spawnPenetrationParticles(serverLevel, blockHitResult.getLocation(),
                    getPhysicsProfile().diameterOf(this), getDeltaMovement().normalize().reverse());
            return;
        }
        arrive(serverLevel, drive.position(), drive.speed());
        playImpactReport(serverLevel, drive.position(), 4.5f, 0.75f);
        ProjectileBlastResolver.applyShock(serverLevel, drive.position(), this, getPhysicsProfile(), null,
                drive.speed());
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double speed = getDeltaMovement().length();
        if (isIncendiary()) {
            burst(serverLevel, entityHitResult.getLocation(), speed);
            return;
        }

        if (target != null) {
            ProjectilePhysicsProfile physics = getPhysicsProfile();
            float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
            boolean directDamageApplied = damageTarget(hitTarget, damage);
            playImpactReport(serverLevel, entityHitResult.getLocation(), 4.0f, 0.85f);
            ProjectileBlastResolver.applyShock(serverLevel, entityHitResult.getLocation(), this, physics,
                    directDamageApplied ? target : null, speed);
            if (tryKineticEntityPenetration(target, speed)) {
                return;
            }
        }
        this.discard();
    }

    /** A fire pot breaking open: it spreads fire and sets alight whoever its splash reaches. */
    private void burst(ServerLevel serverLevel, Vec3 impact, double speed) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        arrive(serverLevel, impact, speed);
        for (LivingEntity burned : ProjectileBlastResolver.applyShock(serverLevel, impact, this, physics, null, speed)) {
            burned.setRemainingFireTicks(Math.max(burned.getRemainingFireTicks(), BURN_SECONDS * 20));
        }
        serverLevel.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 4.0F, 0.65F + random.nextFloat() * 0.15F);
        SiegeParticleEffects.incendiaryImpact(serverLevel, impact, Mth.ceil(physics.fire().radius()));
        this.discard();
    }

    @Override
    protected SoundEvent getImpactSound() {
        //? if forge {
        /*return SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        return SoundEvents.GENERIC_EXPLODE.value();
        //?}
    }

    private void playImpactReport(ServerLevel serverLevel, Vec3 impact, float volume, float pitch) {
        ProjectileImpactEffects.playImpactReport(serverLevel, impact, volume, pitch,
                ProjectileImpactEffects.Style.TREBUCHET,
                getDeltaMovement().lengthSqr() > 1.0E-8D
                        ? getDeltaMovement().normalize().reverse()
                        : new Vec3(0.0D, 1.0D, 0.0D));
    }

    private boolean tryKineticEntityPenetration(LivingEntity target, double speed) {
        double nextSpeed = ProjectilePhysics.remainingEntityPenetrationSpeed(this, target, speed);
        if (nextSpeed <= 0.0D) {
            return false;
        }

        Vec3 direction = getDeltaMovement().normalize();
        setDeltaMovement(direction.scale(nextSpeed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (nextSpeed / speed)));
        setPos(getX() + direction.x * 0.35, getY() + direction.y * 0.35, getZ() + direction.z * 0.35);
        return true;
    }
}
