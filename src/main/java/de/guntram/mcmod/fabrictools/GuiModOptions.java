package de.guntram.mcmod.fabrictools;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.guntram.mcmod.fabrictools.ConfigChangedEvent.OnConfigChangingEvent;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import static net.minecraft.client.gui.widget.AbstractButtonWidget.WIDGETS_LOCATION;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringRenderable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuiModOptions extends Screen implements Supplier<Screen> {
    
    private final Screen parent;
    private final String modName;
    private final ModConfigurationHandler handler;
    private final List<String> options;
    private final Logger LOGGER;
    
    private String screenTitle;
    
    private static final int LINEHEIGHT = 25;
    private static final int BUTTONHEIGHT = 20;
    
    //private static final Text trueText = new TranslatableText("de.guntram.mcmod.fabrictools.true").copy().formatted(Formatting.GREEN);
    // private static final Text falseText = new TranslatableText("de.guntram.mcmod.fabrictools.false").copy().formatted(Formatting.RED);

    private static final int TOP_BAR_SIZE = 40;
    private static final int BOTTOM_BAR_SIZE = 35;

    private boolean mouseReleased = false;          // used in sliders
    private boolean isDraggingScrollbar = false;

    private int buttonWidth;
    private int scrollAmount;
    private int maxScroll;

    private static final Text trueText = new TranslatableText("de.guntram.mcmod.fabrictools.true").formatted(Formatting.GREEN);
    private static final Text falseText = new TranslatableText("de.guntram.mcmod.fabrictools.false").formatted(Formatting.RED);
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiModOptions(Screen parent, String modName, ModConfigurationHandler confHandler) {
        super(new LiteralText(modName));
        this.parent=parent;
        this.modName=modName;
        this.handler=confHandler;
        this.screenTitle=modName+" Configuration";
        this.options=handler.getConfig().getKeys();
        this.LOGGER=LogManager.getLogger();
    }
    
    @Override
    protected void init() {
        buttonWidth = this.width / 2 -50;
        if (buttonWidth > 200) {
            buttonWidth = 200;
        }
        this.addButton(new AbstractButtonWidget(this.width / 2 - 100, this.height - 27, 200, BUTTONHEIGHT, new TranslatableText("gui.done")) {
            @Override
            public void onClick(double x, double y) {
                for (AbstractButtonWidget button: buttons) {
                    if (button instanceof TextFieldWidget) {
                        if (button.isFocused()) {
                            button.changeFocus(false);
                        }
                    }
                }
                handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(modName));
                client.openScreen(parent);
            }
        });
        
        int y=50-LINEHEIGHT;
        for (String text: options) {
            y+=LINEHEIGHT;
            Object value = handler.getConfig().getValue(text);
            AbstractButtonWidget element;
            if (value == null) {
                LogManager.getLogger().warn("value null, adding nothing");
                continue;
            } else if (handler.getConfig().isSelectList(text)) {
                String[] options = handler.getConfig().getListOptions(text);
                element = this.addButton(new AbstractButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, new TranslatableText(options[(Integer)value])) {
                    @Override
                    public void onClick(double x, double y) {
                        int cur = (Integer) handler.getConfig().getValue(text);
                        if (++cur == options.length) {
                            cur = 0;
                        }
                        handler.getConfig().setValue(text, (Integer)cur);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, text, cur));
                        this.changeFocus(true);
                    }
                    @Override
                    public void onFocusedChanged(boolean b) {
                        int cur = (Integer) handler.getConfig().getValue(text);
                        this.setMessage(new TranslatableText(options[cur]));
                        super.onFocusedChanged(b);
                    }
                });
            } else if (value instanceof Boolean) {
                element = this.addButton(new AbstractButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, (Boolean) value == true ? trueText : falseText) {
                    @Override
                    public void onClick(double x, double y) {
                        if ((Boolean)(handler.getConfig().getValue(text))==true) {
                            handler.getConfig().setValue(text, false);
                            handler.onConfigChanging(new OnConfigChangingEvent(modName, text, false));
                        } else {
                            handler.getConfig().setValue(text, true);
                            handler.onConfigChanging(new OnConfigChangingEvent(modName, text, true));
                        }
                        this.changeFocus(true);
                    }
                    @Override
                    public void onFocusedChanged(boolean b) {
                        this.setMessage((Boolean) handler.getConfig().getValue(text) == true ? trueText : falseText);
                        super.onFocusedChanged(b);
                    }
                });
            } else if (value instanceof String) {
                element=this.addButton(new TextFieldWidget(this.textRenderer, this.width/2+10, y, buttonWidth, BUTTONHEIGHT, new LiteralText((String) value)) {
                    @Override
                    public void onFocusedChanged(boolean b) {
                        if (b) {
                            LOGGER.info("value to textfield");
                            this.setText((String) handler.getConfig().getValue(text));
                        } else {
                            LOGGER.info("textfield to value");
                            handler.getConfig().setValue(text, this.getText());
                        }
                        super.onFocusedChanged(b);
                    }
                    @Override
                    public boolean charTyped(char chr, int keyCode) {
                        boolean result = super.charTyped(chr, keyCode);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, text, this.getText()));
                        return result;
                    }

                    @Override
                    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, text, this.getText()));
                        return result;
                    }
                    
                });
                ((TextFieldWidget) element).setMaxLength(120);
                element.changeFocus(false);
            } else if (value instanceof Integer || value instanceof Float || value instanceof Double) {
                element=this.addButton(new GuiSlider(this.width/2+10, y, handler.getConfig(), text));
            } else {
                LogManager.getLogger().warn(modName +" has option "+text+" with data type "+value.getClass().getName());
                continue;
            }
            this.addButton(new AbstractButtonWidget(this.width/2+10+buttonWidth+10, y, BUTTONHEIGHT, BUTTONHEIGHT, new LiteralText("")) {
                @Override
                public void onClick(double x, double y) {
                    handler.getConfig().setValue(text, handler.getConfig().getDefault(text));
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, text, handler.getConfig().getDefault(text)));
                    element.changeFocus(false);
                }
            });
        }
        maxScroll = (this.options.size())*LINEHEIGHT - (this.height - TOP_BAR_SIZE - BOTTOM_BAR_SIZE) + LINEHEIGHT;
        if (maxScroll < 0) {
            maxScroll = 0;
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.scrollAmount = (this.scrollAmount - (int)(amount * BUTTONHEIGHT / 2));
        if (scrollAmount < 0) {
            scrollAmount = 0;
        }
        if (scrollAmount > maxScroll) {
            scrollAmount = maxScroll;
        }
        return true;
    }
    
    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(stack);
/*
        drawCenteredString(stack, textRenderer, screenTitle, this.width/2, BUTTONHEIGHT, 0xffffff);
        super.render(stack, mouseX, mouseY, partialTicks);
        
        int y=50;
        for (String text: options) {
            textRenderer.draw(stack, new TranslatableText(text), this.width / 2 -155, y+2, 0xffffff);
            y+=LINEHEIGHT;
        }
*/


        int y = TOP_BAR_SIZE + LINEHEIGHT/2 - scrollAmount;
        for (int i=1; i<this.buttons.size(); i+=2) {
            if (y > TOP_BAR_SIZE - LINEHEIGHT/2 && y < height - BOTTOM_BAR_SIZE) {
                textRenderer.draw(stack, new TranslatableText(options.get(i/2)), this.width / 2 -155, y+4, 0xffffff);
                ((AbstractButtonWidget)this.buttons.get(i)).y = y;                                          // config elem
                ((AbstractButtonWidget)this.buttons.get(i)).render(stack, mouseX, mouseY, partialTicks);
                ((AbstractButtonWidget)this.buttons.get(i+1)).y = y;                                        // reset button
                ((AbstractButtonWidget)this.buttons.get(i+1)).render(stack, mouseX, mouseY, partialTicks);
            }
            y += LINEHEIGHT;
        }
        
        y = TOP_BAR_SIZE - scrollAmount;
        for (String text: options) {
            if (mouseX>this.width/2-155 && mouseX<this.width/2 && mouseY>y && mouseY<y+BUTTONHEIGHT) {
                StringRenderable tooltip=new TranslatableText(handler.getConfig().getTooltip(text));
                if (textRenderer.getWidth(tooltip)<=250) {
                    renderTooltip(stack, tooltip, 0, mouseY);
                } else {
                    List<StringRenderable> lines = textRenderer.wrapLines(tooltip, 250);
                    renderTooltip(stack, lines, mouseX, mouseY);
                }
            }
            y+=LINEHEIGHT;
        }
        
        this.client.getTextureManager().bindTexture(DrawableHelper.BACKGROUND_TEXTURE);
        RenderSystem.disableDepthTest();
        drawTexture(stack, 0, 0, 0, 0, width, TOP_BAR_SIZE);
        drawTexture(stack, 0, height-BOTTOM_BAR_SIZE, 0, 0, width, BOTTOM_BAR_SIZE);

        drawCenteredString(stack, textRenderer, screenTitle, this.width/2, (TOP_BAR_SIZE - textRenderer.fontHeight)/2, 0xffffff);
        ((AbstractButtonWidget)this.buttons.get(0)).render(stack, mouseX, mouseY, partialTicks);
        
        if (maxScroll > 0) {
            // fill(stack, width-5, TOP_BAR_SIZE, width, height - BOTTOM_BAR_SIZE, 0xc0c0c0);
            int pos = (int)((height - TOP_BAR_SIZE - BOTTOM_BAR_SIZE - BUTTONHEIGHT) * ((float)scrollAmount / maxScroll));
            // fill(stack, width-5, pos, width, pos+BUTTONHEIGHT, 0x303030);
            this.client.getTextureManager().bindTexture(WIDGETS_LOCATION);
            drawTexture(stack, width-5, pos+TOP_BAR_SIZE, 0, 66, 4, 20);
        }
    }

    @Override
    public Screen get() {
        return this;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        if (button == 0) {
            mouseReleased = true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll > 0 && mouseX > width-5) {
            isDraggingScrollbar = true;
            scrollAmount = (int)((mouseY - TOP_BAR_SIZE)/(height - BOTTOM_BAR_SIZE - TOP_BAR_SIZE)*maxScroll);
            scrollAmount = MathHelper.clamp(scrollAmount, 0, maxScroll);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            scrollAmount = (int)((mouseY - TOP_BAR_SIZE)/(height - BOTTOM_BAR_SIZE - TOP_BAR_SIZE)*maxScroll);
            scrollAmount = MathHelper.clamp(scrollAmount, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    private enum Type {INT, FLOAT, DOUBLE;}
    
    private class GuiSlider extends AbstractButtonWidget {
        Type type;
        boolean dragging;
        double sliderValue, min, max;
        Configuration config;
        String configOption;
        
        @SuppressWarnings("OverridableMethodCallInConstructor")
        GuiSlider(int x, int y, Configuration config, String option) {
            super(x, y, buttonWidth, BUTTONHEIGHT, new LiteralText("?"));
            Object value=config.getValue(option);
            if (value instanceof Double) {
                this.setMessage(new LiteralText(Double.toString((Double)value)));
                this.min=(Double)config.getMin(option);
                this.max=(Double)config.getMax(option);
                sliderValue=((Double)value-min)/(max-min);
                type=Type.DOUBLE;
            }
            else if (value instanceof Float) {
                this.setMessage(new LiteralText(Float.toString((Float)value)));
                this.min=(Float)config.getMin(option);
                this.max=(Float)config.getMax(option);
                sliderValue=((Float)value-min)/(max-min);
                type=Type.FLOAT;
            } else {
                this.setMessage(new LiteralText(Integer.toString((Integer)value)));
                this.min=(Integer)config.getMin(option);
                this.max=(Integer)config.getMax(option);
                sliderValue=((Integer)value-min)/(max-min);
                type=Type.INT;
            }

            this.config=config;
            this.configOption=option;
        }
        
        private void updateValue(double value) {
            switch (type) {
                case DOUBLE:
                    double doubleVal=value*(max-min)+min;
                    this.setMessage(new LiteralText(Double.toString(doubleVal)));
                    this.config.setValue(configOption, (Double) doubleVal);
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, configOption, doubleVal));
                    break;
                case FLOAT:
                    float floatVal=(float) (value*(max-min)+min);
                    this.setMessage(new LiteralText(Float.toString(floatVal)));
                    this.config.setValue(configOption, (Float) floatVal);
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, configOption, floatVal));
                    break;
                case INT:
                    int intVal=(int) (value*(max-min)+min);
                    this.setMessage(new LiteralText(Integer.toString(intVal)));
                    this.config.setValue(configOption, (Integer) intVal);
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, configOption, intVal));
                    break;
            }
        }

        @Override
        protected void renderBg(MatrixStack stack, MinecraftClient mc, int mouseX, int mouseY)
        {
            if (this.visible)
            {
                if (this.dragging)
                {
                    this.sliderValue = (double)((float)(mouseX - (this.x + 4)) / (float)(this.width - 8));
                    this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
                    updateValue(this.sliderValue);
                    if (mouseReleased) {
                        this.dragging = false;
                        mouseReleased = false;
                    }
                            
                }
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.drawTexture(stack, this.x + (int)(this.sliderValue * (double)(this.width - 8)), this.y, 0, 66, 4, 20);
                this.drawTexture(stack, this.x + (int)(this.sliderValue * (double)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
            }
        }

        /**
         * Called when the left mouse button is pressed over this button. This method is specific to AbstractButtonWidget.
         */
        @Override
        public final void onClick(double mouseX, double mouseY)
        {
            this.sliderValue = (mouseX - (double)(this.x + 4)) / (double)(this.width - 8);
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
            updateValue(sliderValue);
            this.dragging = true;
            mouseReleased = false;
        }

        /**
         * Called when the left mouse button is released. This method is specific to AbstractButtonWidget.
         */
        @Override
        public void onRelease(double mouseX, double mouseY)
        {
            this.dragging = false;
        }
        
        @Override
        public void onFocusedChanged(boolean b) {
            Object value=config.getValue(configOption);
            if (value instanceof Double) {
                this.setMessage(new LiteralText(Double.toString((Double)value)));
                sliderValue=((Double)value-min)/(max-min);
            }
            else if (value instanceof Float) {
                this.setMessage(new LiteralText(Float.toString((Float)value)));
                sliderValue=((Float)value-min)/(max-min);
            } else {
                this.setMessage(new LiteralText(Integer.toString((Integer)value)));
                sliderValue=((Integer)value-min)/(max-min);
            }
            super.onFocusedChanged(b);
        }
    }
}
