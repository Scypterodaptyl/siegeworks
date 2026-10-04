package me.mss1r.siegeworks.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.block.IncendiaryPotBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface SiegeworksBlockEntities {
    DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    RegistrySupplier<BlockEntityType<IncendiaryPotBlockEntity>> INCENDIARY_POT = BLOCK_ENTITIES.register(
            "incendiary_pot", () -> BlockEntityType.Builder.of(IncendiaryPotBlockEntity::new,
                    SiegeworksBlocks.FIRE_PROJECTILE.get()).build(null));

    static void registerBlockEntities() {
        BLOCK_ENTITIES.register();
    }
}
