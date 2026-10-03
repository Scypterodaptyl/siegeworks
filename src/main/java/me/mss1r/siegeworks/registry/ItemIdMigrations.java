package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryManager;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
//?}

import java.util.Map;

public final class ItemIdMigrations {
    // Keep old saves and datapack references readable; newly saved stacks use the registered ID.
    private static final Map<ResourceLocation, ResourceLocation> RENAMES = Map.of(
            id("singijeon"), id("so_singijeon"),
            id("explosive_singijeon"), id("jung_singijeon"));

    private ItemIdMigrations() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ItemIdMigrations::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) {
            return;
        }
        //? if forge {
        /*RENAMES.forEach(RegistryManager.ACTIVE.getRegistry(Registries.ITEM)::addAlias);
        *///?} else {
        RENAMES.forEach(event.getRegistry()::addAlias);
        //?}
    }

    private static ResourceLocation id(String path) {
        //? if forge {
        /*return new ResourceLocation(Siegeworks.MOD_ID, path);
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, path);
        //?}
    }
}
