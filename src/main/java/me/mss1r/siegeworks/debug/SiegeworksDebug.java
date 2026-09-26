package me.mss1r.siegeworks.debug;

public final class SiegeworksDebug {
    private static final String INSTANT_FIRE_PROPERTY = "siegeworks.debug.instantFire";
    private static final String AUTO_DRIVE_PROPERTY = "siegeworks.debug.autoDrive";
    private static final String RECRUITS_PROPERTY = "siegeworks.debug.recruits";

    private static volatile boolean instantFire;
    private static volatile boolean autoDrive;
    private static volatile boolean recruits;

    static {
        reset();
    }

    private SiegeworksDebug() {
    }

    public static void reset() {
        instantFire = Boolean.getBoolean(INSTANT_FIRE_PROPERTY);
        autoDrive = Boolean.getBoolean(AUTO_DRIVE_PROPERTY);
        recruits = Boolean.getBoolean(RECRUITS_PROPERTY);
    }

    public static boolean instantFire() {
        return instantFire;
    }

    public static void setInstantFire(boolean enabled) {
        instantFire = enabled;
    }

    public static boolean autoDrive() {
        return autoDrive;
    }

    public static void setAutoDrive(boolean enabled) {
        autoDrive = enabled;
    }

    public static boolean recruits() {
        return recruits;
    }

    public static void setRecruits(boolean enabled) {
        recruits = enabled;
    }
}
