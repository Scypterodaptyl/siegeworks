package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlockImpact;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class TrebuchetProjectile extends SiegeProjectile {
    private static final String TAG_IMPACT_MODE = "ImpactMode";
    private static final String TAG_CLOUD_DURATION = "CloudDuration";
    private static final String TAG_TEXTURE_NAME = "TextureName";
    private static final String TAG_STATUS_EFFECT = "StatusEffect";

    private ImpactMode impactMode = ImpactMode.BREAK_BLOCKS;
    private MobEffectInstance statusEffectInstance;
    private int cloudDuration;
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
        tag.putString(TAG_IMPACT_MODE, impactMode.name());
        tag.putInt(TAG_CLOUD_DURATION, cloudDuration);
        tag.putString(TAG_TEXTURE_NAME, getTextureName());
        if (statusEffectInstance != null) {
            tag.put(TAG_STATUS_EFFECT,
                    //? if forge {
                    /*statusEffectInstance.save(new CompoundTag())
                    *///?} else {
                    statusEffectInstance.save()
                    //?}
            );
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_IMPACT_MODE)) {
            try {
                impactMode = ImpactMode.valueOf(tag.getString(TAG_IMPACT_MODE));
            } catch (IllegalArgumentException ignored) {
                impactMode = ImpactMode.BREAK_BLOCKS;
            }
        }
        if (tag.contains(TAG_CLOUD_DURATION)) {
            cloudDuration = tag.getInt(TAG_CLOUD_DURATION);
        }
        if (tag.contains(TAG_TEXTURE_NAME)) {
            setTextureName(tag.getString(TAG_TEXTURE_NAME));
        }
        if (tag.get(TAG_STATUS_EFFECT) instanceof CompoundTag effectTag) {
            statusEffectInstance = MobEffectInstance.load(effectTag);
        }
    }

    public TrebuchetProjectile(EntityType<? extends TrebuchetProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    public TrebuchetProjectile(EntityType<TrebuchetProjectile> cannonProjectile, LivingEntity shooter, Level level) {
        super(cannonProjectile, shooter, level);
    }

    public void setImpactMode(ImpactMode mode) {
        this.impactMode = mode;
    }

    public ImpactMode getImpactMode() {
        return impactMode;
    }

    @Override
    public void tick() {
        super.tick();

    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        int baseRadius = Math.max(1, (int) (getBaseDamage() / 8));
        Level world = this.level();
        if (!(world instanceof ServerLevel serverLevel) || !(this.getOwner() instanceof AbstractSiegeEntity)) return;
        double speed = getDeltaMovement().length();

        if (getImpactMode() == ImpactMode.BREAK_BLOCKS
                && !physics.impactFuse()
                && tryKineticBlockPenetration(blockHitResult, physics)) {
            return;
        }

        boolean impactReportHandled = false;
        switch (getImpactMode()) {
            case BREAK_BLOCKS -> impactReportHandled = handleBlockBreaking(blockHitResult, serverLevel, baseRadius, physics, speed);
            case SPREAD_FIRE -> handleSpreadFire(serverLevel, blockHitResult.getLocation(), blockHitResult.getBlockPos(), baseRadius);
            case SPREAD_EFFECT -> handleSpreadEffect(blockHitResult, serverLevel, baseRadius);
        }

        if (!impactReportHandled) {
            playImpactReport(serverLevel, blockHitResult.getLocation(), 4.5f,
                    getImpactMode() == ImpactMode.BREAK_BLOCKS ? 0.75f : 0.95f);
        }
        this.inGround = false;
        this.shakeTime = 0;
        this.setImpactSound(SoundEvents.ARROW_HIT);
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        int baseRadius = Math.max(1, (int) (getBaseDamage() / 8));
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);

        if (this.level() instanceof ServerLevel serverLevel && getImpactMode() == ImpactMode.SPREAD_FIRE) {
            Vec3 impact = entityHitResult.getLocation();
            handleSpreadFire(serverLevel, impact, BlockPos.containing(impact), baseRadius);
            this.discard();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel && target != null) {
            double speed = getDeltaMovement().length();
            float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
            boolean directDamageApplied = damageTarget(hitTarget, damage);
            if (physics.baseExplosionPower() > 0.0f) {
                ProjectileBlastResolver.applyControlledExplosion(serverLevel, entityHitResult.getLocation(), this, getOwner(),
                        physics, speed, (float) getBaseDamage(), ProjectileImpactEffects.Style.TREBUCHET,
                        directDamageApplied ? target : null);
            } else {
                playImpactReport(serverLevel, entityHitResult.getLocation(), 4.0f, 0.85f);
            }

            if (!physics.impactFuse() && tryKineticEntityPenetration(target, physics, speed)) {
                return;
            }
        }

        this.discard();
    }

    private boolean handleBlockBreaking(BlockHitResult blockHitResult, ServerLevel serverLevel, int baseRadius,
                                        ProjectilePhysicsProfile physics, double speed) {
        damageImpactBlocks(blockHitResult, serverLevel, baseRadius, 1.0f);

        BlockState blockState = this.level().getBlockState(blockHitResult.getBlockPos());
        blockState.onProjectileHit(this.level(), blockState, blockHitResult, this);
        Vec3 vec3d = blockHitResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3d);
        Vec3 vec3d2 = vec3d.normalize().scale(0.05F);
        this.setPos(this.getX() - vec3d2.x, this.getY() - vec3d2.y, this.getZ() - vec3d2.z);

        if (physics.baseExplosionPower() <= 0.0f) {
            return false;
        }

        ProjectileBlastResolver.applyControlledExplosion(serverLevel, blockHitResult.getLocation(), this, getOwner(),
                physics, speed, (float) getBaseDamage(), ProjectileImpactEffects.Style.TREBUCHET);
        return true;
    }

    private void damageImpactBlocks(BlockHitResult blockHitResult, ServerLevel serverLevel, int baseRadius, float damageScale) {
        Vec3 center = blockHitResult.getLocation();
        double radius = Math.max(1.35, Math.min(3.8, baseRadius * 0.72));
        float centerDamage = (float) Math.max(0.3, getBaseDamage() / 44.0) * damageScale;
        ProjectileBlockImpact.damageBlocksInSphere(serverLevel, center, radius, centerDamage,
                SiegeBlockBreaker.responsiblePlayer(getOwner()));
    }

    private void handleSpreadFire(ServerLevel serverLevel, Vec3 impact, BlockPos center, int baseRadius) {
        int fireRadius = Mth.clamp(baseRadius + 2, 4, 8);
        double entityRadius = fireRadius + 1.5D;
        float centerDamage = Math.max(7.0F, (float) getBaseDamage() * 0.48F);

        for (BlockPos pos : BlockPos.withinManhattan(center, fireRadius, fireRadius, fireRadius)) {
            if (!serverLevel.getBlockState(pos).isAir()) continue;

            if (!Blocks.FIRE.defaultBlockState().canSurvive(serverLevel, pos)) {
                continue;
            }

            double distance = Math.sqrt(pos.distSqr(center));
            float chance = (float) Mth.clamp(0.72D - distance * 0.075D, 0.18D, 0.72D);
            if (serverLevel.random.nextFloat() < chance) {
                serverLevel.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(entityRadius), LivingEntity::isAlive)) {
            if (target == getOwner()) {
                continue;
            }

            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(impact);
            if (distance > entityRadius) {
                continue;
            }

            float falloff = (float) Mth.clamp(1.0D - distance / entityRadius, 0.2D, 1.0D);
            damageLivingTarget(target, centerDamage * falloff, false);
            int fireTicks = Math.max(target.getRemainingFireTicks(), Mth.floor((6.0F + 12.0F * falloff) * 20.0F));
            target.setRemainingFireTicks(fireTicks);
        }

        serverLevel.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 4.0F, 0.65F + random.nextFloat() * 0.15F);
        SiegeParticleEffects.incendiaryImpact(serverLevel, impact, fireRadius);
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

    private boolean tryKineticBlockPenetration(BlockHitResult blockHitResult, ProjectilePhysicsProfile physics) {
        Level world = this.level();
        if (!(world instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }

        double speed = getDeltaMovement().length();
        double nextSpeed = ProjectilePhysics.remainingBlockPenetrationSpeed(world, pos, state, physics, speed, 1.0D);
        if (nextSpeed <= 0.0D) {
            return false;
        }

        Vec3 direction = getDeltaMovement().normalize();
        ProjectileBlockImpact.carvePenetrationTunnel(serverLevel, pos, direction, physics,
                (float) getBaseDamage(), speed, SiegeBlockBreaker.responsiblePlayer(getOwner()));
        state.onProjectileHit(world, state, blockHitResult, this);
        if (!serverLevel.getBlockState(pos).isAir()) {
            SiegeBlockBreaker.breakBlock(serverLevel, pos, SiegeBlockBreaker.responsiblePlayer(getOwner()));
        }

        setDeltaMovement(direction.scale(nextSpeed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (nextSpeed / speed)));
        Vec3 impact = blockHitResult.getLocation();
        setPos(impact.x + direction.x * 0.55, impact.y + direction.y * 0.55, impact.z + direction.z * 0.55);

        ProjectileImpactEffects.playPenetrationReport(serverLevel, impact, 2.0f, 1.1f);
        return true;
    }

    private boolean tryKineticEntityPenetration(LivingEntity target, ProjectilePhysicsProfile physics, double speed) {
        double nextSpeed = ProjectilePhysics.remainingEntityPenetrationSpeed(physics, target, speed);
        if (nextSpeed <= 0.0D) {
            return false;
        }

        Vec3 direction = getDeltaMovement().normalize();
        setDeltaMovement(direction.scale(nextSpeed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (nextSpeed / speed)));
        setPos(getX() + direction.x * 0.35, getY() + direction.y * 0.35, getZ() + direction.z * 0.35);
        return true;
    }

    private void handleSpreadEffect(BlockHitResult hitResult, ServerLevel serverLevel, int baseRadius) {
        if (statusEffectInstance == null) return;

        Vec3 center = Vec3.atCenterOf(hitResult.getBlockPos());

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, center.x, center.y, center.z);
        cloud.setRadius(baseRadius);
        cloud.setRadiusOnUse(-0.25F);
        cloud.setWaitTime(0);
        cloud.setDuration(getCloudDuration());
        cloud.setRadiusPerTick(-0.0001F);
        cloud.addEffect(getStatusEffectInstance());

        serverLevel.addFreshEntity(cloud);
    }

    public MobEffectInstance getStatusEffectInstance() {
        return statusEffectInstance;
    }

    public void setStatusEffectInstance(MobEffectInstance statusEffectInstance) {
        this.statusEffectInstance = statusEffectInstance;
    }

    public int getCloudDuration() {
        return cloudDuration;
    }

    public void setCloudDuration(int cloudDuration) {
        this.cloudDuration = cloudDuration;
    }

    public enum ImpactMode {
        BREAK_BLOCKS,
        SPREAD_FIRE,
        SPREAD_EFFECT
    }
}
