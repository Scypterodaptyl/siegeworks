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

public class ArcballistaBoltProjectile extends AbstractBoltProjectile {
    private static final TagKey<Block> PENETRABLE_BLOCKS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "arcballista_bolt_penetrable"));
    public ArcballistaBoltProjectile(EntityType<? extends ArcballistaBoltProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ArcballistaBoltProjectile(EntityType<? extends ArcballistaBoltProjectile> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return SiegeworksItems.ARCBALLISTA_BOLT.get();
    }

    protected double getDefaultGravity() {
        return 0.026D;
    }

    @Override
    protected int getMaxEntityPierces() {
        return 2;
    }

    @Override
    protected TagKey<Block> getPenetrableBlockTag() {
        return PENETRABLE_BLOCKS;
    }
}
