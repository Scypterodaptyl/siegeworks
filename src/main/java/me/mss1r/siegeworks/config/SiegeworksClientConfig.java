package me.mss1r.siegeworks.config;

public final class SiegeworksClientConfig {
    public static final SiegeworksConfigSpec SPEC;
    private static final SiegeworksConfigSpec.BooleanValue SHOW_RECRUIT_TROOPS_ON_MAP;
    private static final SiegeworksConfigSpec.BooleanValue SHOW_UPDATE_NOTICE;

    static {
        SiegeworksConfigSpec.Builder builder = new SiegeworksConfigSpec.Builder();
        builder.push("recruitsMap");
        SHOW_RECRUIT_TROOPS_ON_MAP = builder
                .comment("Show owned recruit group markers on the Recruits world map.")
                .define("showTroops", true);
        builder.pop();
        builder.push("updates");
        SHOW_UPDATE_NOTICE = builder
                .comment("Show a download link in chat once per game launch when a newer version is available.",
                        "The loader checks for updates; this only controls the chat message.")
                .define("showNotice", true);
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

    public static boolean showUpdateNotice() {
        return SHOW_UPDATE_NOTICE.get();
    }
}
