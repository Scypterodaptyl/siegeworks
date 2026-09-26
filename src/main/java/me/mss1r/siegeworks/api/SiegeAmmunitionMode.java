package me.mss1r.siegeworks.api;

import java.util.Locale;

public enum SiegeAmmunitionMode {
    AUTO,
    STANDARD,
    EXPLOSIVE,
    INCENDIARY;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SiegeAmmunitionMode fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AUTO;
        }
    }
}
