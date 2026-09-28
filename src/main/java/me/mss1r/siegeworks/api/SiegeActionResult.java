package me.mss1r.siegeworks.api;

/** Result of one automated loading, winding or firing step. */
public enum SiegeActionResult {
    UNSUPPORTED,
    DENIED,
    MISSING_AMMUNITION,
    IN_PROGRESS,
    LOADED,
    FIRED
}
