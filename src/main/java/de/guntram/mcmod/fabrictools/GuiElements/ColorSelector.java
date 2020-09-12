package de.guntram.mcmod.fabrictools.GuiElements;

import com.mojang.blaze3d.platform.GlStateManager;
import de.guntram.mcmod.fabrictools.GuiModOptions;
import de.guntram.mcmod.fabrictools.Types.ConfigurationMinecraftColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;


public class ColorSelector extends AbstractButtonWidget {

    private ColorButton buttons[];
    private ConfigurationMinecraftColor currentColor;
    private String option;
    private AbstractButtonWidget element;
    private GuiModOptions optionScreen;

    private int standardColors[] = { 
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
        0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    public ColorSelector(GuiModOptions optionScreen, Text message) {
        super(0, 0, 120, 120, message);
        buttons = new ColorButton[16];
    }

    public void init() {
        Text buttonText = new LiteralText("");
        this.x = (optionScreen.width - width) / 2;
        this.y = (optionScreen.height - height) / 2;
        for (int i=0; i<16; i++) {
            buttons[i]=new ColorButton(
                this, x + (i/4) * 25, y + (i%4)*25, 20, 20, buttonText, i, standardColors[i]
            );
        }
        visible = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (visible) {
            for (int i=0; i<buttons.length; i++) {
                if (buttons[i].mouseClicked(mouseX, mouseY, button))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            // renderButton(stack, mouseX, mouseY, partialTicks);
            for (int i=0; i<16; i++) {
                buttons[i].render(stack, mouseX, mouseY, partialTicks);
            }
        }
    }

    public void setLink(String option, AbstractButtonWidget element) {
        this.option = option;
        this.element = element;
    }

    public void setCurrentColor(ConfigurationMinecraftColor color) {
        currentColor = color;
    }

    public ConfigurationMinecraftColor getCurrentColor() {
        return currentColor;
    }

    public void onColorSelected(int color) {
        currentColor.colorIndex = color;
        optionScreen.onConfigChanging(option, currentColor);
        element.setMessage(null);
        optionScreen.subscreenFinished();
    }

    private class ColorButton extends AbstractButtonWidget {

        private final ColorSelector parent;
        private final int index;
        private final int color;

        public ColorButton(ColorSelector parent, int x, int y, int width, int height, Text message, int index, int color) {
            super(x, y, width, height, message);
            this.index = index;
            this.color = color;
            this.parent = parent;
        }

        @Override
        protected void renderBg(MatrixStack stack, MinecraftClient mc, int mouseX, int mouseY) {
            if (this.visible) {
                super.renderBg(stack, mc, mouseX, mouseY);

                final Tessellator tessellator = Tessellator.getInstance();
                final BufferBuilder bufferBuilder = tessellator.getBuffer();
                float red = ((color >> 16)/255.0f);
                float green = (((color >> 8) & 0xff)/255.0f);
                float blue = (((color >> 0) & 0xff) /255.0f);

                GlStateManager.disableTexture();

                bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                Matrix4f model = stack.peek().getModel();
                int x1=this.x+3;
                int x2=this.x+this.width-3;
                int y1=this.y+3;
                int y2=this.y+this.height-3;
                if (index == parent.getCurrentColor().colorIndex) {
                    bufferBuilder.vertex(model, x1, y1, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).next();
                    bufferBuilder.vertex(model, x1, y2, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).next();
                    bufferBuilder.vertex(model, x2, y2, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).next();
                    bufferBuilder.vertex(model, x2, y1, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).next();
                    tessellator.draw();
                    bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    x1++; y1++; x2--; y2--;
                }
                bufferBuilder.vertex(model, x1, y1, 0.0f).color(red, green, blue, 1.0f).next();
                bufferBuilder.vertex(model, x1, y2, 0.0f).color(red, green, blue, 1.0f).next();
                bufferBuilder.vertex(model, x2, y2, 0.0f).color(red, green, blue, 1.0f).next();
                bufferBuilder.vertex(model, x2, y1, 0.0f).color(red, green, blue, 1.0f).next();
                tessellator.draw();

                GlStateManager.enableTexture();
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            // System.out.println("selected "+Integer.toHexString(color)+" from button "+this.index);
            parent.onColorSelected(this.index);
        }
    }
}
