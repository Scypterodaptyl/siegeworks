package me.mss1r.siegeworks.client.entity.siegeladder;

import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import me.mss1r.siegeworks.client.entity.SiegeConstructionRenderer;

public class SiegeLadderRenderer extends SiegeConstructionRenderer<SiegeLadderEntity> {
    public SiegeLadderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SiegeLadderModel());
    }

    @Override
    public boolean shouldShowName(SiegeLadderEntity animatable) {
        return false;
    }

    @Override
    public boolean shouldRender(SiegeLadderEntity animatable, Frustum camera, double camX, double camY, double camZ) {
        double length = (1.0D + animatable.getSections()) * 3.0D;
        return super.shouldRender(animatable, camera, camX, camY, camZ)
                || camera.isVisible(animatable.getBoundingBox().inflate(length, length, length));
    }
}
