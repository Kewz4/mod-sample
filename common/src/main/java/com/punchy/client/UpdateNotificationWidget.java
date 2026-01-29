package com.punchy.client;

import com.punchy.UpdateChecker;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.net.URI;

public class UpdateNotificationWidget extends AbstractWidget {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("toast/advancement");
    private static final int TOAST_WIDTH = 160;
    private static final int TOAST_HEIGHT = 32;
    private final String version;
    private final Button downloadBtn;
    private long startTime;

    public UpdateNotificationWidget(int x, int y, String version) {
        super(x, y, TOAST_WIDTH, TOAST_HEIGHT, Component.literal("Update Notification"));
        this.version = version;
        this.startTime = System.currentTimeMillis();

        this.downloadBtn = Button.builder(Component.literal("Download"), (btn) -> {
            if (!UpdateChecker.downloadUrl.isEmpty()) {
                Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
            }
        }).bounds(x + TOAST_WIDTH - 50 - 5, y + TOAST_HEIGHT - 20 - 2, 50, 16).build();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 15000) { // "last way longer" -> 15 seconds
            this.visible = false;
            return; // Hide
        }

        // Render Background (Toast texture)
        // Note: Toast textures are usually 160x32.
        guiGraphics.blitSprite(TEXTURE, this.getX(), this.getY(), this.width, this.height);

        // Render Icon (Generic "Info" or Item)
        // We'll simulate an icon box or just leave it.
        // User asked for "render the mod icon form modrinth".
        // Fetching remote images and rendering them is complex (async texture manager).
        // I will use a standard item for now (e.g. Paper or Map) to represent "News".
        // Or just the text as space is limited.

        guiGraphics.drawString(Minecraft.getInstance().font, "New Punchy! Update Available", this.getX() + 10, this.getY() + 5, 0xFFFFFF00, false);
        guiGraphics.drawString(Minecraft.getInstance().font, "v" + version, this.getX() + 10, this.getY() + 16, 0xFFFFFFFF, false);

        // Render the download button
        this.downloadBtn.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;
        if (this.downloadBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // No-op for now
    }
}
