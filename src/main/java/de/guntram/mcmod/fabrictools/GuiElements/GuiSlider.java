package de.guntram.mcmod.fabrictools.GuiElements;

import de.guntram.mcmod.fabrictools.IConfiguration;
import de.guntram.mcmod.fabrictools.Types.SliderValueConsumer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class GuiSlider extends SliderWidget {

    private enum Type {INT, FLOAT, DOUBLE}
    Type type;
    double defaultValue, min, max;
    String configOption;
    SliderValueConsumer parent;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiSlider(SliderValueConsumer optionScreen, int x, int y, int width, int height, IConfiguration config, String option) {
        super(x, y, width, height, Text.literal("?"), Double.parseDouble(config.getValue(option).toString()));
        Object currVal=config.getValue(option);
        if (currVal instanceof Double) {
            this.setMessage(Text.literal(Double.toString((Double)currVal)));
            this.min=(Double)config.getMin(option);
            this.max=(Double)config.getMax(option);
            this.defaultValue=(Double)config.getDefault(option);
            this.value=((Double)currVal-min)/(max-min);
            type=Type.DOUBLE;
        }
        else if (currVal instanceof Float) {
            this.setMessage(Text.literal(Float.toString((Float)currVal)));
            this.min=(Float)config.getMin(option);
            this.max=(Float)config.getMax(option);
            this.defaultValue=(Float)config.getDefault(option);
            this.value=((Float)currVal-min)/(max-min);
            type=Type.FLOAT;
        } else {
            this.setMessage(Text.literal(Integer.toString((Integer)currVal)));
            this.min=(Integer)config.getMin(option);
            this.max=(Integer)config.getMax(option);
            this.defaultValue=(Integer)config.getDefault(option);
            this.value=((Integer)currVal-min)/(max-min);
            type=Type.INT;
        }
        this.configOption=option;
        this.parent = optionScreen;
        optionScreen.setMouseReleased(false);
    }
    
    public GuiSlider(SliderValueConsumer optionScreen, int x, int y, int width, int height, int val, int min, int max, String option) {
        super(x, y, width, height, ScreenTexts.EMPTY, val);
        this.setMessage(Text.literal(Integer.toString(val)));
        this.min = min;
        this.max = max;
        this.value=(val-min)/(max-min);
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
        this.value = (value-min)/(max-min);
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
    protected void updateMessage() {
        updateValue(this.value);
    }

    @Override
    protected void applyValue() {
        updateValue(this.value);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.value = (mouseX - (double)(this.getX() + 4)) / (double)(this.width - 8);
        this.value = MathHelper.clamp(this.value, 0.0D, 1.0D);
        updateValue(this.value);
    }
}
