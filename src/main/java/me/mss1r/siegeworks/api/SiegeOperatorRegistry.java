package me.mss1r.siegeworks.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class SiegeOperatorRegistry {
    private static final Map<ResourceLocation, Predicate<LivingEntity>> OPERATOR_TYPES = new LinkedHashMap<>();

    private static volatile List<Predicate<LivingEntity>> predicates = List.of();

    private SiegeOperatorRegistry() {
    }

    public static synchronized void register(ResourceLocation id, Predicate<LivingEntity> predicate) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(predicate, "predicate");
        if (OPERATOR_TYPES.putIfAbsent(id, predicate) != null) {
            throw new IllegalArgumentException("A siege operator predicate is already registered for " + id);
        }
        predicates = List.copyOf(new ArrayList<>(OPERATOR_TYPES.values()));
    }

    public static boolean isRegisteredOperator(LivingEntity entity) {
        List<Predicate<LivingEntity>> current = predicates;
        for (int index = 0; index < current.size(); index++) {
            if (current.get(index).test(entity)) {
                return true;
            }
        }
        return false;
    }
}
