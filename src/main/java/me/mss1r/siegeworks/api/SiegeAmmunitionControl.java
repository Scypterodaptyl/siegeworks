package me.mss1r.siegeworks.api;

import java.util.Set;

/** Exposes ammunition choices to command integrations. */
public interface SiegeAmmunitionControl extends SiegeArtilleryControl {
    Set<SiegeAmmunitionMode> getSupportedAmmunitionModes();

    SiegeAmmunitionMode getAutomatedAmmunitionMode();

    /** Changes the ammunition preference used by automated loading. */
    void setAutomatedAmmunitionMode(SiegeAmmunitionMode mode);

    default boolean supportsAmmunitionMode(SiegeAmmunitionMode mode) {
        return getSupportedAmmunitionModes().contains(mode);
    }
}
