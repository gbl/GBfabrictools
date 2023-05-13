package de.guntram.mcmod.fabrictools.GuiElements;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class GBButtonWidget extends ClickableWidget {

    protected GBButtonWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }
    public void renderButton(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = drawContext.getMatrices();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        drawContext.drawNineSlicedTexture(WIDGETS_TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.active ? 16777215 : 10526880;
        this.drawMessage(drawContext, minecraftClient.textRenderer, i | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }

    public void drawMessage(DrawContext drawContext, TextRenderer textRenderer, int color) {
        this.drawScrollableText(drawContext, textRenderer, 2, color);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
