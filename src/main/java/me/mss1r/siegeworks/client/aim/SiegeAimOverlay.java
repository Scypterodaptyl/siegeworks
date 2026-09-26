package me.mss1r.siegeworks.client.aim;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public final class SiegeAimOverlay {
    private static final int RETICLE_RADIUS = 4;
    private static final int SHADOW_COLOR = 0x90000000;
    private static final int RETICLE_COLOR = 0xE6E8D8AD;

    private SiegeAimOverlay() {
    }

    public static void render(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }

        AbstractSiegeEntity siege = SiegeAimView.findControlledSiege(minecraft.player);
        if (siege == null || SiegeAimController.isFreeLookActive(minecraft.player, siege)) {
            return;
        }

        siege.updateRenderedAim(partialTick);
        float aimPitch = siege.getRenderedAimPitch();
        int x = screenWidth / 2;
        int y = screenHeight / 2;

        drawReticle(graphics, x + 1, y + 1, SHADOW_COLOR);
        drawReticle(graphics, x, y, RETICLE_COLOR);

        float elevation = -aimPitch;
        if (Math.abs(elevation) < 0.05F) {
            elevation = 0.0F;
        }
        String angle = String.format(Locale.ROOT, "%+.1f°", elevation);
        int angleX = x + RETICLE_RADIUS + 4;
        int angleY = y - minecraft.font.lineHeight / 2;
        if (angleX + minecraft.font.width(angle) < screenWidth - 2) {
            graphics.drawString(minecraft.font, angle, angleX, angleY, RETICLE_COLOR, true);
        }
    }

    private static void drawReticle(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 1, y - 4, x + 2, y - 3, color);
        graphics.fill(x - 1, y + 4, x + 2, y + 5, color);
        graphics.fill(x - 4, y - 1, x - 3, y + 2, color);
        graphics.fill(x + 4, y - 1, x + 5, y + 2, color);
        pixel(graphics, x - 3, y - 3, color);
        pixel(graphics, x + 3, y - 3, color);
        pixel(graphics, x - 3, y + 3, color);
        pixel(graphics, x + 3, y + 3, color);
    }

    private static void pixel(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }
}
