package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ProjectilePhysics {
    private ProjectilePhysics() {
    }

    public static double blockPenetrationCost(Level level, BlockPos pos, BlockState state, ProjectilePhysicsProfile profile) {
        if (state.isAir()) {
            return 0.0;
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0f) {
            return Double.POSITIVE_INFINITY;
        }

        double normalizedResistance = normalizedExplosionResistance(state);
        double rawCost = Math.max(0.05f, hardness) * 560.0 + normalizedResistance * 115.0;
        double density = materialDensityMultiplier(hardness, normalizedResistance);
        return rawCost * density * softMaterialMultiplier(hardness, normalizedResistance)
                * Math.max(0.05, profile.blockCostMultiplier());
    }

    public static double minContinueSpeedForBlock(Level level, BlockPos pos, ProjectilePhysicsProfile profile, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0f) {
            return Double.POSITIVE_INFINITY;
        }

        double resistance = normalizedExplosionResistance(state);
        if (isVerySoftMaterial(hardness, resistance)) {
            return 0.18;
        }

        double density = materialDensityMultiplier(hardness, resistance);
        if (density <= 0.35) {
            return 0.35;
        }
        if (density <= 0.7) {
            return 0.75;
        }
        return profile.minContinueSpeed();
    }

    public static double remainingSpeedAfterCost(ProjectilePhysicsProfile profile, double currentSpeed, double cost) {
        double energy = profile.kineticEnergy(currentSpeed);
        double remainingEnergy = energy - cost * Math.max(0.0, profile.energyLossMultiplier());
        if (remainingEnergy <= 0.0) {
            return 0.0;
        }

        return Math.sqrt((2.0 * remainingEnergy) / Math.max(0.01, profile.mass()));
    }

    public static double remainingBlockPenetrationSpeed(Level level, BlockPos pos, BlockState state,
                                                        ProjectilePhysicsProfile profile, double currentSpeed,
                                                        double costMultiplier) {
        if (currentSpeed < profile.minContinueSpeed()) {
            return 0.0;
        }

        double cost = blockPenetrationCost(level, pos, state, profile) * Math.max(0.0, costMultiplier);
        if (profile.penetrationPower(currentSpeed) < cost) {
            return 0.0;
        }

        double nextSpeed = remainingSpeedAfterCost(profile, currentSpeed, cost);
        if (nextSpeed < minContinueSpeedForBlock(level, pos, profile, state)) {
            return 0.0;
        }

        return nextSpeed;
    }

    public static double blockPathLength(Vec3 origin, Vec3 direction, BlockPos pos) {
        RaySpan span = rayBlockSpan(origin, direction, pos);
        if (span == null) {
            return 0.35D;
        }
        return Math.max(0.02D, span.exit() - Math.max(0.0D, span.enter()));
    }

    public static Vec3 blockExitPoint(Vec3 origin, Vec3 direction, BlockPos pos) {
        RaySpan span = rayBlockSpan(origin, direction, pos);
        if (span == null) {
            return Vec3.atCenterOf(pos).add(direction.scale(0.55D));
        }
        return origin.add(direction.scale(Math.max(0.0D, span.exit()) + 0.04D));
    }

    public static float entityDamage(ProjectilePhysicsProfile profile, float baseDamage, double speed, LivingEntity target) {
        double speedMultiplier = Mth.clamp(0.78 + Math.log1p(Math.max(0.0, speed)) * 0.24, 0.65, 1.75);
        float rawDamage = (float) (baseDamage * profile.entityDamageMultiplier() * speedMultiplier);
        if (target instanceof AbstractSiegeEntity) {
            return structuralImpactDamage(profile, rawDamage, speed);
        }
        return damageWithArmorPiercing(profile, rawDamage, target);
    }

    public static float structuralImpactDamage(ProjectilePhysicsProfile profile, float rawDamage, double speed) {
        double energyFactor = Math.sqrt(profile.kineticEnergy(speed) / 1_500.0D);
        double massBearingFactor = 0.95D * Math.sqrt(Math.max(0.01D, profile.mass()) / 90.0D);
        double materialCoupling = 0.45D
                + 0.55D * Math.sqrt(Math.max(0.0D, profile.blockDamageMultiplier()));
        double impactCoupling = Mth.clamp(
                Math.max(energyFactor, massBearingFactor) * materialCoupling, 0.1D, 0.7D);
        return (float) (Math.max(0.0F, rawDamage) * impactCoupling);
    }

    public static float damageWithArmorPiercing(ProjectilePhysicsProfile profile, float rawDamage,
                                                 LivingEntity target) {
        float damage = Math.max(0.0F, rawDamage);
        float piercing = Mth.clamp((float) profile.armorPiercing(), 0.0F, 1.0F);
        float armor = target.getArmorValue();
        float toughness = (float) armorToughness(target);
        if (damage <= 0.0F || piercing <= 0.0F || armor <= 0.0F) {
            return damage;
        }

        float absorbedDamage = armorAdjustedDamage(target, damage, armor, toughness);
        float desiredDamage = Mth.lerp(piercing, absorbedDamage, damage);
        if (desiredDamage <= absorbedDamage + 1.0E-4F) {
            return damage;
        }

        float low = damage;
        float high = Math.max(damage + 1.0F, damage * 2.0F);
        while (armorAdjustedDamage(target, high, armor, toughness) < desiredDamage && high < 1_000_000.0F) {
            high *= 2.0F;
        }
        for (int iteration = 0; iteration < 18; iteration++) {
            float middle = (low + high) * 0.5F;
            if (armorAdjustedDamage(target, middle, armor, toughness) < desiredDamage) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    private static float armorAdjustedDamage(LivingEntity target, float damage, float armor, float toughness) {
        //? if forge {
        /*return CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        *///?} else {
        return CombatRules.getDamageAfterAbsorb(
                target, damage, target.damageSources().generic(), armor, toughness);
        //?}
    }

    public static double entityPenetrationCost(ProjectilePhysicsProfile profile, LivingEntity target) {
        double bodySize = Math.max(1.0, target.getBbWidth() * target.getBbHeight());
        double armor = target.getArmorValue() + armorToughness(target) * 0.5;
        return (70.0 + bodySize * 65.0 + armor * 14.0) * Math.max(0.05, profile.blockCostMultiplier());
    }

    public static double remainingEntityPenetrationSpeed(ProjectilePhysicsProfile profile, LivingEntity target,
                                                         double currentSpeed) {
        if (currentSpeed < profile.minContinueSpeed()) {
            return 0.0;
        }

        double cost = entityPenetrationCost(profile, target);
        if (profile.penetrationPower(currentSpeed) < cost) {
            return 0.0;
        }

        double nextSpeed = remainingSpeedAfterCost(profile, currentSpeed, cost);
        if (nextSpeed < profile.minContinueSpeed()) {
            return 0.0;
        }

        return nextSpeed;
    }

    private static double armorToughness(LivingEntity target) {
        AttributeInstance toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        return toughness == null ? 0.0 : toughness.getValue();
    }

    private static double normalizedExplosionResistance(BlockState state) {
        return Math.max(0.03, Math.min(1200.0F, state.getBlock().getExplosionResistance()));
    }

    private static double materialDensityMultiplier(double hardness, double resistance) {
        double hardnessDensity = Math.sqrt(Math.max(0.03, hardness) / 1.5);
        double resistanceDensity = Math.sqrt(Math.max(0.03, resistance) / 6.0);
        return Mth.clamp(hardnessDensity * 0.45 + resistanceDensity * 0.55, 0.05, 3.0);
    }

    private static double softMaterialMultiplier(double hardness, double resistance) {
        if (isVerySoftMaterial(hardness, resistance)) {
            return 0.18;
        }
        if (hardness <= 0.6 && resistance <= 1.5) {
            return 0.42;
        }
        return 1.0;
    }

    private static boolean isVerySoftMaterial(double hardness, double resistance) {
        return hardness <= 0.25 && resistance <= 0.8;
    }

    private static RaySpan rayBlockSpan(Vec3 origin, Vec3 direction, BlockPos pos) {
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        double[] originValues = {origin.x, origin.y, origin.z};
        double[] directionValues = {direction.x, direction.y, direction.z};
        double[] minValues = {pos.getX(), pos.getY(), pos.getZ()};
        double[] maxValues = {pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D};

        for (int axis = 0; axis < 3; axis++) {
            double axisDirection = directionValues[axis];
            if (Math.abs(axisDirection) < 1.0E-7D) {
                if (originValues[axis] < minValues[axis] || originValues[axis] > maxValues[axis]) {
                    return null;
                }
                continue;
            }

            double first = (minValues[axis] - originValues[axis]) / axisDirection;
            double second = (maxValues[axis] - originValues[axis]) / axisDirection;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            enter = Math.max(enter, first);
            exit = Math.min(exit, second);
            if (exit < enter) {
                return null;
            }
        }

        return exit < 0.0D ? null : new RaySpan(enter, exit);
    }

    private record RaySpan(double enter, double exit) {
    }
}
