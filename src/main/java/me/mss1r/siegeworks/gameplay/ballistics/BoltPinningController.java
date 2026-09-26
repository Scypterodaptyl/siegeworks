package me.mss1r.siegeworks.gameplay.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class BoltPinningController {
    private static final String TAG_TARGET = "PinnedTarget";
    private static final String TAG_ANCHOR_X = "PinnedAnchorX";
    private static final String TAG_ANCHOR_Y = "PinnedAnchorY";
    private static final String TAG_ANCHOR_Z = "PinnedAnchorZ";
    private static final String TAG_TICKS = "PinTicks";
    private static final double DRIFT_TELEPORT_EPSILON_SQR = 1.0E-4D;

    private final Host host;
    private UUID targetUuid;
    private Vec3 targetAnchor = Vec3.ZERO;
    private int ticksRemaining;

    public BoltPinningController(Host host) {
        this.host = host;
    }

    public void save(CompoundTag tag) {
        if (targetUuid == null || ticksRemaining <= 0) {
            return;
        }
        tag.putUUID(TAG_TARGET, targetUuid);
        tag.putDouble(TAG_ANCHOR_X, targetAnchor.x);
        tag.putDouble(TAG_ANCHOR_Y, targetAnchor.y);
        tag.putDouble(TAG_ANCHOR_Z, targetAnchor.z);
        tag.putInt(TAG_TICKS, ticksRemaining);
    }

    public void load(CompoundTag tag) {
        clear();
        if (!tag.hasUUID(TAG_TARGET) || !tag.contains(TAG_TICKS)) {
            return;
        }
        targetUuid = tag.getUUID(TAG_TARGET);
        targetAnchor = new Vec3(
                tag.getDouble(TAG_ANCHOR_X),
                tag.getDouble(TAG_ANCHOR_Y),
                tag.getDouble(TAG_ANCHOR_Z));
        ticksRemaining = Math.max(0, tag.getInt(TAG_TICKS));
    }

    public boolean tryPin(ServerLevel level, LivingEntity target, Vec3 impact, Vec3 direction) {
        PinData pin = findPinData(level, target, impact, direction);
        if (pin == null) {
            return false;
        }

        targetUuid = target.getUUID();
        targetAnchor = pin.targetAnchor();
        ticksRemaining = host.pinDurationTicks();
        lock(target);
        host.embed(pin.boltImpact(), direction, pin.blockPos());
        return true;
    }

    public void tick(ServerLevel level) {
        if (targetUuid == null || ticksRemaining <= 0) {
            return;
        }

        Entity entity = level.getEntity(targetUuid);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            clear();
            return;
        }

        lock(target);
        if (--ticksRemaining <= 0) {
            clear();
        }
    }

    public void clear() {
        targetUuid = null;
        targetAnchor = Vec3.ZERO;
        ticksRemaining = 0;
    }

    private PinData findPinData(ServerLevel level, LivingEntity target, Vec3 impact, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        Vec3 normalizedDirection = direction.normalize();
        Vec3 horizontal = new Vec3(normalizedDirection.x, 0.0D, normalizedDirection.z);
        Vec3 supportDirection = horizontal.lengthSqr() > 1.0E-6D ? horizontal.normalize() : normalizedDirection;

        BlockHitResult hit = findSupport(level, target, impact, supportDirection);
        if (hit == null && supportDirection != normalizedDirection) {
            hit = findSupport(level, target, impact, normalizedDirection);
            supportDirection = normalizedDirection;
        }
        if (hit == null) {
            return null;
        }

        Vec3 anchor = calculateTargetAnchor(target, hit.getLocation(), supportDirection);
        Vec3 boltImpact = calculateBoltPosition(target, impact, anchor, normalizedDirection);
        return new PinData(anchor, boltImpact, hit.getBlockPos());
    }

    private BlockHitResult findSupport(ServerLevel level, LivingEntity target, Vec3 impact, Vec3 direction) {
        Vec3 body = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
        Vec3 upperBody = target.position().add(0.0D, target.getBbHeight() * 0.72D, 0.0D);
        Vec3 lowerBody = target.position().add(0.0D, target.getBbHeight() * 0.34D, 0.0D);
        Vec3[] starts = {
                impact.add(direction.scale(0.12D)),
                body,
                upperBody,
                lowerBody,
                body.add(direction.scale(target.getBbWidth() * 0.55D)),
                upperBody.add(direction.scale(target.getBbWidth() * 0.55D))
        };

        for (Vec3 start : starts) {
            Vec3 end = start.add(direction.scale(host.pinSearchDistance()));
            BlockHitResult hit = level.clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.projectile()));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit;
            }
        }
        return null;
    }

    private static Vec3 calculateTargetAnchor(LivingEntity target, Vec3 supportHit, Vec3 supportDirection) {
        Vec3 horizontal = new Vec3(supportDirection.x, 0.0D, supportDirection.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return target.position();
        }

        Vec3 horizontalDirection = horizontal.normalize();
        double clearance = target.getBbWidth() * 0.5D + 0.16D;
        return new Vec3(
                supportHit.x - horizontalDirection.x * clearance,
                target.getY(),
                supportHit.z - horizontalDirection.z * clearance);
    }

    private static Vec3 calculateBoltPosition(LivingEntity target, Vec3 impact, Vec3 targetAnchor, Vec3 direction) {
        double yOffset = Mth.clamp(
                impact.y - target.getY(), target.getBbHeight() * 0.32D, target.getBbHeight() * 0.78D);
        Vec3 torso = new Vec3(targetAnchor.x, targetAnchor.y + yOffset, targetAnchor.z);
        return torso.add(direction.normalize().scale(target.getBbWidth() * 0.22D));
    }

    private void lock(LivingEntity target) {
        if (target.position().distanceToSqr(targetAnchor) > DRIFT_TELEPORT_EPSILON_SQR) {
            target.teleportTo(targetAnchor.x, targetAnchor.y, targetAnchor.z);
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    public interface Host {
        Entity projectile();

        double pinSearchDistance();

        int pinDurationTicks();

        void embed(Vec3 impact, Vec3 direction, BlockPos supportPos);
    }

    private record PinData(Vec3 targetAnchor, Vec3 boltImpact, BlockPos blockPos) {
    }
}
