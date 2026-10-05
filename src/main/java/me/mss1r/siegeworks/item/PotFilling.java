package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.siegeworks.data.profile.PotFillingProfile.Effect;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contents of an incendiary pot, filled in order: the base, required for it to burn; then additives; then a wick,
 * which seals it and makes it lightable. Which items those are, and what they do, comes from
 * {@link PotFillingProfile}.
 */
public record PotFilling(List<Item> contents, boolean wick) {
    public static final PotFilling EMPTY = new PotFilling(List.of(), false);
    private static final String TAG_POT = "Pot";
    private static final String TAG_CONTENTS = "Contents";
    private static final String TAG_WICK = "Wick";
    /** What pots held before hand filling existed. */
    private static final List<Item> LEGACY_STANDARD = List.of(Items.CHARCOAL, Items.HONEYCOMB,
            Items.CHARCOAL, Items.CHARCOAL, Items.HONEYCOMB, Items.HONEYCOMB);

    public PotFilling {
        contents = List.copyOf(contents);
    }

    /**
     * Filling of pots made before hand filling existed: base, two more charcoal, two more honeycomb, and a wick.
     * Items the current profile no longer accepts are left out.
     */
    public static PotFilling standard() {
        return rebuild(LEGACY_STANDARD, true);
    }

    public static boolean isIngredient(Item item) {
        return profile().isIngredient(item);
    }

    public static boolean isWick(ItemStack stack) {
        return profile().isWick(stack);
    }

    private static PotFillingProfile profile() {
        return PotFillingProfile.current();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    /** Base ingredients still missing, by profile key. Empty once the base is complete. */
    public Map<String, Integer> missingBase() {
        return baseState().missing();
    }

    public boolean hasBase() {
        return baseState().missing().isEmpty();
    }

    public List<Item> additives() {
        BaseState state = baseState();
        return state.missing().isEmpty() ? contents.subList(state.used(), contents.size()) : List.of();
    }

    /** Count of additives filed under the same profile key as {@code item}. */
    public int additivesOf(Item item) {
        String key = profile().additiveKey(item);
        if (key == null) {
            return 0;
        }
        return (int) additives().stream().filter(additive -> key.equals(profile().additiveKey(additive))).count();
    }

    public boolean canLight() {
        return wick && hasBase();
    }

    /** This filling with one more ingredient, if it is accepted now. */
    public Optional<PotFilling> with(Item item) {
        if (wick) {
            return Optional.empty();
        }
        PotFillingProfile profile = profile();
        Map<String, Integer> missing = missingBase();
        if (!missing.isEmpty()) {
            if (PotFillingProfile.firstMatch(missing.keySet(), item) == null) {
                return Optional.empty();
            }
        } else if (profile.additiveKey(item) == null || additives().size() >= profile.additiveSlots()
                || additivesOf(item) >= profile.maxOfAKind()) {
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
        PotFillingProfile profile = profile();
        Effect total = profile.baseBurst();
        for (Item additive : additives()) {
            String key = profile.additiveKey(additive);
            if (key != null) {
                total = total.plus(profile.additives().get(key));
            }
        }
        return new Burst(total.fireRadius(), Math.min(1.0D, total.fireChance()), total.burnSeconds(),
                total.blastEnergy());
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
            items.add(wickItem());
        }
        return items;
    }

    private static ItemStack wickItem() {
        String key = profile().wick();
        if (!key.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            return id == null ? new ItemStack(Items.STRING) : new ItemStack(BuiltInRegistries.ITEM.get(id));
        }
        return BuiltInRegistries.ITEM.stream().filter(item -> PotFillingProfile.matches(key, item)).findFirst()
                .map(ItemStack::new).orElseGet(() -> new ItemStack(Items.STRING));
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

    /** Reads saved contents, keeping only what the current profile still accepts in that order. */
    public static PotFilling load(CompoundTag tag) {
        List<Item> contents = new ArrayList<>();
        for (Tag entry : tag.getList(TAG_CONTENTS, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                contents.add(BuiltInRegistries.ITEM.get(id));
            }
        }
        return rebuild(contents, tag.getBoolean(TAG_WICK));
    }

    private static PotFilling rebuild(List<Item> items, boolean wick) {
        PotFilling filling = EMPTY;
        for (Item item : items) {
            filling = filling.with(item).orElse(filling);
        }
        return wick ? filling.withWick().orElse(filling) : filling;
    }

    /** Tooltip lines: whether the base is complete, and additive counts. */
    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        if (isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.empty").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        Map<String, Integer> missing = missingBase();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.base_missing", names(missing))
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("tooltip.siegeworks.pot.base").withStyle(ChatFormatting.GRAY));
        if (!additives().isEmpty()) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.additives", counted(additives()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!wick) {
            lines.add(Component.translatable("tooltip.siegeworks.pot.needs_wick",
                    PotFillingProfile.name(profile().wick())).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    /** Profile keys with counts above one shown as "name ×count", joined by commas. */
    public static Component names(Map<String, Integer> keys) {
        MutableComponent names = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : keys.entrySet()) {
            if (!first) {
                names.append(", ");
            }
            first = false;
            Component name = PotFillingProfile.name(entry.getKey());
            names.append(entry.getValue() > 1
                    ? Component.translatable("tooltip.siegeworks.pot.count", name, entry.getValue()) : name);
        }
        return names;
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

    /** Base keys still missing, and how many contents items went into the base so far. */
    private BaseState baseState() {
        Map<String, Integer> missing = new LinkedHashMap<>(sortedBase());
        int used = 0;
        while (used < contents.size() && !missing.isEmpty()) {
            String key = PotFillingProfile.firstMatch(missing.keySet(), contents.get(used));
            if (key == null) {
                break;
            }
            missing.merge(key, -1, Integer::sum);
            missing.values().removeIf(count -> count <= 0);
            used++;
        }
        return new BaseState(missing, used);
    }

    /** The profile's base in a stable order, so tooltips list it the same way every time. */
    private static Map<String, Integer> sortedBase() {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        profile().base().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private record BaseState(Map<String, Integer> missing, int used) {
    }
}
