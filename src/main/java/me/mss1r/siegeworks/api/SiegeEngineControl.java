package me.mss1r.siegeworks.api;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;

/** Common control surface used by recruits and external command mods. */
public interface SiegeEngineControl {
    boolean canOperate(LivingEntity operator);

    boolean mountOperator(LivingEntity operator);

    void dismountOperator(LivingEntity operator);

    boolean isOperator(LivingEntity operator);

    void setOperatorMovement(LivingEntity operator, float forward, float steering);

    void setOperatorAim(LivingEntity operator, float yaw, float pitch);

    void clearOperatorInput(LivingEntity operator);

    SiegeOperationState getOperationState();

    /**
     * Advances the machine's main action by one tick. The inventory supplies any required items.
     */
    default SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        return SiegeActionResult.UNSUPPORTED;
    }

    /** Clears an action that should not continue after the current order ends. */
    default void cancelPrimaryAction(LivingEntity operator) {
    }

    boolean hasAmmoLoaded();

    int getCooldown();

    int getWindingTime();
}
