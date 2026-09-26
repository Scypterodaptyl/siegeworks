package me.mss1r.siegeworks.client.harness;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public class HarnessLayer<T extends AbstractHorse, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public HarnessLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T mount,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(mount.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) {
            return;
        }

        MountHarness harness = MountHarness.forMount(mount);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(harness.texture()));
        harness.model().render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
