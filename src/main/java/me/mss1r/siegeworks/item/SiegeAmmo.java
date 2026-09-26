package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SiegeAmmo {
    public static final String AMMO_STONE = "stone";
    public static final String AMMO_FIRE = "fire";
    public static final String AMMO_GRAPESHOT = "siegeworks:grapeshot";
    public static final String AMMO_IRON_SCATTERSHOT = "minecraft:iron_nugget";

    private static final List<Item> STONE_PROJECTILE_ITEMS = List.of(
            Items.STONE,
            Items.COBBLESTONE,
            Items.MOSSY_COBBLESTONE,
            Items.DEEPSLATE,
            Items.COBBLED_DEEPSLATE,
            Items.BLACKSTONE,
            Items.ANDESITE,
            Items.DIORITE,
            Items.GRANITE,
            Items.TUFF,
            Items.CALCITE,
            Items.DRIPSTONE_BLOCK
    );

    private SiegeAmmo() {
    }

    public static boolean isStoneProjectile(ItemStack stack) {
        return isStoneProjectile(stack.getItem());
    }

    public static boolean isStoneProjectile(Item item) {
        return STONE_PROJECTILE_ITEMS.contains(item);
    }

    public static String stoneAmmoKey(Item item) {
        return isStoneProjectile(item) ? BuiltInRegistries.ITEM.getKey(item).toString() : AMMO_STONE;
    }

    @Nullable
    public static BlockState stoneBlockState(String ammo) {
        if (isGrapeshotAmmoKey(ammo)) {
            return SiegeworksBlocks.GRAPESHOT.get().defaultBlockState();
        }

        Item projectile = AMMO_STONE.equals(ammo) ? Items.STONE : null;
        if (projectile == null) {
            for (Item item : STONE_PROJECTILE_ITEMS) {
                if (BuiltInRegistries.ITEM.getKey(item).toString().equals(ammo)) {
                    projectile = item;
                    break;
                }
            }
        }

        return projectile instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;
    }

    public static boolean isStoneAmmoKey(String ammo) {
        return AMMO_STONE.equals(ammo) || "minecraft:stone".equals(ammo)
                || "minecraft:cobblestone".equals(ammo) || "minecraft:mossy_cobblestone".equals(ammo)
                || "minecraft:deepslate".equals(ammo) || "minecraft:cobbled_deepslate".equals(ammo)
                || "minecraft:blackstone".equals(ammo) || "minecraft:andesite".equals(ammo)
                || "minecraft:diorite".equals(ammo) || "minecraft:granite".equals(ammo)
                || "minecraft:tuff".equals(ammo) || "minecraft:calcite".equals(ammo)
                || "minecraft:dripstone_block".equals(ammo);
    }

    public static boolean isFireAmmoKey(String ammo) {
        return AMMO_FIRE.equals(ammo) || "siegeworks:fire_projectile".equals(ammo);
    }

    public static boolean isGrapeshot(ItemStack stack) {
        return stack.is(SiegeworksItems.GRAPESHOT.get());
    }

    public static boolean isGrapeshotAmmoKey(String ammo) {
        return AMMO_GRAPESHOT.equals(ammo);
    }

    public static boolean isIronScattershotAmmoKey(String ammo) {
        return AMMO_IRON_SCATTERSHOT.equals(ammo);
    }

    public static Component stoneAmmoDescription() {
        return Component.literal("Stone / Cobblestone / Deepslate / Blackstone / Andesite / Diorite / Granite / Tuff");
    }
}
