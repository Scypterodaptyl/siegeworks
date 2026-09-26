package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camel.class)
public abstract class DraftCamelWalkAnimationMixin {
    private static final float CAMEL_WALK_ANIMATION_SCALE = 6.0F;

    @Inject(method = "updateWalkAnimation", at = @At("HEAD"), cancellable = true)
    private void siegeworks$useDrawnEngineWalkSpeed(float measuredTravel, CallbackInfo ci) {
        Camel camel = (Camel) (Object) this;
        if (!(camel.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) {
            return;
        }

        float travel = (float) siege.getVisualHorizontalTravelSpeed()
                * CAMEL_WALK_ANIMATION_SCALE;
        camel.walkAnimation.update(Math.min(travel, 1.0F), 0.2F);
        ci.cancel();
    }
}
