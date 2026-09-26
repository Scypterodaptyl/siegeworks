package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class TowerCrossbowBoltProjectile extends AbstractBoltProjectile {
    private static final TagKey<Block> PENETRABLE_BLOCKS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "tower_crossbow_bolt_penetrable"));

    public TowerCrossbowBoltProjectile(EntityType<? extends TowerCrossbowBoltProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public TowerCrossbowBoltProjectile(EntityType<? extends TowerCrossbowBoltProjectile> entityType,
                                       LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return SiegeworksItems.TOWER_CROSSBOW_BOLT.get();
    }

    @Override
    protected TagKey<Block> getPenetrableBlockTag() {
        return PENETRABLE_BLOCKS;
    }
}
