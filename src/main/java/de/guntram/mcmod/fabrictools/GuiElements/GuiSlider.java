package de.guntram.mcmod.fabrictools.GuiElements;

import com.mojang.blaze3d.systems.RenderSystem;
import de.guntram.mcmod.fabrictools.IConfiguration;
import de.guntram.mcmod.fabrictools.Types.SliderValueConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import static net.minecraft.client.gui.widget.ClickableWidget.WIDGETS_TEXTURE;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class GuiSlider extends ClickableWidget {

    private enum Type {INT, FLOAT, DOUBLE;}
    
    Type type;
    boolean dragging;
    double sliderValue, defaultValue, min, max;
    String configOption;
    SliderValueConsumer parent;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiSlider(SliderValueConsumer optionScreen, int x, int y, int width, int height, IConfiguration config, String option) {
        super(x, y, width, height, Text.literal("?"));
        Object value=config.getValue(option);
        if (value instanceof Double) {
            this.setMessage(Text.literal(Double.toString((Double)value)));
            this.min=(Double)config.getMin(option);
            this.max=(Double)config.getMax(option);
            this.defaultValue=(Double)config.getDefault(option);
            sliderValue=((Double)value-min)/(max-min);
            type=Type.DOUBLE;
        }
        else if (value instanceof Float) {
            this.setMessage(Text.literal(Float.toString((Float)value)));
            this.min=(Float)config.getMin(option);
            this.max=(Float)config.getMax(option);
            this.defaultValue=(Float)config.getDefault(option);
            sliderValue=((Float)value-min)/(max-min);
            type=Type.FLOAT;
        } else {
            this.setMessage(Text.literal(Integer.toString((Integer)value)));
            this.min=(Integer)config.getMin(option);
            this.max=(Integer)config.getMax(option);
            this.defaultValue=(Integer)config.getDefault(option);
            sliderValue=((Integer)value-min)/(max-min);
            type=Type.INT;
        }

        this.configOption=option;
        this.parent = optionScreen;
        optionScreen.setMouseReleased(false);
    }
    
    public GuiSlider(SliderValueConsumer optionScreen, int x, int y, int width, int height, int val, int min, int max, String option) {
        super(x, y, width, height, ScreenTexts.EMPTY);
        this.setMessage(Text.literal(""+val));
        this.min = min;
        this.max = max;
        this.sliderValue=(val-min)/(max-min);
        this.type = Type.INT;
        this.configOption = option;
        this.parent = optionScreen;
    }

    /**
     * Callable from the outside (parent), to set a new value. Updates the
     * slider position and text, but does not emit "updated" events.
     * @param value the new value, in terms between min and max.
     */
    public void reinitialize(double value) {
        this.sliderValue = (value-min)/(max-min);
        switch (type) {
            case DOUBLE:
                this.setMessage(Text.literal(Double.toString(value)));
                break;
            case FLOAT:
                this.setMessage(Text.literal(Float.toString((float)value)));
                break;
            case INT:
                this.setMessage(Text.literal(Integer.toString((int)value)));
                break;
        }
    }
    
    /**
     * Called when the user clicks, drags, or otherwise moves the slider.
     * Resets the text message to reflect the new value, and tells our
     * parent the config has changed.
     * 
     * @param value The new slider value. As the slider always uses a
     * range between 0 and 1 internally, this value is expected to 
     * be in that range too.
     * 
     */
    private void updateValue(double value) {
        switch (type) {
            case DOUBLE:
                double doubleVal=value*(max-min)+min;
                this.setMessage(Text.literal(String.format("%.2f", doubleVal)));
                parent.onConfigChanging(configOption, doubleVal);
                break;
            case FLOAT:
                float floatVal=(float) (value*(max-min)+min);
                this.setMessage(Text.literal(String.format("%.2f", floatVal)));
                parent.onConfigChanging(configOption, floatVal);
                break;
            case INT:
                int intVal=(int) (value*(max-min)+min+0.5);
                this.setMessage(Text.literal(String.format("%d", intVal)));
                parent.onConfigChanging(configOption, intVal);
                break;
        }
    }

    @Override
    protected void renderBackground(MatrixStack stack, MinecraftClient mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            if (this.dragging)
            {
                this.sliderValue = (double)((float)(mouseX - (this.x + 4)) / (float)(this.width - 8));
                this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
                updateValue(this.sliderValue);
                if (parent.wasMouseReleased()) {
                    this.dragging = false;
                }
            }
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexture(stack, this.x + (int)(this.sliderValue * (double)(this.width - 8)), this.y, 0, 66, 4, 20);
            this.drawTexture(stack, this.x + (int)(this.sliderValue * (double)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
        }
    }

    /*
     * Called when the left mouse button is pressed over this button. This method is specific to ClickableWidget.
     */
    @Override
    public final void onClick(double mouseX, double mouseY)
    {
        this.sliderValue = (mouseX - (double)(this.x + 4)) / (double)(this.width - 8);
        this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
        updateValue(sliderValue);
        this.dragging = true;
        parent.setMouseReleased(false);
    }

    /*
     * Called when the left mouse button is released. This method is specific to ClickableWidget.
     */
    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.dragging = false;
    }
    
    @Override
    public void onFocusedChanged(boolean b) {
        // called when the user presses the "reset" button next to the slider
        sliderValue=(defaultValue-min)/(max-min);
        updateValue(sliderValue);
        super.onFocusedChanged(b);
    }
    
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }    
}
