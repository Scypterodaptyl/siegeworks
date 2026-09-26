package me.mss1r.siegeworks.client.projectile;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.projectile.FireArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FireArrowRenderer extends ArrowRenderer<FireArrowEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "textures/entity/projectiles/fire_arrow.png");

    public FireArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FireArrowEntity entity) {
        return TEXTURE;
    }
}
