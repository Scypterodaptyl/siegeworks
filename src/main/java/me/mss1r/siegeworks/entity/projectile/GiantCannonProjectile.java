package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class GiantCannonProjectile extends CannonProjectile {
    private boolean renderRotationLocked;
    private float fixedRenderYaw;
    private float fixedRenderPitch;

    public GiantCannonProjectile(EntityType<? extends GiantCannonProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GiantCannonProjectile(EntityType<? extends GiantCannonProjectile> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
    }

    public void lockRenderRotation(float yaw, float pitch) {
        this.renderRotationLocked = true;
        this.fixedRenderYaw = yaw;
        this.fixedRenderPitch = pitch;
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    @Override
    public void tick() {
        super.tick();

        if (!renderRotationLocked) {
            lockRenderRotation(getYRot(), getXRot());
        } else {
            setYRot(fixedRenderYaw);
            setXRot(fixedRenderPitch);
            yRotO = fixedRenderYaw;
            xRotO = fixedRenderPitch;
        }

    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        double speed = getDeltaMovement().length();
        if (!physics.impactFuse() && tryKineticBlockPenetration(blockHitResult, physics)) {
            return;
        }

        applyImpactBlockDamage(blockHitResult, 1.0f);
        explodeAt(blockHitResult.getLocation(), physics.scaledExplosionPower(speed), physics, speed);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        double speed = getDeltaMovement().length();
        LivingEntity excludedTarget = null;
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);
        if (target != null) {
            float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
            if (damageTarget(hitTarget, damage)) {
                excludedTarget = target;
            }
        }
        explodeAt(entityHitResult.getLocation(), physics.scaledExplosionPower(speed), physics, speed, excludedTarget);
    }

    @Override
    protected void onKineticPenetrationStopped(BlockHitResult blockHitResult, ProjectilePhysicsProfile physics, double speed) {
        applyImpactBlockDamage(blockHitResult, 1.0f);
        explodeAt(blockHitResult.getLocation(), physics.scaledExplosionPower(speed), physics, speed);
    }

    protected void explodeAt(Vec3 impactPos) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        explodeAt(impactPos, physics.scaledExplosionPower(getDeltaMovement().length()), physics, getDeltaMovement().length());
    }

    protected void explodeAt(Vec3 impactPos, float explosionPower, ProjectilePhysicsProfile physics, double speed) {
        explodeAt(impactPos, explosionPower, physics, speed, null);
    }

    protected void explodeAt(Vec3 impactPos, float explosionPower, ProjectilePhysicsProfile physics, double speed,
                             @Nullable LivingEntity excludedTarget) {
        if (level() instanceof ServerLevel serverLevel) {
            ProjectileBlastResolver.applyControlledExplosion(serverLevel, impactPos, this, getOwner(),
                    physics, speed, (float) getBaseDamage(), ProjectileImpactEffects.Style.HEAVY,
                    excludedTarget);
        }
        discard();
    }
}
