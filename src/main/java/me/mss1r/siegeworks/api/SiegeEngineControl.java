package me.mss1r.siegeworks.api;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;

public interface SiegeEngineControl {
    boolean canOperate(LivingEntity operator);

    boolean mountOperator(LivingEntity operator);

    void dismountOperator(LivingEntity operator);

    boolean isOperator(LivingEntity operator);

    void setOperatorMovement(LivingEntity operator, float forward, float steering);

    void setOperatorAim(LivingEntity operator, float yaw, float pitch);

    void clearOperatorInput(LivingEntity operator);

    SiegeOperationState getOperationState();

    default SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        return SiegeActionResult.UNSUPPORTED;
    }

    default void cancelPrimaryAction(LivingEntity operator) {
    }

    boolean hasAmmoLoaded();

    int getCooldown();

    int getWindingTime();
}
