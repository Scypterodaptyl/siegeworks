package me.mss1r.siegeworks.client.pose;

public record ResolvedSiegePose(SiegePoseProfile profile, float modelPitch, float aimPitch,
                                boolean ladderClimbing, float blendWeight, float ladderInclineRadians,
                                float ladderUphillYawDegrees) {
    public ResolvedSiegePose(SiegePoseProfile profile, float modelPitch, float aimPitch) {
        this(profile, modelPitch, aimPitch, false, 1.0F, 0.0F, 0.0F);
    }
}
