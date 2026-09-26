package me.mss1r.siegeworks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            shift = At.Shift.AFTER
    ))
    private void siegeworks$transformCapeWithRiderPose(PoseStack poseStack, MultiBufferSource bufferSource,
                                                        int packedLight, AbstractClientPlayer player,
                                                        float limbSwing, float limbSwingAmount, float partialTick,
                                                        float ageInTicks, float netHeadYaw, float headPitch,
                                                        CallbackInfo ci) {
        if (!(player.getVehicle() instanceof AbstractSiegeEntity)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = ((CapeLayer) (Object) this).getParentModel();
        ModelPart body = playerModel.body;
        poseStack.translate(body.x / 16.0F, body.y / 16.0F, body.z / 16.0F);
        poseStack.mulPose(Axis.ZP.rotation(body.zRot));
        poseStack.mulPose(Axis.YP.rotation(body.yRot));
        poseStack.mulPose(Axis.XP.rotation(body.xRot));
    }
}
