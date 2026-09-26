package me.mss1r.siegeworks.client.projectile;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.client.projectile.TrebuchetProjectileModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
/*import net.minecraftforge.client.model.data.ModelData;
*///?} else {
import net.neoforged.neoforge.client.model.data.ModelData;
//?}

public class TrebuchetProjectileRenderer extends EntityRenderer<TrebuchetProjectile> {
    private final EntityModel<Entity> model;

    public TrebuchetProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TrebuchetProjectileModel(TrebuchetProjectileModel.getTexturedModelData().bakeRoot());
    }

    public void render(TrebuchetProjectile entity, float yaw, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        BlockState stoneState = SiegeAmmo.stoneBlockState(entity.getTextureName());
        if (stoneState != null && (entity.getType() == SiegeworksEntities.MANGONEL_PROJECTILE.get()
                || entity.getType() == SiegeworksEntities.TREBUCHET_PROJECTILE.get())) {
            poseStack.pushPose();
            float spin = (entity.tickCount + tickDelta) * 12.0F;
            float scale = entity.getType() == SiegeworksEntities.TREBUCHET_PROJECTILE.get() ? 0.75F : 0.5F;
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.65F));
            poseStack.translate(-scale / 2.0F, -scale / 2.0F, -scale / 2.0F);
            poseStack.scale(scale, scale, scale);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    stoneState, poseStack, multiBufferSource, light, OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY, null);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        if (entity.getType() == SiegeworksEntities.TREBUCHET_PROJECTILE.get()) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        }

        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.armorCutoutNoCull(this.getTextureLocation(entity)));
        //? if forge {
        /*model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
        poseStack.popPose();
    }

    public ResourceLocation getTextureLocation(TrebuchetProjectile entity) {
        String textureName = entity.getTextureName();
        if (textureName == null || textureName.isBlank()) {
            textureName = "stone";
        }
        if ("fire".equals(textureName)) {
            return ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "textures/block/fire_projectile.png");
        }
        return ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "textures/entity/trebuchet_projectile_" + textureName + ".png");
    }
}
