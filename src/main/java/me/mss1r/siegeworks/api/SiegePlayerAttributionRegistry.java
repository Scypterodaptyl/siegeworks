package me.mss1r.siegeworks.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/** Resolves an indirect operator or owner back to a player for block-breaking attribution. */
public final class SiegePlayerAttributionRegistry {
    private static final Map<ResourceLocation, Function<Entity, UUID>> RESOLVER_TYPES =
            new LinkedHashMap<>();
    private static volatile List<Function<Entity, UUID>> resolvers = List.of();

    private SiegePlayerAttributionRegistry() {
    }

    /** A resolver returns null when it does not know the entity. */
    public static synchronized void register(ResourceLocation id, Function<Entity, UUID> resolver) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resolver, "resolver");
        if (RESOLVER_TYPES.putIfAbsent(id, resolver) != null) {
            throw new IllegalArgumentException("A siege player attribution resolver is already registered for " + id);
        }
        resolvers = List.copyOf(new ArrayList<>(RESOLVER_TYPES.values()));
    }

    /** Resolvers run in registration order; the first non-null player id wins. */
    @Nullable
    public static UUID playerOwnerOf(Entity entity) {
        if (entity == null) {
            return null;
        }
        for (Function<Entity, UUID> resolver : resolvers) {
            UUID owner = resolver.apply(entity);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }
}
