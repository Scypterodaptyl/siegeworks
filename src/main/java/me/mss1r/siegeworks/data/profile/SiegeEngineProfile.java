package me.mss1r.siegeworks.data.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record SiegeEngineProfile(double baseDamage, float projectileSpeed, float accuracyMultiplier,
                                 SiegeDamageRules damageRules, ScattershotProfile scattershot) {
    public static final SiegeEngineProfile DEFAULT = new SiegeEngineProfile(
            25.0D, 140.0F, 1.0F, SiegeDamageRules.DEFAULT, ScattershotProfile.DEFAULT
    );

    public static final Codec<SiegeEngineProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("baseDamage", 25.0D).forGetter(SiegeEngineProfile::baseDamage),
            Codec.FLOAT.optionalFieldOf("projectileSpeed", 140.0F).forGetter(SiegeEngineProfile::projectileSpeed),
            Codec.FLOAT.optionalFieldOf("accuracyMultiplier", 1.2F).forGetter(SiegeEngineProfile::accuracyMultiplier),
            SiegeDamageRules.CODEC.optionalFieldOf("damageConfig", SiegeDamageRules.DEFAULT)
                    .forGetter(SiegeEngineProfile::damageRules),
            ScattershotProfile.CODEC.optionalFieldOf("scattershot", ScattershotProfile.DEFAULT)
                    .forGetter(SiegeEngineProfile::scattershot)
    ).apply(instance, SiegeEngineProfile::new));

    public Optional<String> validationError() {
        Optional<String> error = ProfileValidation.nonNegative("baseDamage", baseDamage);
        if (error.isPresent()) {
            return error;
        }
        error = ProfileValidation.nonNegative("projectileSpeed", projectileSpeed);
        if (error.isPresent()) {
            return error;
        }
        error = ProfileValidation.nonNegative("accuracyMultiplier", accuracyMultiplier);
        if (error.isPresent()) {
            return error;
        }
        error = damageRules.validationError();
        return error.isPresent() ? error : scattershot.validationError();
    }
}
