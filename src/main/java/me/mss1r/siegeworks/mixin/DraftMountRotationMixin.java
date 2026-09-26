package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class DraftMountRotationMixin {
    @Unique
    private static final float WALK_ANIMATION_SCALE = 4.0F;

    @Inject(method = "updateWalkAnimation", at = @At("HEAD"), cancellable = true)
    private void siegeworks$useDrawnEngineWalkSpeed(float measuredTravel, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractHorse horse)) {
            return;
        }
        if (!(horse.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) {
            return;
        }

        float travel = (float) siege.getVisualHorizontalTravelSpeed() * WALK_ANIMATION_SCALE;
        horse.walkAnimation.update(Math.min(travel, 1.0F), 0.4F);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void siegeworks$keepSquareWithDrawnEngine(CallbackInfo ci) {
        if (!((Object) this instanceof AbstractHorse horse)) {
            return;
        }
        if (!(horse.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) {
            return;
        }

        horse.setYRot(siege.getYRot());
        horse.yRotO = siege.yRotO;
        horse.setYBodyRot(siege.yBodyRot);
        horse.yBodyRotO = siege.yBodyRotO;
        horse.setYHeadRot(siege.yBodyRot);
        horse.yHeadRotO = siege.yBodyRotO;
    }
}
