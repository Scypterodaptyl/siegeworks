package me.mss1r.siegeworks.particle;

import me.mss1r.siegeworks.Siegeworks;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public final class SiegeworksParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.PARTICLE_TYPE);

    public static final RegistrySupplier<SimpleParticleType> SIEGE_SMOKE =
            PARTICLE_TYPES.register("siege_smoke", () -> new SimpleParticleType(false));
    public static final RegistrySupplier<SimpleParticleType> HEAVY_SIEGE_SMOKE =
            PARTICLE_TYPES.register("heavy_siege_smoke", () -> new SimpleParticleType(false));
    public static final RegistrySupplier<SimpleParticleType> MUZZLE_PLUME =
            PARTICLE_TYPES.register("muzzle_plume", () -> new SimpleParticleType(false));
    public static final RegistrySupplier<SimpleParticleType> IMPACT_SMOKE_PLUME =
            PARTICLE_TYPES.register("impact_smoke_plume", () -> new SimpleParticleType(false));

    private SiegeworksParticles() {
    }

    public static void register() {
        PARTICLE_TYPES.register();
    }
}
