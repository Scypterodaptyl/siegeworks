package me.mss1r.siegeworks.gameplay.crew;

import me.mss1r.siegeworks.api.SiegeOperatorRegistry;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SiegeCrewController {
    private final AbstractSiegeEntity siege;

    public SiegeCrewController(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public static boolean supportsDirectOperator(Entity entity) {
        return entity instanceof Player
                || entity instanceof LivingEntity living && SiegeOperatorRegistry.isRegisteredOperator(living);
    }

    public boolean canOperate(LivingEntity operator) {
        return supportsDirectOperator(operator)
                && (siege.getPassengers().isEmpty() || siege.hasPassenger(operator));
    }

    public boolean mount(LivingEntity operator) {
        if (isOperator(operator)) {
            return true;
        }
        return siege.canOperate(operator) && operator.startRiding(siege);
    }

    public void dismount(LivingEntity operator) {
        if (!isOperator(operator)) {
            return;
        }
        clearInput(operator);
        operator.stopRiding();
    }

    public boolean isOperator(LivingEntity operator) {
        if (operator.getVehicle() == siege && siege.shouldPassengerControlMovement(operator)) {
            return true;
        }
        return operator == siege.getReinsHolder();
    }

    public void setMovement(LivingEntity operator, float forward, float steering) {
        if (isOperator(operator)) {
            siege.setMovementInput(forward, steering);
        }
    }

    public void clearInput(LivingEntity operator) {
        if (isOperator(operator)) {
            siege.setMovementInput(0.0F, 0.0F);
        }
    }

    @Nullable
    public Entity movementController() {
        for (Entity passenger : siege.getPassengers()) {
            if (siege.shouldPassengerControlMovement(passenger)
                    && (passenger instanceof LivingEntity || passenger.getFirstPassenger() instanceof Player)) {
                return passenger;
            }
        }
        return null;
    }

    @Nullable
    public LivingEntity controllingPassenger() {
        for (Entity passenger : siege.getPassengers()) {
            if (!siege.shouldPassengerControlRotation(passenger)) {
                continue;
            }
            if (passenger.getFirstPassenger() instanceof LivingEntity livingPassenger) {
                return livingPassenger;
            }
            if (passenger instanceof LivingEntity livingPassenger) {
                return livingPassenger;
            }
        }
        return null;
    }

    public void stopAfterControllerRemoved() {
        siege.setMovementInput(0.0F, 0.0F);
        siege.setCurrentDriveSpeed(0.0D);
        Vec3 movement = siege.getDeltaMovement();
        siege.setDeltaMovement(0.0D, movement.y, 0.0D);
    }
}
