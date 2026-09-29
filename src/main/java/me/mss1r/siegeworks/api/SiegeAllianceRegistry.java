package me.mss1r.siegeworks.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/** Lets faction mods declare two different scoreboard teams allied, so they share siege engines. */
public final class SiegeAllianceRegistry {
    private static final Map<ResourceLocation, BiPredicate<String, String>> RESOLVER_TYPES =
            new LinkedHashMap<>();
    private static volatile List<BiPredicate<String, String>> resolvers = List.of();

    private SiegeAllianceRegistry() {
    }

    public static synchronized void register(ResourceLocation id, BiPredicate<String, String> allied) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(allied, "allied");
        if (RESOLVER_TYPES.putIfAbsent(id, allied) != null) {
            throw new IllegalArgumentException("A siege alliance resolver is already registered for " + id);
        }
        resolvers = List.copyOf(new ArrayList<>(RESOLVER_TYPES.values()));
    }

    public static boolean allied(String firstTeam, String secondTeam) {
        for (BiPredicate<String, String> resolver : resolvers) {
            if (resolver.test(firstTeam, secondTeam)) {
                return true;
            }
        }
        return false;
    }
}
