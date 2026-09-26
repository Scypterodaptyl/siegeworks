package me.mss1r.siegeworks.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

//? if forge {
/*import net.minecraftforge.common.util.FakePlayer;
*///?} else {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.FakePlayer;
//?}

import java.util.function.Consumer;

public final class MinecraftVersionCompat {
    private MinecraftVersionCompat() {
    }

    public static double entityInteractionRange(Player player) {
        //? if forge {
        /*return player.getEntityReach();
        *///?} else {
        return player.entityInteractionRange();
        //?}
    }

    public static void damageHeldItem(ItemStack stack, int amount, LivingEntity owner, InteractionHand hand) {
        //? if forge {
        /*stack.hurtAndBreak(amount, owner, entity -> entity.broadcastBreakEvent(hand));
        *///?} else {
        stack.hurtAndBreak(amount, owner, LivingEntity.getSlotForHand(hand));
        //?}
    }

    public static boolean isSameItemAndData(ItemStack first, ItemStack second) {
        //? if forge {
        /*return ItemStack.isSameItemSameTags(first, second);
        *///?} else {
        return ItemStack.isSameItemSameComponents(first, second);
        //?}
    }

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    public static CompoundTag customData(ItemStack stack) {
        //? if forge {
        /*CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag;
        *///?} else {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
        //?}
    }

    public static void editCustomData(ItemStack stack, Consumer<CompoundTag> editor) {
        //? if forge {
        /*editor.accept(stack.getOrCreateTag());
        *///?} else {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, editor);
        //?}
    }

    public static boolean hasCustomName(ItemStack stack) {
        //? if forge {
        /*return stack.hasCustomHoverName();
        *///?} else {
        return stack.has(DataComponents.CUSTOM_NAME);
        //?}
    }

    public static void setCustomName(ItemStack stack, Component name) {
        //? if forge {
        /*stack.setHoverName(name);
        *///?} else {
        stack.set(DataComponents.CUSTOM_NAME, name);
        //?}
    }

    public static ItemStack readItem(CompoundTag tag, HolderLookup.Provider registries) {
        //? if forge {
        /*return ItemStack.of(tag);
        *///?} else {
        return ItemStack.parseOptional(registries, tag);
        //?}
    }

    public static CompoundTag writeItem(ItemStack stack, HolderLookup.Provider registries) {
        //? if forge {
        /*return stack.save(new CompoundTag());
        *///?} else {
        return (CompoundTag) stack.save(registries, new CompoundTag());
        //?}
    }

    public static SoundEvent genericExplodeSound() {
        //? if forge {
        /*return SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        return SoundEvents.GENERIC_EXPLODE.value();
        //?}
    }

    public static SoundEvent leatherEquipSound() {
        //? if forge {
        /*return SoundEvents.ARMOR_EQUIP_LEATHER;
        *///?} else {
        return SoundEvents.ARMOR_EQUIP_LEATHER.value();
        //?}
    }
}
