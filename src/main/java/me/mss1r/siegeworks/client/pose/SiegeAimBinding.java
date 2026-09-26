package me.mss1r.siegeworks.client.pose;

public record SiegeAimBinding(
        float upperBodyPitchScale,
        float rightArmPitchScale,
        float leftArmPitchScale
) {
    public static final SiegeAimBinding NONE = new SiegeAimBinding(0.0F, 0.0F, 0.0F);
}
