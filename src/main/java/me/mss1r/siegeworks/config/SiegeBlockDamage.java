package me.mss1r.siegeworks.config;

/** Where siege weapons may break blocks. */
public enum SiegeBlockDamage {
    /** Anywhere. */
    EVERYWHERE,
    /** Only where the player behind the engine could break the block by hand. */
    RESPECT_PROTECTION,
    /** Nowhere: entities and engines still take damage, blocks don't. */
    NEVER
}
