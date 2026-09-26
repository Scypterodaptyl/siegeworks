package me.mss1r.siegeworks.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class SiegeDeploymentItemLookup {
    private SiegeDeploymentItemLookup() {
    }

    public static @Nullable Item forEntity(@Nullable EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        ResourceLocation itemId = ResourceLocation.tryBuild(
                entityId.getNamespace(), entityId.getPath() + "_spawner");
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item instanceof SiegeDeploymentItem deploymentItem
                && deploymentItem.entityType() == entityType
                ? item
                : null;
    }
}
