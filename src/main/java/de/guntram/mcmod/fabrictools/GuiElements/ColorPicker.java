package de.guntram.mcmod.fabrictools.GuiElements;

import de.guntram.mcmod.fabrictools.GuiModOptions;
import de.guntram.mcmod.fabrictools.Types.ConfigurationTrueColor;
import de.guntram.mcmod.fabrictools.Types.SliderValueConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;


public class ColorPicker extends ClickableWidget implements SliderValueConsumer {

    private ColorDisplayAreaButton colorDisplay;
    private GuiSlider redSlider, greenSlider, blueSlider;
    private String option;
    private ClickableWidget element;
    private GuiModOptions optionScreen;
    private int currentColor;

    public ColorPicker(GuiModOptions optionScreen, int initialRGB, Text message) {
        super(0, 0, 250, 100, message);
        this.currentColor = initialRGB;
        this.optionScreen = optionScreen;
    }

    public void init() {
        Text buttonText = Text.literal("");
        this.setX((optionScreen.width - width) / 2);
        this.setY((optionScreen.height - height) / 2);
        colorDisplay = new ColorDisplayAreaButton(
            this, getX(), getY(), 20, 100, buttonText, currentColor
        );
        redSlider = new GuiSlider(this, getX()+50, getY(), 200, 20, (currentColor>>16)&0xff, 0, 255, "red");
        greenSlider = new GuiSlider(this, getX()+50, getY()+40, 200, 20, (currentColor>>16)&0xff, 0, 255, "green");
        blueSlider = new GuiSlider(this, getX()+50, getY()+80, 200, 20, (currentColor>>16)&0xff, 0, 255, "blue");
        visible = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (visible) {
            if (redSlider.mouseClicked(mouseX, mouseY, button)
            ||  greenSlider.mouseClicked(mouseX, mouseY, button)
            ||  blueSlider.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            optionScreen.getTextRenderer().draw(stack, "R", getX()+30, getY()+10, 0xff0000);
            optionScreen.getTextRenderer().draw(stack, "G", getX()+30, getY()+50, 0x00ff00);
            optionScreen.getTextRenderer().draw(stack, "B", getX()+30, getY()+90, 0x0000ff);
            colorDisplay.render(stack, mouseX, mouseY, partialTicks);
            redSlider.render(stack, mouseX, mouseY, alpha);
            greenSlider.render(stack, mouseX, mouseY, alpha);
            blueSlider.render(stack, mouseX, mouseY, alpha);
        }
    }

    public void setLink(String option, ClickableWidget element) {
        this.option = option;
        this.element = element;
    }

    public void setCurrentColor(ConfigurationTrueColor color) {
        currentColor = color.getInt();
        colorDisplay.setColor(currentColor);
        redSlider.reinitialize(color.red);
        greenSlider.reinitialize(color.green);
        blueSlider.reinitialize(color.blue);
    }

    public ConfigurationTrueColor getCurrentColor() {
        return new ConfigurationTrueColor(currentColor);
    }

    public void onDoneButton() {
        optionScreen.onConfigChanging(option, new ConfigurationTrueColor(currentColor));
        element.setMessage(null);
        optionScreen.subscreenFinished();
    }

    @Override
    public void onConfigChanging(String color, Object value) {
        if (color.equals("red")) {
            currentColor = (currentColor & 0x00ffff) | ((int)(Integer)value) << 16;
        }
        else if (color.equals("green")) {
            currentColor = (currentColor & 0xff00ff) | ((int)(Integer)value) << 8;
        }
        else if (color.equals("blue")) {
            currentColor = (currentColor & 0xffff00) | ((int)(Integer)value);
        }
        colorDisplay.setColor(currentColor);
        optionScreen.onConfigChanging(option, new ConfigurationTrueColor(currentColor));
    }

    @Override
    public boolean wasMouseReleased() {
        return optionScreen.wasMouseReleased();
    }
    
    @Override
    public void setMouseReleased(boolean value) {
        optionScreen.setMouseReleased(value);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder narrationMessageBuilder) {
    }
    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    }

    private class ColorDisplayAreaButton extends ClickableWidget {

        private final ColorPicker parent;
        private int rgb;

        public ColorDisplayAreaButton(ColorPicker parent, int x, int y, int width, int height, Text message, int rgb) {
            super(x, y, width, height, message);
            this.rgb = rgb;
            this.parent = parent;
        }
        
        public void setColor(int rgb) {
            this.rgb = rgb;
        }

        @Override
        public void render(MatrixStack stack, int mouseX, int mouseY, float delta) {
            if (this.visible) {
                DrawableHelper.fill(stack, getX(), getY(), getX()+width, getY()+height, rgb | 0xff000000);
            }
            super.render(stack, mouseX, mouseY, delta);
        }

        @Override
        public int getHeight() {
            return super.getHeight();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder narrationMessageBuilder) {
        }
        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        }
    }
}
