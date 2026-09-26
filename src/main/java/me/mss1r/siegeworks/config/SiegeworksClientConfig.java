package me.mss1r.siegeworks.config;

public final class SiegeworksClientConfig {
    public static final SiegeworksConfigSpec SPEC;
    private static final SiegeworksConfigSpec.BooleanValue SHOW_RECRUIT_TROOPS_ON_MAP;

    static {
        SiegeworksConfigSpec.Builder builder = new SiegeworksConfigSpec.Builder();
        builder.push("recruitsMap");
        SHOW_RECRUIT_TROOPS_ON_MAP = builder
                .comment("Show owned recruit group markers on the Recruits world map.")
                .define("showTroops", true);
        builder.pop();
        SPEC = builder.build();
    }

    private SiegeworksClientConfig() {
    }

    public static boolean showRecruitTroopsOnMap() {
        return SHOW_RECRUIT_TROOPS_ON_MAP.get();
    }

    public static void setShowRecruitTroopsOnMap(boolean visible) {
        SHOW_RECRUIT_TROOPS_ON_MAP.set(visible);
    }
}
