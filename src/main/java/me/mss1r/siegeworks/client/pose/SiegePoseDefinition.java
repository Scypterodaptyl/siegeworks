package me.mss1r.siegeworks.client.pose;

public record SiegePoseDefinition(
        String source,
        SiegePoseRotation upperBody,
        SiegePoseRotation head,
        SiegePoseRotation body,
        SiegePoseRotation rightArm,
        SiegePoseRotation leftArm,
        SiegePoseRotation rightLeg,
        SiegePoseRotation leftLeg
) {
}
