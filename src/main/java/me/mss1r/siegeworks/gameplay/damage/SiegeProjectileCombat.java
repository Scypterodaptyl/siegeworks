package me.mss1r.siegeworks.gameplay.damage;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
        return target != projectile.getOwner()
                && target != attacker
                && !isFriendlyFireProtected(attacker, target);
    }

    public static boolean damage(SiegeProjectile projectile, LivingEntity target,
                                 float damage, boolean breakShield) {
        if (breakShield) {
            breakBlockingShield(target);
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
