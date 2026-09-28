package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.gameplay.damage.SiegeProjectileCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ProjectileBlastResolver {
    private static final double SHRAPNEL_FALLOFF_EXPONENT = 0.65D;

    private ProjectileBlastResolver() {
    }

    public static void applyImpactShockDamage(ServerLevel level, Vec3 center,
                                              @Nullable Entity directSource,
                                              @Nullable Entity owner, double radius, float damage) {
        applyImpactShockDamageAndCollect(level, center, directSource, owner,
                ProjectilePhysicsProfile.DEFAULT, null, radius, damage);
    }

    public static List<LivingEntity> applyImpactShockDamageAndCollect(
            ServerLevel level, Vec3 center, @Nullable Entity directSource,
            @Nullable Entity owner, double radius, float damage) {
        return applyImpactShockDamageAndCollect(level, center, directSource, owner,
                ProjectilePhysicsProfile.DEFAULT, null, radius, damage);
    }

    public static List<LivingEntity> applyImpactShockDamageAndCollect(
            ServerLevel level, Vec3 center, @Nullable Entity directSource,
            @Nullable Entity owner, ProjectilePhysicsProfile physics,
            @Nullable LivingEntity excludedTarget, double radius, float damage) {
        List<LivingEntity> damagedTargets = new ArrayList<>();
        if (radius <= 0.0D || damage <= 0.0F) {
            return damagedTargets;
        }
        owner = responsibleAttacker(directSource, owner);

        AABB area = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, Entity::isAlive)) {
            if (target == directSource || target == owner || target == excludedTarget) {
                continue;
            }

            double distance = distanceToBounds(center, target.getBoundingBox());
            if (distance > radius) {
                continue;
            }

            double falloff = 1.0D - distance / radius;
            double exposure = Explosion.getSeenPercent(center, target);
            float scaledDamage = (float) (damage * Math.pow(falloff, 0.75D) * exposure);
            float appliedDamage = ProjectilePhysics.damageWithArmorPiercing(physics, scaledDamage, target);
            if (appliedDamage > 0.5F
                    && target.hurt(level.damageSources().explosion(directSource, owner), appliedDamage)) {
                damagedTargets.add(target);
            }
        }
        return damagedTargets;
    }

    public static void applyControlledExplosion(
            ServerLevel level, Vec3 center, @Nullable Entity directSource, @Nullable Entity owner,
            ProjectilePhysicsProfile physics, double speed, float baseDamage,
            ProjectileImpactEffects.Style style) {
        applyControlledExplosion(level, center, directSource, owner, physics, speed,
                baseDamage, style, null);
    }

    public static void applyControlledExplosion(
            ServerLevel level, Vec3 center, @Nullable Entity directSource, @Nullable Entity owner,
            ProjectilePhysicsProfile physics, double speed, float baseDamage,
            ProjectileImpactEffects.Style style, @Nullable LivingEntity excludedTarget) {
        owner = responsibleAttacker(directSource, owner);
        float explosionPower = physics.scaledExplosionPower(speed);
        if (explosionPower > 0.0F) {
            float damageScale = Mth.clamp(
                    (float) Math.sqrt(Math.max(1.0F, baseDamage) / 36.0F), 0.85F, 1.55F);
            explosionPower *= damageScale;
        }

        float volume = style == ProjectileImpactEffects.Style.HEAVY
                ? Math.max(8.0F, 3.0F + explosionPower * 1.35F)
                : Math.max(4.0F, 3.0F + explosionPower * 1.2F);
        ProjectileImpactEffects.playImpactReport(
                level, center, volume,
                style == ProjectileImpactEffects.Style.HEAVY ? 0.85F : 0.95F,
                style, ProjectileImpactEffects.outwardDirection(directSource));

        if (explosionPower > 0.0F) {
            double massScale = Math.sqrt(Math.max(1.0D, physics.mass()) / 48.0D);
            double radius = Math.max(2.0D, explosionPower * (0.55D + 0.15D * massScale)
                    + physics.blockDamageMultiplier() * 0.8D);
            double cap = Math.max(3.0D, physics.shockRadius() * 0.65D);
            radius = Math.min(radius, cap);

            float blockDamage = (float) (explosionPower * (0.45D + 0.12D * massScale))
                    * (float) Math.max(0.7D, physics.blockDamageMultiplier());
            ProjectileBlockImpact.damageBlocksInSphere(
                    level, center, radius, Math.max(0.9F, blockDamage),
                    SiegeBlockBreaker.responsiblePlayer(owner));
        }

        double shockRadius = Math.max(physics.shockRadius(), physics.scaledShockRadius(speed));
        float shockDamage = baseDamage * Math.max(0.0F, physics.scaledShockDamageMultiplier(speed));
        applyImpactShockDamageAndCollect(level, center, directSource, owner,
                physics, excludedTarget, shockRadius, shockDamage);
    }

    public static void explodeShrapnel(
            ServerLevel level, Vec3 center, @Nullable Entity directSource,
            @Nullable Entity owner, ProjectilePhysicsProfile physics, double speed) {
        explodeShrapnel(level, center, directSource, owner, physics,
                physics.scaledExplosionPower(speed));
    }

    public static void explodeShrapnel(
            ServerLevel level, Vec3 center, @Nullable Entity directSource,
            @Nullable Entity owner, ProjectilePhysicsProfile physics, float explosionPower) {
        owner = responsibleAttacker(directSource, owner);
        int fragments = physics.shrapnelFragments();
        double radius = physics.shrapnelRadius();
        float maxDamage = physics.shrapnelDamage();
        float volume = Math.max(4.0F, 3.0F + explosionPower * 1.5F);

        ProjectileImpactEffects.playImpactReport(
                level, center, volume, 0.95F, ProjectileImpactEffects.Style.GUNPOWDER,
                ProjectileImpactEffects.outwardDirection(directSource));
        ProjectileImpactEffects.spawnGunpowderBurst(level, center, fragments);

        if (explosionPower > 0.0F) {
            double blockRadius = Math.max(1.75D, Math.min(physics.shockRadius() * 0.45D,
                    explosionPower * 0.75D + physics.blockDamageMultiplier() * 0.8D));
            float blockDamage = Math.max(
                    0.7F, explosionPower * (float) Math.max(0.45D, physics.blockDamageMultiplier()));
            ProjectileBlockImpact.damageBlocksInSphere(level, center, blockRadius, blockDamage,
                    SiegeBlockBreaker.responsiblePlayer(owner));
        }

        AABB blastBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blastBox, Entity::isAlive)) {
            if (target == directSource || target == owner) {
                continue;
            }

            double distance = Math.max(0.25D, distanceToBounds(center, target.getBoundingBox()));
            if (distance > radius) {
                continue;
            }

            double distanceFalloff = Mth.clamp(1.0D - distance / radius, 0.0D, 1.0D);
            double exposure = Explosion.getSeenPercent(center, target);
            float damage = (float) (maxDamage
                    * Math.pow(distanceFalloff, SHRAPNEL_FALLOFF_EXPONENT)
                    * exposure);
            float appliedDamage = ProjectilePhysics.damageWithArmorPiercing(physics, damage, target);
            if (appliedDamage > 0.25F) {
                target.hurt(level.damageSources().explosion(directSource, owner), appliedDamage);
            }
        }
    }

    private static double distanceToBounds(Vec3 point, AABB bounds) {
        double closestX = Mth.clamp(point.x, bounds.minX, bounds.maxX);
        double closestY = Mth.clamp(point.y, bounds.minY, bounds.maxY);
        double closestZ = Mth.clamp(point.z, bounds.minZ, bounds.maxZ);
        return point.distanceTo(new Vec3(closestX, closestY, closestZ));
    }

    @Nullable
    private static Entity responsibleAttacker(@Nullable Entity directSource, @Nullable Entity owner) {
        return directSource instanceof SiegeProjectile projectile
                ? SiegeProjectileCombat.responsibleAttacker(projectile)
                : owner;
    }
}
