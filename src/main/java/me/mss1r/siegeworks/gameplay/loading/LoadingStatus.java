package me.mss1r.siegeworks.gameplay.loading;

public enum LoadingStatus {
    LOADING_AMMUNITION("siege.loading.state.ammunition"),
    LOADING_POWDER("siege.loading.state.powder"),
    RAMMING_CHARGE("siege.loading.state.ramming_charge"),
    RAMMING_PROJECTILE("siege.loading.state.ramming_projectile"),
    PRIMING("siege.loading.state.priming");

    private final String translationKey;

    LoadingStatus(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
