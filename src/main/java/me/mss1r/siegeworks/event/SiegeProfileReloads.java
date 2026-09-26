package me.mss1r.siegeworks.event;

import dev.architectury.registry.ReloadListenerRegistry;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.data.profile.JsonProfileReloadListener;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeEngineProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public final class SiegeProfileReloads {
    private SiegeProfileReloads() {
    }

    public static void register() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>(
                        "definitions/siege_engines",
                        "siege engine profiles",
                        SiegeEngineProfile.CODEC,
                        SiegeEngineProfile::validationError,
                        SiegeProfileCatalogs.ENGINES),
                id("siege_engine_profiles"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>(
                        "definitions/projectile_physics",
                        "projectile physics profiles",
                        ProjectilePhysicsProfile.CODEC,
                        ProjectilePhysicsProfile::validationError,
                        SiegeProfileCatalogs.PROJECTILES),
                id("projectile_physics_profiles"));
        SiegeMaintenanceData.registerReloadListener();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, path);
    }
}
