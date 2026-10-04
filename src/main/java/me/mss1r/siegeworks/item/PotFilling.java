package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contents of an incendiary pot. Filled in order: the base (one charcoal and one honeycomb), required for it to burn;
 * then up to {@link #ADDITIVE_SLOTS} additives, at most {@link #MAX_OF_A_KIND} of each; then a string wick, which seals
 * it and makes it lightable.
 */
public record PotFilling(List<Item> contents, boolean wick) {
    public static final int BASE_SLOTS = 2;
    public static final int ADDITIVE_SLOTS = 6;
    public static final int MAX_OF_A_KIND = 3;
    public static final PotFilling EMPTY = new PotFilling(List.of(), false);
    private static final String TAG_POT = "Pot";
    private static final String TAG_CONTENTS = "Contents";
    private static final String TAG_WICK = "Wick";

    public PotFilling {
        contents = List.copyOf(contents);
    }

    /** Filling of pots made before hand filling existed: base, two more charcoal, two more honeycomb, and a wick. */
    public static PotFilling standard() {
        return new PotFilling(List.of(Items.CHARCOAL, Items.HONEYCOMB,
                Items.CHARCOAL, Items.CHARCOAL, Items.HONEYCOMB, Items.HONEYCOMB), true);
    }

    public static boolean isBaseIngredient(Item item) {
        return item == Items.CHARCOAL || item == Items.HONEYCOMB;
    }

    public static boolean isIngredient(Item item) {
        return isBaseIngredient(item) || item == Items.BLAZE_POWDER || item == Items.GUNPOWDER;
    }

    public static boolean isWick(ItemStack stack) {
        return stack.is(Items.STRING);
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean hasBase() {
        return contents.size() >= BASE_SLOTS
                && contents.subList(0, BASE_SLOTS).contains(Items.CHARCOAL)
                && contents.subList(0, BASE_SLOTS).contains(Items.HONEYCOMB);
    }

    public List<Item> additives() {
        return hasBase() ? contents.subList(BASE_SLOTS, contents.size()) : List.of();
    }

    /** Count of this additive, excluding the base. */
    public int additivesOf(Item item) {
        return (int) additives().stream().filter(additive -> additive == item).count();
    }

    public boolean canLight() {
        return wick && hasBase();
    }

    /** This filling with one more ingredient, if it is accepted now. */
    public Optional<PotFilling> with(Item item) {
        if (wick || !isIngredient(item)) {
            return Optional.empty();
        }
        if (!hasBase()) {
            if (!isBaseIngredient(item) || contents.contains(item)) {
                return Optional.empty();
            }
        } else if (additives().size() >= ADDITIVE_SLOTS || additivesOf(item) >= MAX_OF_A_KIND) {
            return Optional.empty();
        }
        List<Item> more = new ArrayList<>(contents);
        more.add(item);
        return Optional.of(new PotFilling(more, false));
    }

    /** This filling with a wick, if the base is complete and there is no wick yet. */
    public Optional<PotFilling> withWick() {
        return wick || !hasBase() ? Optional.empty() : Optional.of(new PotFilling(contents, true));
    }

    /** Burst stats from the base and additives; {@link Burst#NONE} without a base. */
    public Burst burst() {
        if (!hasBase()) {
            return Burst.NONE;
        }
        double fireRadius = SiegeworksServerConfig.getIncendiaryBaseFireRadius();
        double fireChance = SiegeworksServerConfig.getIncendiaryBaseFireChance();
        int burnSeconds = SiegeworksServerConfig.getIncendiaryBaseBurnSeconds();
        double blastEnergy = 0.0D;
        for (Item additive : additives()) {
            if (additive == Items.CHARCOAL) {
                fireRadius += SiegeworksServerConfig.getIncendiaryCharcoalFireRadius();
            } else if (additive == Items.HONEYCOMB) {
                burnSeconds += SiegeworksServerConfig.getIncendiaryHoneycombBurnSeconds();
            } else if (additive == Items.BLAZE_POWDER) {
                fireChance += SiegeworksServerConfig.getIncendiaryBlazePowderFireChance();
            } else if (additive == Items.GUNPOWDER) {
                blastEnergy += SiegeworksServerConfig.getIncendiaryGunpowderBlastEnergy();
            }
        }
        return new Burst(fireRadius, Math.min(1.0D, fireChance), burnSeconds, blastEnergy);
    }

    /** Fire radius and chance, burn time for entities, and blast energy. */
    public record Burst(double fireRadius, double fireChance, int burnSeconds, double blastEnergy) {
        public static final Burst NONE = new Burst(0.0D, 0.0D, 0, 0.0D);
    }

    /** Contents as item stacks, including the wick. */
    public List<ItemStack> asItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : contents) {
            items.add(new ItemStack(item));
        }
        if (wick) {
            items.add(new ItemStack(Items.STRING));
        }
        return items;
    }

    /**
     * Filling stored on a pot item. Items without data hold their default: empty for a clay pot, the standard filling
     * for an incendiary pot, so pots made before hand filling still work.
     */
    public static PotFilling of(ItemStack stack) {
        CompoundTag data = MinecraftVersionCompat.customData(stack);
        if (data.contains(TAG_POT, Tag.TAG_COMPOUND)) {
            return load(data.getCompound(TAG_POT));
        }
        return stack.is(SiegeworksItems.FIRE_PROJECTILE.get()) ? standard() : EMPTY;
    }

    public static boolean isPot(ItemStack stack) {
        return stack.is(SiegeworksItems.CLAY_POT.get()) || stack.is(SiegeworksItems.FIRE_PROJECTILE.get());
    }

    /** Item for this filling: an incendiary pot once it has a wick, a clay pot before. */
    public ItemStack toItem() {
        boolean incendiary = wick;
        ItemStack stack = new ItemStack(incendiary ? SiegeworksItems.FIRE_PROJECTILE.get()
                : SiegeworksItems.CLAY_POT.get());
        if (!equals(incendiary ? standard() : EMPTY)) {
            MinecraftVersionCompat.editCustomData(stack, tag -> tag.put(TAG_POT, save()));
        }
        return stack;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Item item : contents) {
            list.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        tag.put(TAG_CONTENTS, list);
        tag.putBoolean(TAG_WICK, wick);
        return tag;
    }

    public static PotFilling load(CompoundTag tag) {
        List<Item> contents = new ArrayList<>();
        for (Tag entry : tag.getList(TAG_CONTENTS, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (isIngredient(item)) {
                contents.add(item);
            }
        }
        return new PotFilling(contents, tag.getBoolean(TAG_WICK));
    }

    /** Tooltip lines: whether the base is complete, and additive counts. */
    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        if (isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.empty").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (!hasBase()) {
            Item missing = contents.contains(Items.CHARCOAL) ? Items.HONEYCOMB : Items.CHARCOAL;
            lines.add(Component.translatable("tooltip.siegeworks.pot.base_missing", missing.getDescription())
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("tooltip.siegeworks.pot.base").withStyle(ChatFormatting.GRAY));
        if (!additives().isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.additives", counted(additives()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!wick) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.needs_wick").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    /** Each additive once, in insertion order, with its count. */
    private static Component counted(List<Item> items) {
        MutableComponent names = Component.empty();
        List<Item> kinds = items.stream().distinct().toList();
        for (int i = 0; i < kinds.size(); i++) {
            Item kind = kinds.get(i);
            if (i > 0) {
                names.append(", ");
            }
            long count = items.stream().filter(item -> item == kind).count();
            names.append(Component.translatable("tooltip.siegeworks.pot.count", kind.getDescription(), count));
        }
        return names;
    }
}
