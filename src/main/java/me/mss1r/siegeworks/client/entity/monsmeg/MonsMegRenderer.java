package me.mss1r.siegeworks.client.entity.monsmeg;

import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MonsMegRenderer extends TowedSiegeRenderer<MonsMegEntity> {

    public MonsMegRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MonsMegModel());
    }

}
