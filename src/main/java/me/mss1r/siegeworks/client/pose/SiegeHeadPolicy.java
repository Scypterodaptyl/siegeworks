package me.mss1r.siegeworks.client.pose;

public record SiegeHeadPolicy(Mode mode, float maxYaw, float maxPitch, boolean allowFreeLook) {
    public static SiegeHeadPolicy aim(boolean allowFreeLook) {
        return new SiegeHeadPolicy(Mode.AIM, 80.0F, 80.0F, allowFreeLook);
    }

    public static SiegeHeadPolicy look(float maxYaw, float maxPitch) {
        return new SiegeHeadPolicy(Mode.LOOK, maxYaw, maxPitch, false);
    }

    public enum Mode {
        AIM,
        LOOK
    }
}
