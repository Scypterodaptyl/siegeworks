package me.mss1r.siegeworks.gameplay.maintenance;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

final class MaintenanceMaterials {
    private MaintenanceMaterials() {
    }

    static boolean has(ServerPlayer player, Map<ResourceLocation, Integer> items) {
        for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item == null || count(player, item) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    static boolean has(Container container, Map<ResourceLocation, Integer> items) {
        for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item == null || count(container, item) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    static void consume(ServerPlayer player, Map<ResourceLocation, Integer> items) {
        for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item != null) {
                consume(player.getInventory().items, item,
                        consume(player.getInventory().offhand, item, entry.getValue()));
            }
        }
    }

    static void consume(Container container, Map<ResourceLocation, Integer> items) {
        for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item == null) {
                continue;
            }
            int remaining = entry.getValue();
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !stack.is(item)) {
                    continue;
                }
                int removed = Math.min(stack.getCount(), remaining);
                stack.shrink(removed);
                remaining -= removed;
            }
        }
        container.setChanged();
    }

    static void returnOrDrop(AbstractSiegeEntity siege, ServerLevel level, @Nullable Container target,
                             ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || count <= 0) {
            return;
        }

        ItemStack remaining = new ItemStack(item, count);
        if (target != null) {
            remaining = insert(target, remaining);
        }
        while (!remaining.isEmpty()) {
            int stackSize = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack drop = remaining.split(stackSize);
            ItemEntity itemEntity = new ItemEntity(
                    level, siege.getX(), siege.getY() + 0.5D, siege.getZ(), drop);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    private static int count(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int count(Container container, Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int consume(List<ItemStack> stacks, Item item, int count) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.is(item)) {
                int removed = Math.min(stack.getCount(), count);
                stack.shrink(removed);
                count -= removed;
                if (count <= 0) {
                    return 0;
                }
            }
        }
        return count;
    }

    private static ItemStack insert(Container target, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < target.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = target.getItem(slot);
            if (!target.canPlaceItem(slot, remaining)) {
                continue;
            }

            if (existing.isEmpty()) {
                int moved = Math.min(remaining.getCount(),
                        Math.min(target.getMaxStackSize(), remaining.getMaxStackSize()));
                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                target.setItem(slot, inserted);
                remaining.shrink(moved);
                continue;
            }
            if (MinecraftVersionCompat.isSameItemAndData(existing, remaining)) {
                int limit = Math.min(target.getMaxStackSize(), existing.getMaxStackSize());
                int moved = Math.min(remaining.getCount(), limit - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }
        target.setChanged();
        return remaining;
    }
}
