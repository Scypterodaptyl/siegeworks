package me.mss1r.siegeworks.api;

/** Coarse state intended for automation and UI, not animation timing. */
public enum SiegeOperationState {
    IDLE,
    LOADING,
    WINDING,
    READY,
    COOLDOWN
}
