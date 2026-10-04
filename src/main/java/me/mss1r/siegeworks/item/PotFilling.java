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
 * What an incendiary pot holds. A pot is filled in order: first its base, a piece of charcoal and a honeycomb, which
 * is what makes it burn at all; then up to {@link #ADDITIVE_SLOTS} additives, each strengthening one thing; then a
 * string wick, after which it is sealed and can be lit.
 */
public record PotFilling(List<Item> contents, boolean wick) {
    public static final int BASE_SLOTS = 2;
    public static final int ADDITIVE_SLOTS = 4;
    public static final PotFilling EMPTY = new PotFilling(List.of(), false);
    private static final String TAG_POT = "Pot";
    private static final String TAG_CONTENTS = "Contents";
    private static final String TAG_WICK = "Wick";

    public PotFilling {
        contents = List.copyOf(contents);
    }

    /**
     * What a pot made before pots were filled by hand holds, and what a full pot evenly spread between fire and
     * stickiness holds: the base, two more charcoal, two more honeycombs, and a wick.
     */
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

    public boolean canLight() {
        return wick && hasBase();
    }

    /** The pot with one more ingredient in it, if it takes that one now. */
    public Optional<PotFilling> with(Item item) {
        if (wick || !isIngredient(item)) {
            return Optional.empty();
        }
        if (!hasBase()) {
            if (!isBaseIngredient(item) || contents.contains(item)) {
                return Optional.empty();
            }
        } else if (additives().size() >= ADDITIVE_SLOTS) {
            return Optional.empty();
        }
        List<Item> more = new ArrayList<>(contents);
        more.add(item);
        return Optional.of(new PotFilling(more, false));
    }

    /** The pot with its wick in, if its base is complete and it has none yet. */
    public Optional<PotFilling> withWick() {
        return wick || !hasBase() ? Optional.empty() : Optional.of(new PotFilling(contents, true));
    }

    /** What bursting does, from the base and each additive in turn; nothing for a pot without a base. */
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

    /** Fire on the ground round it, how long the creatures it splashes burn, and the blast of its powder. */
    public record Burst(double fireRadius, double fireChance, int burnSeconds, double blastEnergy) {
        public static final Burst NONE = new Burst(0.0D, 0.0D, 0, 0.0D);
    }

    /** Everything it holds, as items, wick included. */
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
     * What a pot item holds. An item says so itself unless it holds what its kind holds by default: nothing for a
     * clay pot, and for an incendiary pot the standard filling, so one made before pots were filled keeps working.
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

    /** The item that holds this: an incendiary pot once it has its wick, a clay pot until then. */
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

    /** Lines describing what a pot holds, for its tooltip. */
    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        if (isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.empty").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (!hasBase()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.base",
                    names(contents)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.siegeworks.pot.needs_base").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        lines.add(Component.translatable("tooltip.siegeworks.pot.base",
                names(contents.subList(0, BASE_SLOTS))).withStyle(ChatFormatting.GRAY));
        if (!additives().isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.additives",
                    names(additives())).withStyle(ChatFormatting.GRAY));
        }
        if (!wick) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.needs_wick").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static Component names(List<Item> items) {
        MutableComponent names = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(items.get(i).getDescription());
        }
        return names;
    }
}
