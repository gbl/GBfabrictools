package de.guntram.mcmod.fabrictools.GuiElements;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class GBButtonWidget extends ClickableWidget {

    protected GBButtonWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        drawNineSlicedTexture(matrices, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.active ? 16777215 : 10526880;
        this.drawMessage(matrices, minecraftClient.textRenderer, i | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }

    public void drawMessage(MatrixStack matrices, TextRenderer textRenderer, int color) {
        this.drawScrollableText(matrices, textRenderer, 2, color);
    }

    private int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isSelected()) {
            i = 2;
        }

        return 46 + i * 20;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
