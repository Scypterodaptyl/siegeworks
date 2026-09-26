package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FireArrowEntity extends Arrow {
    private static final float VELOCITY_SCALE = 0.7F;
    private static final int FIRE_DURATION_TICKS = 20 * 20;

    public FireArrowEntity(EntityType<? extends FireArrowEntity> type, Level level) {
        super(type, level);
    }

    public FireArrowEntity(Level level, LivingEntity shooter) {
        super(SiegeworksEntities.FIRE_ARROW.get(), level);
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ());
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(SiegeworksItems.FIRE_ARROW.get());
    }

    @Override
    public void shootFromRotation(Entity shooter, float xRot, float yRot, float yRotOffset, float velocity, float inaccuracy) {
        super.shootFromRotation(shooter, xRot, yRot, yRotOffset, velocity * VELOCITY_SCALE, inaccuracy);
    }

    public void applyFlintAndSteelIgnition(LivingEntity shooter) {
        if (shooter instanceof Player player && player.getInventory().hasAnyMatching(stack -> stack.is(Items.FLINT_AND_STEEL))) {
            setRemainingFireTicks(FIRE_DURATION_TICKS);
        }
    }
}
