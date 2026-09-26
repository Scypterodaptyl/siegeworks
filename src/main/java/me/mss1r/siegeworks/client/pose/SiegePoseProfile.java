package me.mss1r.siegeworks.client.pose;

public record SiegePoseProfile(
        SiegePoseDefinition definition,
        SiegeAimBinding aimBinding,
        SiegeHeadPolicy headPolicy,
        float legSwingScale
) {
}
