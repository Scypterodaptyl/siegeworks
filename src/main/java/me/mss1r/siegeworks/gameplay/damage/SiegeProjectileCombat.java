package me.mss1r.siegeworks.gameplay.damage;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
//? if forge {
/*import net.minecraftforge.entity.PartEntity;
*///?} else {
import net.neoforged.neoforge.entity.PartEntity;
//?}
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import net.minecraftforge.common.ToolActions;
*///?} else {
import net.neoforged.neoforge.common.ItemAbilities;
//?}

public final class SiegeProjectileCombat {
    private SiegeProjectileCombat() {
    }

    public static boolean mayHit(SiegeProjectile projectile, Entity target) {
        Entity attacker = responsibleAttacker(projectile);
        Entity logicalTarget = logicalTarget(target);
        return logicalTarget != logicalTarget(projectile.getOwner())
                && logicalTarget != logicalTarget(attacker)
                && !isFriendlyFireProtected(attacker, logicalTarget);
    }

    public static boolean damage(SiegeProjectile projectile, Entity target,
                                 float damage, boolean breakShield) {
        LivingEntity livingTarget = livingTarget(target);
        if (livingTarget == null) {
            return false;
        }
        if (breakShield) {
            breakBlockingShield(livingTarget);
        }

        if (projectile.getOwner() instanceof AbstractSiegeEntity) {
            return target.hurt(target.damageSources().thrown(projectile, responsibleAttacker(projectile)), damage);
        }
        return target.hurt(target.damageSources().generic(), damage);
    }

    public static Entity responsibleAttacker(SiegeProjectile projectile) {
        Entity owner = projectile.getOwner();
        if (owner instanceof AbstractSiegeEntity siege) {
            Entity operator = siege.getOwner();
            return operator == null ? siege : operator;
        }
        return owner;
    }

    @Nullable
    public static Entity logicalTarget(@Nullable Entity target) {
        if (target instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return target;
    }

    @Nullable
    public static LivingEntity livingTarget(Entity target) {
        Entity logicalTarget = logicalTarget(target);
        return logicalTarget instanceof LivingEntity living ? living : null;
    }

    public static int targetId(Entity target) {
        return logicalTarget(target).getId();
    }

    private static boolean isFriendlyFireProtected(Entity attacker, Entity target) {
        if (attacker == null || attacker.getTeam() == null || attacker.getTeam().isAllowFriendlyFire()) {
            return false;
        }

        if (target instanceof AbstractSiegeEntity siegeTarget) {
            Entity targetOwner = siegeTarget.getOwner();
            if (targetOwner != null && targetOwner != siegeTarget) {
                return attacker.isAlliedTo(targetOwner);
            }
            return ("team:" + attacker.getTeam().getName()).equals(siegeTarget.getDeploymentGroup());
        }
        return attacker.isAlliedTo(target);
    }

    private static void breakBlockingShield(LivingEntity target) {
        if (!target.isBlocking()) {
            return;
        }

        ItemStack activeItem = target.getUseItem();
        //? if forge {
        /*if (!activeItem.canPerformAction(ToolActions.SHIELD_BLOCK)) {
        *///?} else {
        if (!activeItem.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
        //?}
            return;
        }

        //? if neoforge {
        target.onEquippedItemBroken(activeItem.getItem(), LivingEntity.getSlotForHand(target.getUsedItemHand()));
        //?}
        activeItem.shrink(1);
        //? if forge {
        /*target.broadcastBreakEvent(target.getUsedItemHand());
        *///?}
        target.stopUsingItem();
    }
}
