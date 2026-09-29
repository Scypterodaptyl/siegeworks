package me.mss1r.siegeworks.integration.recruits;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class SiegeTypeButton extends AbstractButton {
    private static final int SIZE = 20;
    private static final int SELECTED_BORDER = 0xFFFFFFFF;

    private final ItemStack icon;
    private final Runnable action;
    private final boolean selected;

    SiegeTypeButton(int x, int y, SiegeCommandType type, boolean selected, Runnable action) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.icon = type.icon();
        this.action = action;
        this.selected = selected;
        setTooltip(Tooltip.create(icon.getHoverName()));
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (selected) {
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, SELECTED_BORDER);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, SELECTED_BORDER);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, SELECTED_BORDER);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, SELECTED_BORDER);
        }
        graphics.renderItem(icon, getX() + 2, getY() + 2);
    }
}
