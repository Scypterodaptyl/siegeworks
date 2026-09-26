package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlockImpact;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileSweep;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class CannonProjectile extends SiegeProjectile {
    private static final String TAG_BREAKS_BLOCKS = "BreaksBlocks";
    private boolean shouldBreakBlocks = true;
    private Vec3 lastFlightMovement = Vec3.ZERO;

    public CannonProjectile(EntityType<? extends CannonProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    public CannonProjectile(EntityType<? extends CannonProjectile> cannonProjectile, LivingEntity shooter, Level level) {
        super(cannonProjectile, shooter, level);
    }

    public boolean shouldBreakBlocks() {
        return shouldBreakBlocks;
    }

    public void setShouldBreakBlocks(boolean shouldBreakBlocks) {
        this.shouldBreakBlocks = shouldBreakBlocks;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_BREAKS_BLOCKS, shouldBreakBlocks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_BREAKS_BLOCKS)) {
            shouldBreakBlocks = tag.getBoolean(TAG_BREAKS_BLOCKS);
        }
    }

    @Override
    public void tick() {
        Vec3 previousPos = position();
        Vec3 previousMovement = getDeltaMovement();
        if (previousMovement.lengthSqr() > 1.0E-6D) {
            lastFlightMovement = previousMovement;
        }

        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (isRemoved()) {
                return;
            }

            if (this.inGround) {
                resolveEmbeddedProjectile(serverLevel);
                return;
            }

            if (sweepMissedBlockHit(serverLevel, previousPos, position(), previousMovement)) {
                return;
            }
        }
    }

    private boolean sweepMissedBlockHit(ServerLevel serverLevel, Vec3 previousPos, Vec3 currentPos, Vec3 previousMovement) {
        return ProjectileSweep.hitFirstBlockingBlock(serverLevel, previousPos, currentPos, previousMovement,
                this::shouldIgnoreSweepBlock, this::onHitBlock);
    }

    private boolean shouldIgnoreSweepBlock(Level world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty()) {
            return true;
        }
        return state.getCollisionShape(world, pos).isEmpty() && state.getDestroySpeed(world, pos) <= 0.05F;
    }

    private void resolveEmbeddedProjectile(ServerLevel serverLevel) {
        BlockPos pos = findEmbeddedBlock(serverLevel);
        if (pos == null) {
            discard();
            return;
        }

        Vec3 direction = lastFlightMovement.lengthSqr() > 1.0E-6D ? lastFlightMovement.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Direction face = Direction.getNearest(-direction.x, -direction.y, -direction.z);
        Vec3 impact = position();
        this.inGround = false;
        this.shakeTime = 0;
        onKineticPenetrationStopped(new BlockHitResult(impact, face, pos, false), getPhysicsProfile(), lastFlightMovement.length());
    }

    private BlockPos findEmbeddedBlock(ServerLevel serverLevel) {
        BlockPos center = blockPosition();
        if (!shouldIgnoreSweepBlock(serverLevel, center, serverLevel.getBlockState(center))) {
            return center;
        }

        Vec3 direction = lastFlightMovement.lengthSqr() > 1.0E-6D ? lastFlightMovement.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        for (int step = 1; step <= 4; step++) {
            Vec3 sample = position().subtract(direction.scale(step * 0.25D));
            BlockPos pos = BlockPos.containing(sample);
            if (!shouldIgnoreSweepBlock(serverLevel, pos, serverLevel.getBlockState(pos))) {
                return pos;
            }
        }
        return null;
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();

        if (!shouldBreakBlocks()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                Vec3 impact = blockHitResult.getLocation();
                playImpactReport(serverLevel, impact, 3.5f, 1.0f);
            }

            this.discard();
            return;
        }

        if (!physics.impactFuse() && tryKineticBlockPenetration(blockHitResult, physics)) {
            return;
        }

        handleTerminalBlockImpact(blockHitResult, physics, 1.0f);
    }

    protected void handleTerminalBlockImpact(BlockHitResult blockHitResult, ProjectilePhysicsProfile physics, float damageScale) {
        double speed = getDeltaMovement().length();
        if (!applyImpactBlockDamage(blockHitResult, damageScale)) {
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 impact = blockHitResult.getLocation();
            playImpactReport(serverLevel, impact, 5.0f, 0.85f);
            applyImpactShockDamage(serverLevel, impact, physics, null,
                    physics.scaledShockRadius(speed), physics.scaledShockDamageMultiplier(speed));
        }

        this.inGround = false;
        this.shakeTime = 0;
        this.setImpactSound(SoundEvents.ARROW_HIT);
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();

        if (this.level() instanceof ServerLevel serverLevel && entityHitResult.getEntity() instanceof LivingEntity target) {
            Vec3 impact = entityHitResult.getLocation();
            double speed = getDeltaMovement().length();
            float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
            boolean directDamageApplied = damageLivingTarget(target, damage);
            playImpactReport(serverLevel, impact, 4.5f, 0.9f);
            if (shouldBreakBlocks()) {
                applyImpactShockDamage(serverLevel, impact, physics,
                        directDamageApplied ? target : null,
                        physics.scaledShockRadius(speed), physics.scaledShockDamageMultiplier(speed));
            }

            if (!physics.impactFuse() && tryKineticEntityPenetration(target, physics, speed)) {
                return;
            }
        }

        this.discard();
    }

    protected boolean applyImpactBlockDamage(BlockHitResult blockHitResult, float damageScale) {
        if (!damageImpactBlocks(blockHitResult, damageScale)) {
            return false;
        }

        BlockState blockState = this.level().getBlockState(blockHitResult.getBlockPos());
        blockState.onProjectileHit(this.level(), blockState, blockHitResult, this);
        Vec3 vec3d = blockHitResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3d);
        Vec3 vec3d2 = vec3d.normalize().scale(0.05F);
        this.setPos(this.getX() - vec3d2.x, this.getY() - vec3d2.y, this.getZ() - vec3d2.z);

        return true;
    }

    protected boolean damageImpactBlocks(BlockHitResult blockHitResult, float damageScale) {
        return ProjectileBlockImpact.damageImpactBlocks(this, blockHitResult, getPhysicsProfile(), damageScale);
    }

    protected double getImpactBlockDamageRadius(ProjectilePhysicsProfile physics) {
        return ProjectileBlockImpact.impactBlockDamageRadius(this, physics);
    }

    protected void damageBlocksInSphere(ServerLevel serverLevel, Vec3 center, double radius, float centerDamage) {
        ProjectileBlockImpact.damageBlocksInSphere(serverLevel, center, radius, centerDamage,
                SiegeBlockBreaker.responsiblePlayer(getOwner()));
    }

    protected boolean tryKineticBlockPenetration(BlockHitResult blockHitResult, ProjectilePhysicsProfile physics) {
        Level world = this.level();
        if (!(world instanceof ServerLevel serverLevel)) {
            return false;
        }

        double speed = getDeltaMovement().length();
        if (speed < physics.minContinueSpeed()) {
            return false;
        }
        double originalSpeed = speed;

        Vec3 direction = getDeltaMovement().normalize();
        Direction impactFace = blockHitResult.getDirection();
        Vec3 entry = blockHitResult.getLocation();
        BlockPos currentPos;
        BlockPos previousSampledPos = null;
        BlockHitResult currentHit = blockHitResult;
        Vec3 exitPoint = entry;
        boolean penetratedAny = false;
        boolean exitedSolidMass = false;
        int airSamplesAfterPenetration = 0;
        int solidBlocksChecked = 0;
        int maxSolidBlocks = calculateMaxPenetrationBlocks(physics, speed);
        double maxTraceDistance = calculatePenetrationTraceDistance(physics, speed);
        double sampleStep = 0.22D;

        for (double distance = 0.04D; distance <= maxTraceDistance && solidBlocksChecked < maxSolidBlocks; distance += sampleStep) {
            Vec3 samplePos = entry.add(direction.scale(distance));
            currentPos = distance <= sampleStep ? blockHitResult.getBlockPos() : BlockPos.containing(samplePos);
            if (currentPos.equals(previousSampledPos)) {
                continue;
            }
            previousSampledPos = currentPos;
            currentHit = currentPos.equals(blockHitResult.getBlockPos())
                    ? blockHitResult
                    : createPenetrationHit(currentPos, direction, impactFace);

            BlockState state = world.getBlockState(currentPos);
            if (isPassThroughBlock(world, currentPos, state)) {
                if (penetratedAny) {
                    exitPoint = samplePos;
                    airSamplesAfterPenetration++;
                    if (airSamplesAfterPenetration >= 2) {
                        exitedSolidMass = true;
                        break;
                    }
                }
                continue;
            }
            airSamplesAfterPenetration = 0;
            solidBlocksChecked++;

            double pathLength = ProjectilePhysics.blockPathLength(entry, direction, currentPos);
            double thicknessMultiplier = Math.max(0.12D, Math.min(1.75D, pathLength));
            double nextSpeed = ProjectilePhysics.remainingBlockPenetrationSpeed(world, currentPos, state,
                    physics, speed, thicknessMultiplier);
            if (nextSpeed <= 0.0D) {
                if (penetratedAny) {
                    onKineticPenetrationStopped(currentHit, physics, speed);
                    return true;
                }
                return false;
            }

            ProjectileBlockImpact.carvePenetrationTunnel(serverLevel, currentPos, direction, physics,
                    (float) getBaseDamage(), speed, SiegeBlockBreaker.responsiblePlayer(getOwner()));
            state.onProjectileHit(world, state, currentHit, this);
            if (!serverLevel.getBlockState(currentPos).isAir()) {
                SiegeBlockBreaker.breakBlock(serverLevel, currentPos, SiegeBlockBreaker.responsiblePlayer(getOwner()));
            }

            Vec3 impact = currentHit.getLocation();
            ProjectileImpactEffects.playPenetrationReport(serverLevel, impact, 2.2f, 1.1f);

            penetratedAny = true;
            exitPoint = ProjectilePhysics.blockExitPoint(entry, direction, currentPos);
            speed = nextSpeed;
        }

        if (!penetratedAny) {
            return false;
        }

        if (!exitedSolidMass && isStillInsideBlockingMass(world, exitPoint, direction)) {
            onKineticPenetrationStopped(createPenetrationHit(BlockPos.containing(exitPoint), direction, impactFace), physics, speed);
            return true;
        }

        if (speed < minimumExitSpeedForContinuedFlight(physics)) {
            onKineticPenetrationStopped(createPenetrationHit(BlockPos.containing(exitPoint), direction, impactFace), physics, speed);
            return true;
        }

        setDeltaMovement(direction.scale(speed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (speed / Math.max(0.001, originalSpeed))));
        setPosAfterPenetration(exitPoint, direction);
        this.inGround = false;
        this.shakeTime = 0;
        this.hasImpulse = true;
        return true;
    }

    protected void onKineticPenetrationStopped(BlockHitResult blockHitResult, ProjectilePhysicsProfile physics, double speed) {
        handleTerminalBlockImpact(blockHitResult, physics, 1.0f);
    }

    private BlockHitResult createPenetrationHit(BlockPos pos, Vec3 direction, Direction impactFace) {
        Vec3 hitLocation = Vec3.atCenterOf(pos).subtract(direction.scale(0.5));
        return new BlockHitResult(hitLocation, impactFace, pos, false);
    }

    private void setPosAfterPenetration(Vec3 pathPoint, Vec3 direction) {
        Vec3 exitPos = pathPoint.add(direction.normalize().scale(0.2D));
        setPos(exitPos.x, exitPos.y, exitPos.z);
    }

    private boolean isPassThroughBlock(Level world, BlockPos pos, BlockState state) {
        return state.isAir();
    }

    private boolean isStillInsideBlockingMass(Level world, Vec3 exitPoint, Vec3 direction) {
        for (int i = 0; i < 4; i++) {
            Vec3 sample = exitPoint.add(direction.scale(i * 0.35D));
            BlockPos pos = BlockPos.containing(sample);
            BlockState state = world.getBlockState(pos);
            if (!isPassThroughBlock(world, pos, state)) {
                return true;
            }
        }
        return false;
    }

    private int calculateMaxPenetrationBlocks(ProjectilePhysicsProfile physics, double speed) {
        double energyBudget = physics.penetrationPower(speed);
        double massBonus = Math.sqrt(Math.max(1.0D, physics.mass())) * Math.max(0.35D, physics.penetration());
        return Math.max(4, Math.min(48, (int) Math.ceil(energyBudget / 95.0D + massBonus)));
    }

    private double calculatePenetrationTraceDistance(ProjectilePhysicsProfile physics, double speed) {
        double massScale = Math.sqrt(Math.max(1.0D, physics.mass()));
        double distance = speed * (2.0D + physics.penetration() * 1.6D + massScale * 0.13D);
        return Math.max(8.0D, Math.min(56.0D, distance));
    }

    private double minimumExitSpeedForContinuedFlight(ProjectilePhysicsProfile physics) {
        return Math.max(0.85D, physics.minContinueSpeed() * 0.65D);
    }

    protected boolean tryKineticEntityPenetration(LivingEntity target, ProjectilePhysicsProfile physics, double speed) {
        double nextSpeed = ProjectilePhysics.remainingEntityPenetrationSpeed(physics, target, speed);
        if (nextSpeed <= 0.0D) {
            return false;
        }

        Vec3 direction = getDeltaMovement().normalize();
        setDeltaMovement(direction.scale(nextSpeed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (nextSpeed / speed)));
        setPos(getX() + direction.x * 0.45, getY() + direction.y * 0.45, getZ() + direction.z * 0.45);
        return true;
    }

    protected void playImpactReport(ServerLevel serverLevel, Vec3 impact, float volume, float pitch) {
        ProjectileImpactEffects.playImpactReport(serverLevel, impact, volume, pitch,
                ProjectileImpactEffects.Style.STANDARD, impactOutwardDirection());
    }

    protected Vec3 impactOutwardDirection() {
        Vec3 movement = getDeltaMovement();
        return movement.lengthSqr() > 1.0E-8D
                ? movement.normalize().reverse()
                : new Vec3(0.0D, 1.0D, 0.0D);
    }

    protected void spawnImpactParticles(ServerLevel serverLevel, Vec3 impact, float intensity) {
        ProjectileImpactEffects.spawnImpactParticles(serverLevel, impact, intensity);
    }

    protected void applyImpactShockDamage(ServerLevel serverLevel, Vec3 center, ProjectilePhysicsProfile physics,
                                          @Nullable LivingEntity excludedTarget,
                                          double radius, float damageScale) {
        if (radius <= 0.0D || damageScale <= 0.0F) {
            return;
        }
        ProjectileBlastResolver.applyImpactShockDamageAndCollect(serverLevel, center, this, getOwner(),
                physics, excludedTarget, radius, (float) (getBaseDamage() * damageScale));
    }

    @Override
    protected SoundEvent getImpactSound() {
        //? if forge {
        /*return SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        return SoundEvents.GENERIC_EXPLODE.value();
        //?}
    }
}
