package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class DraftMountControlMixin {
    @Inject(method = "tickRidden", at = @At("HEAD"), cancellable = true)
    private void siegeworks$leaveDrawnMountAlone(Player player, Vec3 travelVector, CallbackInfo ci) {
        AbstractHorse mount = (AbstractHorse) (Object) this;
        if (mount.getVehicle() instanceof AbstractSiegeEntity siege && siege.isTowed()) {
            ci.cancel();
        }
    }
}
