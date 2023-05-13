package de.guntram.mcmod.fabrictools.GuiElements;

import de.guntram.mcmod.fabrictools.GuiModOptions;
import de.guntram.mcmod.fabrictools.Types.ConfigurationMinecraftColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;


public class ColorSelector extends ClickableWidget {

    private ColorButton buttons[];
    private ConfigurationMinecraftColor currentColor;
    private String option;
    private ClickableWidget element;
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
        this.optionScreen = optionScreen;
    }

    public void init() {
        Text buttonText = Text.literal("");
        this.setX((optionScreen.width - width) / 2);
        this.setY((optionScreen.height - height) / 2);
        for (int i=0; i<16; i++) {
            buttons[i]=new ColorButton(
                this, getX() + (i/4) * 25, getY() + (i%4)*25, 20, 20, buttonText, i, standardColors[i]
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            // renderButton(stack, mouseX, mouseY, partialTicks);
            for (int i=0; i<16; i++) {
                buttons[i].render(drawContext, mouseX, mouseY, partialTicks);
            }
        }
    }

    public void setLink(String option, ClickableWidget element) {
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

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder narrationMessageBuilder) {
    }

    @Override
    public void renderButton(DrawContext drawContext, int mouseX, int mouseY, float delta) {
    }

    private class ColorButton extends ClickableWidget {

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
        public void render(DrawContext drawContext, int mouseX, int mouseY, float offset) {
            if (this.visible) {
                int x1=this.getX()+3;
                int x2=this.getX()+this.width-3;
                int y1=this.getY()+3;
                int y2=this.getY()+this.height-3;
                if (index == parent.getCurrentColor().colorIndex) {
                    drawContext.fill(x1, y1, x2, y2, 0xffffffff);
                    x1++; y1++; x2--; y2--;
                }
                drawContext.fill(x1, y1, x2, y2, color | 0xff000000);
            }
            super.render(drawContext, mouseX, mouseY, offset);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            // System.out.println("selected "+Integer.toHexString(color)+" from button "+this.index);
            parent.onColorSelected(this.index);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder narrationMessageBuilder) {
        }

        @Override
        public void renderButton(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        }
    }
}
