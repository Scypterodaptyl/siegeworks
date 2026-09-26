package me.mss1r.siegeworks.data.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.Optional;

public record ProjectilePhysicsProfile(
        double mass,
        double penetration,
        double drag,
        boolean impactFuse,
        double blockCostMultiplier,
        double blockDamageMultiplier,
        double energyLossMultiplier,
        double armorPiercing,
        double entityDamageMultiplier,
        double shockRadius,
        double shockDamageMultiplier,
        float baseExplosionPower,
        float speedExplosionScale,
        int shrapnelFragments,
        double shrapnelRadius,
        float shrapnelDamage
) {
    public static final ProjectilePhysicsProfile DEFAULT = new ProjectilePhysicsProfile(
            40.0D, 0.65D, 0.0015D, false, 1.0D, 1.0D, 2.0D, 0.0D,
            1.0D, 3.0D, 0.75D, 0.0F, 0.15F, 0, 0.0D, 0.0F
    );

    public static final Codec<ProjectilePhysicsProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("mass", DEFAULT.mass()).forGetter(ProjectilePhysicsProfile::mass),
            Codec.DOUBLE.optionalFieldOf("penetration", DEFAULT.penetration()).forGetter(ProjectilePhysicsProfile::penetration),
            Codec.DOUBLE.optionalFieldOf("drag", DEFAULT.drag()).forGetter(ProjectilePhysicsProfile::drag),
            Codec.BOOL.optionalFieldOf("impactFuse", DEFAULT.impactFuse()).forGetter(ProjectilePhysicsProfile::impactFuse),
            Codec.DOUBLE.optionalFieldOf("blockCostMultiplier", DEFAULT.blockCostMultiplier()).forGetter(ProjectilePhysicsProfile::blockCostMultiplier),
            Codec.DOUBLE.optionalFieldOf("blockDamageMultiplier", DEFAULT.blockDamageMultiplier()).forGetter(ProjectilePhysicsProfile::blockDamageMultiplier),
            Codec.DOUBLE.optionalFieldOf("energyLossMultiplier", DEFAULT.energyLossMultiplier()).forGetter(ProjectilePhysicsProfile::energyLossMultiplier),
            Codec.DOUBLE.optionalFieldOf("armorPiercing", DEFAULT.armorPiercing()).forGetter(ProjectilePhysicsProfile::armorPiercing),
            Codec.DOUBLE.optionalFieldOf("entityDamageMultiplier", DEFAULT.entityDamageMultiplier()).forGetter(ProjectilePhysicsProfile::entityDamageMultiplier),
            Codec.DOUBLE.optionalFieldOf("shockRadius", DEFAULT.shockRadius()).forGetter(ProjectilePhysicsProfile::shockRadius),
            Codec.DOUBLE.optionalFieldOf("shockDamageMultiplier", DEFAULT.shockDamageMultiplier()).forGetter(ProjectilePhysicsProfile::shockDamageMultiplier),
            Codec.FLOAT.optionalFieldOf("baseExplosionPower", DEFAULT.baseExplosionPower()).forGetter(ProjectilePhysicsProfile::baseExplosionPower),
            Codec.FLOAT.optionalFieldOf("speedExplosionScale", DEFAULT.speedExplosionScale()).forGetter(ProjectilePhysicsProfile::speedExplosionScale),
            Codec.INT.optionalFieldOf("shrapnelFragments", DEFAULT.shrapnelFragments()).forGetter(ProjectilePhysicsProfile::shrapnelFragments),
            Codec.DOUBLE.optionalFieldOf("shrapnelRadius", DEFAULT.shrapnelRadius()).forGetter(ProjectilePhysicsProfile::shrapnelRadius),
            Codec.FLOAT.optionalFieldOf("shrapnelDamage", DEFAULT.shrapnelDamage()).forGetter(ProjectilePhysicsProfile::shrapnelDamage)
    ).apply(instance, ProjectilePhysicsProfile::new));

    public Optional<String> validationError() {
        Optional<String> error = ProfileValidation.nonNegative("mass", mass);
        if (mass == 0.0D) {
            return Optional.of("mass must be greater than zero");
        }
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("penetration", penetration);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("drag", drag);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("blockCostMultiplier", blockCostMultiplier);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("blockDamageMultiplier", blockDamageMultiplier);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("energyLossMultiplier", energyLossMultiplier);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("armorPiercing", armorPiercing);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("entityDamageMultiplier", entityDamageMultiplier);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("shockRadius", shockRadius);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("shockDamageMultiplier", shockDamageMultiplier);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("baseExplosionPower", baseExplosionPower);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("speedExplosionScale", speedExplosionScale);
        if (error.isPresent()) return error;
        if (shrapnelFragments < 0) return Optional.of("shrapnelFragments must not be negative");
        error = ProfileValidation.nonNegative("shrapnelRadius", shrapnelRadius);
        return error.isPresent() ? error : ProfileValidation.nonNegative("shrapnelDamage", shrapnelDamage);
    }

    public double kineticEnergy(double speed) {
        return 0.5D * Math.max(0.01D, mass) * speed * speed;
    }

    public double penetrationPower(double speed) {
        return kineticEnergy(speed) * Math.max(0.0D, penetration);
    }

    public float scaledExplosionPower(double speed) {
        if (baseExplosionPower <= 0.0F) {
            return 0.0F;
        }
        float speedFactor = 0.7F + (float) Math.log1p(Math.max(0.0D, speed)) * speedExplosionScale;
        return baseExplosionPower * Mth.clamp(speedFactor, 0.65F, 1.75F);
    }

    public double minContinueSpeed() {
        return 1.2D;
    }

    public double scaledShockRadius(double speed) {
        return shockRadius * Mth.clamp(0.85D + Math.log1p(Math.max(0.0D, speed)) * 0.18D, 0.75D, 1.45D);
    }

    public float scaledShockDamageMultiplier(double speed) {
        return (float) (shockDamageMultiplier
                * Mth.clamp(0.85D + Math.log1p(Math.max(0.0D, speed)) * 0.2D, 0.75D, 1.55D));
    }
}
