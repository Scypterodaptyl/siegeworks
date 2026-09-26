package me.mss1r.siegeworks.api;

import java.util.Set;

public interface SiegeAmmunitionControl extends SiegeArtilleryControl {
    Set<SiegeAmmunitionMode> getSupportedAmmunitionModes();

    SiegeAmmunitionMode getAutomatedAmmunitionMode();

    void setAutomatedAmmunitionMode(SiegeAmmunitionMode mode);

    default boolean supportsAmmunitionMode(SiegeAmmunitionMode mode) {
        return getSupportedAmmunitionModes().contains(mode);
    }
}
