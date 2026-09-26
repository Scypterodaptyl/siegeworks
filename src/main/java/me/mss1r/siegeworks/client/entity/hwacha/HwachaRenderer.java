package me.mss1r.siegeworks.client.entity.hwacha;

import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HwachaRenderer extends TowedSiegeRenderer<HwachaEntity> {
    public HwachaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HwachaModel());
    }
}
