package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
    protected void playImpactReport(ServerLevel serverLevel, Vec3 impact, float volume, float pitch) {
        ProjectileImpactEffects.playImpactReport(serverLevel, impact, Math.max(8.0F, volume), 0.85F,
                ProjectileImpactEffects.Style.HEAVY, impactOutwardDirection());
    }
}
