package me.mss1r.siegeworks.entity.base;

import me.mss1r.siegeworks.gameplay.damage.SiegeProjectileCombat;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.gameplay.ballistics.DistantFlight;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileSweep;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.axiomata.collision.CollidableStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class SiegeProjectile extends ThrowableItemProjectile {
    private static final double RENDER_DISTANCE = 160.0D;

    private static final String TAG_BASE_DAMAGE = "BaseDamage";
    private static final String TAG_IN_GROUND = "InGround";
    private static final String TAG_SHAKE_TIME = "ShakeTime";
    private static final String TAG_PHYSICS_PROFILE = "PhysicsProfile";
    private static final String TAG_FIRED_BY_SIEGE = "FiredBySiege";
    private static final String TAG_RESPONSIBLE_PLAYER = "ResponsiblePlayer";
    private static final double TICKS_PER_SECOND = 20.0D;
    /** The share of its velocity the game lets a thrown projectile keep each tick in air. */
    private static final double VANILLA_AIR_RETENTION = 0.99D;
    /** Ticks a client runs a shot on past the newest server position before it waits for the next. */
    private static final int MAX_TICKS_AHEAD = 3;
    /**
     * The velocity it was launched at, exactly: the spawn packet clips a shot's velocity to 3.9 blocks a tick,
     * which a cannon ball leaves at five times over.
     */
    private static final EntityDataAccessor<Vector3f> LAUNCH_VELOCITY =
            SynchedEntityData.defineId(SiegeProjectile.class, EntityDataSerializers.VECTOR3);

    private boolean applyingPredictedPhysics;
    private boolean hitEntityThisTick;
    private final Set<Integer> hitTargets = new HashSet<>();
    protected boolean inGround;
    protected int shakeTime;
    private double baseDamage = 2.0D;
    private SoundEvent hitSound = SoundEvents.ARROW_HIT;
    @Nullable
    private ResourceLocation physicsProfileId;
    // Set from the shooter while the superclass constructs, so these must not have initializers.
    private boolean firedBySiege;
    @Nullable
    private UUID responsiblePlayerId;
    /** The game time of the last tick it flew, so a shot the level left untouched is flown on elsewhere. */
    private long lastFlownTick = Long.MIN_VALUE;
    /** Where it went into the body it is stuck in, where what it still carries opens the crater. */
    @Nullable
    private Vec3 craterMouth;
    @Nullable
    private Vec3 craterFace;
    /** On a client, where the server last had the shot, and how far it moved in the server tick before that. */
    @Nullable
    private Vec3 serverPosition;
    private Vec3 serverStep = Vec3.ZERO;
    private int serverPositionsThisTick;
    private int ticksAheadOfServer;

    public SiegeProjectile(EntityType<? extends SiegeProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
        moveItself();
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
        data.define(LAUNCH_VELOCITY, new Vector3f());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        double range = RENDER_DISTANCE * getViewScale();
        return distanceSquared < range * range;
    }

    public SiegeProjectile(EntityType<? extends SiegeProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        moveItself();
    }

    /**
     * A shot moves itself and finds its own hits along its path; a moving engine's arm or frame must not carry or
     * shove it, as it would a slow stone a low-tension throw has only just let go of.
     */
    private void moveItself() {
        noPhysics = true;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STONE;
    }

    // The game's own fall is left out: {@link #flyThroughAir} drags and drops the shot after it moves.
    //? if forge {
    /*@Override
    protected float getGravity() {
        return 0.0F;
    }
    *///?} else {
    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }
    //?}

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble(TAG_BASE_DAMAGE, baseDamage);
        tag.putBoolean(TAG_IN_GROUND, inGround);
        tag.putInt(TAG_SHAKE_TIME, shakeTime);
        tag.putBoolean(TAG_FIRED_BY_SIEGE, firedBySiege);
        if (responsiblePlayerId != null) {
            tag.putUUID(TAG_RESPONSIBLE_PLAYER, responsiblePlayerId);
        }
        if (physicsProfileId != null) {
            tag.putString(TAG_PHYSICS_PROFILE, physicsProfileId.toString());
        }
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
        firedBySiege = tag.getBoolean(TAG_FIRED_BY_SIEGE);
        responsiblePlayerId = tag.hasUUID(TAG_RESPONSIBLE_PLAYER) ? tag.getUUID(TAG_RESPONSIBLE_PLAYER) : null;
        physicsProfileId = tag.contains(TAG_PHYSICS_PROFILE)
                ? ResourceLocation.tryParse(tag.getString(TAG_PHYSICS_PROFILE))
                : null;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Debris thrown from the hole a shot just made flies out across its path and is no target for it.
        if (target instanceof FallingBlockEntity || hasHitTarget(target) || !SiegeProjectileCombat.mayHit(this, target)) {
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
        Entity hitTarget = entityHitResult.getEntity();
        rememberHitTarget(hitTarget);
        if (livingTarget(hitTarget) != null) {
            damageTarget(hitTarget, (float) this.getBaseDamage());
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
            followServer();
            return;
        }

        applyingPredictedPhysics = true;
        try {
            super.tick();
            if (!this.inGround && !this.isRemoved()) {
                flyThroughAir();
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

    @Override
    //? if forge {
    /*public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean teleport) {
    *///?} else {
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
    //?}
        Vec3 target = new Vec3(x, y, z);
        if (shouldPredictMotionOnClient()) {
            setPos(x, y, z);
        } else if (!target.equals(lerpTarget()) && !target.equals(position())) {
            // A packet that only turns the shot hands back where it already is or is headed.
            startFollowingServer();
            serverStep = target.subtract(serverPosition);
            serverPosition = target;
            serverPositionsThisTick++;
        }
        setRot(yaw, pitch);
    }

    private Vec3 lerpTarget() {
        return serverPosition != null ? serverPosition : position();
    }

    /** Starts from where it was spawned, flying at the velocity it was launched at. */
    private void startFollowingServer() {
        if (serverPosition == null) {
            serverPosition = position();
            serverStep = new Vec3(entityData.get(LAUNCH_VELOCITY));
        }
    }

    //? if neoforge {
    @Override
    public double lerpTargetX() {
        return lerpTarget().x;
    }

    @Override
    public double lerpTargetY() {
        return lerpTarget().y;
    }

    @Override
    public double lerpTargetZ() {
        return lerpTarget().z;
    }
    //?}

    /**
     * Shows a client's shot where the server has it now: the newest position the server sent, carried on by
     * the step the server made last for each tick that has passed since. Each server tick sends one position,
     * but they come unevenly, none in one tick and two in the next, so the shot runs on by itself through a
     * gap and the next positions catch it up instead of standing it still. A shot the server has stopped has
     * no velocity left and stays where the server left it.
     */
    private void followServer() {
        startFollowingServer();
        ticksAheadOfServer = Mth.clamp(ticksAheadOfServer + 1 - serverPositionsThisTick, 0, MAX_TICKS_AHEAD);
        serverPositionsThisTick = 0;
        Vec3 step = getDeltaMovement().lengthSqr() > 1.0E-8D ? serverStep : Vec3.ZERO;
        Vec3 shown = serverPosition.add(step.scale(ticksAheadOfServer));
        setPos(shown.x, shown.y, shown.z);
    }

    /**
     * Slows the shot by the air, as its mass, width and shape make it, and drops it by the earth's gravity, in
     * place of the game's flat 1% a tick, which takes far more from a heavy ball than air does. In water the
     * game's own heavy drag stays.
     */
    private void flyThroughAir() {
        if (level() instanceof ServerLevel serverLevel) {
            lastFlownTick = serverLevel.getGameTime();
            DistantFlight.track(this);
        }
        Vec3 movement = getDeltaMovement();
        if (isInWater()) {
            if (!isNoGravity()) {
                super.setDeltaMovement(movement.add(0.0D, -SiegeBallistics.GRAVITY, 0.0D));
            }
            return;
        }
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        double gravity = isNoGravity() ? 0.0D : SiegeBallistics.GRAVITY;
        SiegeBallistics.Flight air = new SiegeBallistics.Flight(gravity, physics.airDrag(physics.diameterOf(this)),
                0.0D, 0);
        super.setDeltaMovement(air.afterMove(movement.scale(1.0D / VANILLA_AIR_RETENTION)));
    }

    /** Whether it flew on the given game tick. */
    public boolean flewOn(long gameTime) {
        return lastFlownTick == gameTime;
    }

    /** Whether it is still in the air, not stuck in or resting on anything. */
    public boolean isInFlight() {
        return !inGround && !isRemoved();
    }

    @Override
    public void setDeltaMovement(Vec3 deltaMovement) {
        super.setDeltaMovement(deltaMovement);
        if (!level().isClientSide && tickCount == 0) {
            entityData.set(LAUNCH_VELOCITY, deltaMovement.toVector3f());
        }
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

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        if (owner instanceof AbstractSiegeEntity) {
            firedBySiege = true;
            responsiblePlayerId = SiegeBlockBreaker.responsibleUuid(owner);
        }
    }

    /**
     * A burst no engine fired, such as a pot set down going off, still reaches the blocks around it, answered for
     * by {@code responsible}.
     */
    public void reachBlocksAs(@Nullable UUID responsible) {
        firedBySiege = true;
        responsiblePlayerId = responsible;
    }

    /** Only shots from a siege engine touch blocks, even when the engine is gone before they land. */
    public boolean isFiredBySiege() {
        return firedBySiege;
    }

    /** Flies and strikes by another profile than the entity type's, for a variant such as an explosive one. */
    public void setPhysicsProfile(ResourceLocation profileId) {
        this.physicsProfileId = profileId;
    }

    public ProjectilePhysicsProfile getPhysicsProfile() {
        return physicsProfileId != null
                ? SiegeProfileCatalogs.PROJECTILES.get(physicsProfileId)
                : SiegeProfileCatalogs.PROJECTILES.forEntity(this.getType());
    }

    /**
     * Runs into the block it hit for as long as it can cut through. When it comes out the far side it is
     * moved there and flies on at the speed it kept; otherwise the result says where it stopped.
     */
    protected ProjectileImpacts.Drive driveInto(ServerLevel level, BlockHitResult hit) {
        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();
        BlockState struck = level.getBlockState(hit.getBlockPos());
        struck.onProjectileHit(level, struck, hit, this);
        if (!firedBySiege || speed < 1.0E-6D) {
            return new ProjectileImpacts.Drive(false, hit.getLocation(), speed, hit.getBlockPos(), hit.getLocation(),
                    Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
        }
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        double metresPerSecond = TICKS_PER_SECOND;
        ProjectileImpacts.Drive drive = ProjectileImpacts.drive(level, physics, physics.diameterOf(this),
                hit.getLocation(), hit.getBlockPos(), velocity.scale(metresPerSecond), breaker());
        double kept = drive.speed() / metresPerSecond;
        if (drive.passedThrough()) {
            setPos(drive.position().x, drive.position().y, drive.position().z);
            setDeltaMovement(velocity.scale(kept / speed));
            setBaseDamage(Math.max(1.0D, getBaseDamage() * kept / speed));
            hasImpulse = true;
        }
        craterMouth = drive.passedThrough() ? null : drive.mouth();
        craterFace = drive.passedThrough() ? null : drive.face();
        return new ProjectileImpacts.Drive(drive.passedThrough(), drive.position(), kept, drive.block(),
                drive.mouth(), drive.face());
    }

    /**
     * Spends what is left of it where it stopped, {@code speed} being its speed in the game: what it still
     * carried breaks the material around it, and any charge and fire it has go off.
     */
    protected void arrive(ServerLevel level, Vec3 at, double speed) {
        if (!firedBySiege) {
            return;
        }
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        Vec3 outward = getDeltaMovement().lengthSqr() > 1.0E-8D
                ? getDeltaMovement().normalize().reverse()
                : new Vec3(0.0D, 1.0D, 0.0D);
        ProjectileImpacts.stop(level, craterMouth != null ? craterMouth : at, craterFace != null ? craterFace : outward,
                outward.reverse(), physics, realSpeed(speed), physics.diameterOf(this), breaker());
        craterMouth = null;
        craterFace = null;
        double blastEnergy = payloadBlastEnergy(physics);
        ProjectilePhysicsProfile.Fire fire = payloadFire(physics);
        Player breaker = breaker();
        ProjectileImpacts.blast(level, at, outward, blastEnergy, breaker);
        ProjectileImpacts.ignite(level, at, fire, breaker);
        if (blastEnergy > 0.0D || fire.radius() > 0.0D) {
            // A charge or a burst of fire going off sets off the pots it reaches, as TNT sets off TNT.
            IncendiaryPotBlock.detonateAround(level, at, physics.shock().radius(), responsiblePlayerId);
        }
    }

    /** The blast of the charge it carries, in joules. */
    protected double payloadBlastEnergy(ProjectilePhysicsProfile physics) {
        return physics.blast().energy();
    }

    /** The fire it spreads where it goes off. */
    protected ProjectilePhysicsProfile.Fire payloadFire(ProjectilePhysicsProfile physics) {
        return physics.fire();
    }

    /** How fast, in metres per second, a shot flying at {@code speed} blocks per tick strikes. */
    public double realSpeed(double speed) {
        return speed * TICKS_PER_SECOND;
    }

    @Nullable
    private Player breaker() {
        Entity owner = getOwner();
        if (owner != null) {
            UUID current = SiegeBlockBreaker.responsibleUuid(owner);
            if (current != null) {
                responsiblePlayerId = current;
            }
            return SiegeBlockBreaker.responsiblePlayer(owner);
        }
        return level() instanceof ServerLevel serverLevel
                ? SiegeBlockBreaker.playerFor(serverLevel, responsiblePlayerId)
                : null;
    }

    protected boolean damageLivingTarget(LivingEntity target, float damage) {
        return damageLivingTarget(target, damage, true);
    }

    protected boolean damageLivingTarget(LivingEntity target, float damage, boolean breakShield) {
        return damageTarget(target, damage, breakShield);
    }

    protected boolean damageTarget(Entity target, float damage) {
        return damageTarget(target, damage, true);
    }

    protected boolean damageTarget(Entity target, float damage, boolean breakShield) {
        return SiegeProjectileCombat.damage(this, target, damage, breakShield);
    }

    protected LivingEntity livingTarget(Entity target) {
        return SiegeProjectileCombat.livingTarget(target);
    }

    protected void rememberHitTarget(Entity target) {
        hitTargets.add(SiegeProjectileCombat.targetId(target));
    }

    protected boolean hasHitTarget(Entity target) {
        return hitTargets.contains(SiegeProjectileCombat.targetId(target));
    }

    protected int hitTargetCount() {
        return hitTargets.size();
    }
}
