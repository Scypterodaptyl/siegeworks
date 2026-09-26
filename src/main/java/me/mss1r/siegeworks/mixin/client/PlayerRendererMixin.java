package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.client.pose.SiegePoseRenderContext;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"))
    private void siegeworks$beginFirstPersonHand(CallbackInfo ci) {
        SiegePoseRenderContext.enterFirstPersonHand();
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void siegeworks$endFirstPersonHand(CallbackInfo ci) {
        SiegePoseRenderContext.exitFirstPersonHand();
    }
}
