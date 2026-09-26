package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class DraftMountAnimationMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void siegeworks$walkWhileDrawingEngine(EntityModel model, Entity entity,
                                                   float limbSwing, float limbSwingAmount,
                                                   float ageInTicks, float netHeadYaw,
                                                   float headPitch) {
        if (entity instanceof AbstractHorse mount
                && mount.getVehicle() instanceof AbstractSiegeEntity siege && siege.isTowed()) {
            limbSwing = mount.walkAnimation.position(ageInTicks - mount.tickCount);
            limbSwingAmount = Math.min(
                    mount.walkAnimation.speed(ageInTicks - mount.tickCount), 1.0F);
        }

        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
