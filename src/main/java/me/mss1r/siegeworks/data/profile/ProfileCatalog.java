package me.mss1r.siegeworks.data.profile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ProfileCatalog<T> {
    private final T fallback;
    private final AtomicReference<Map<ResourceLocation, T>> snapshot =
            new AtomicReference<>(Map.of());

    public ProfileCatalog(T fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public T forEntity(EntityType<?> entityType) {
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    public T get(ResourceLocation id) {
        return snapshot.get().getOrDefault(id, fallback);
    }

    public boolean contains(ResourceLocation id) {
        return snapshot.get().containsKey(id);
    }

    public Map<ResourceLocation, T> snapshot() {
        return snapshot.get();
    }

    void publish(Map<ResourceLocation, T> profiles) {
        snapshot.set(Map.copyOf(profiles));
    }
}
