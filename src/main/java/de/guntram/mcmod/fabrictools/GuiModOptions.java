package de.guntram.mcmod.fabrictools;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.guntram.mcmod.fabrictools.ConfigChangedEvent.OnConfigChangingEvent;
import de.guntram.mcmod.fabrictools.Types.ConfigurationMinecraftColor;
import de.guntram.mcmod.fabrictools.Types.ConfigurationTrueColor;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import static net.minecraft.client.gui.widget.AbstractButtonWidget.WIDGETS_LOCATION;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

public class GuiModOptions extends Screen implements Supplier<Screen> {
    
    private final Screen parent;
    private final String modName;
    private final ModConfigurationHandler handler;
    private final List<String> options;
    private final Logger LOGGER;
    
    private String screenTitle;
    
    private static final int LINEHEIGHT = 25;
    private static final int BUTTONHEIGHT = 20;
    private static final int TOP_BAR_SIZE = 40;
    private static final int BOTTOM_BAR_SIZE = 35;

    private boolean mouseReleased = false;          // used in sliders
    private boolean isDraggingScrollbar = false;

    private int buttonWidth;
    private int scrollAmount;
    private int maxScroll;

    private static final Text trueText = new TranslatableText("de.guntram.mcmod.fabrictools.true").formatted(Formatting.GREEN);
    private static final Text falseText = new TranslatableText("de.guntram.mcmod.fabrictools.false").formatted(Formatting.RED);
    
    private ColorSelector colorSelector;
    private ColorPicker colorPicker;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiModOptions(Screen parent, String modName, ModConfigurationHandler confHandler) {
        super(new LiteralText(modName));
        this.parent=parent;
        this.modName=modName;
        this.handler=confHandler;
        this.screenTitle=modName+" Configuration";
        this.options=handler.getConfig().getKeys();
        this.LOGGER=LogManager.getLogger();
        this.colorSelector = new ColorSelector(new LiteralText("Color"));
    }
    
    @Override
    protected void init() {
        buttonWidth = this.width / 2 -50;
        if (buttonWidth > 200) {
            buttonWidth = 200;
        }

        /* This button should always be in first position so it gets clicks
         * before any scrolled-out-of-visibility buttons do.
         */
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
        for (String option: options) {
            y+=LINEHEIGHT;
            Object value = handler.getConfig().getValue(option);
            AbstractButtonWidget element;
            if (value == null) {
                LogManager.getLogger().warn("value null, adding nothing");
                continue;
            } else if (handler.getConfig().isSelectList(option)) {
                String[] options = handler.getConfig().getListOptions(option);
                element = this.addButton(new AbstractButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, new TranslatableText(options[(Integer)value])) {
                    @Override
                    public void onClick(double x, double y) {
                        int cur = (Integer) handler.getConfig().getValue(option);
                        if (++cur == options.length) {
                            cur = 0;
                        }
                        handler.getConfig().setValue(option, (Integer)cur);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, option, cur));
                        this.changeFocus(true);
                    }
                    @Override
                    public void onFocusedChanged(boolean b) {
                        int cur = (Integer) handler.getConfig().getValue(option);
                        this.setMessage(new TranslatableText(options[cur]));
                        super.onFocusedChanged(b);
                    }
                });
            } else if (value instanceof Boolean) {
                element = this.addButton(new AbstractButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, (Boolean) value == true ? trueText : falseText) {
                    @Override
                    public void onClick(double x, double y) {
                        if ((Boolean)(handler.getConfig().getValue(option))==true) {
                            handler.getConfig().setValue(option, false);
                            handler.onConfigChanging(new OnConfigChangingEvent(modName, option, false));
                        } else {
                            handler.getConfig().setValue(option, true);
                            handler.onConfigChanging(new OnConfigChangingEvent(modName, option, true));
                        }
                        this.changeFocus(true);
                    }
                    @Override
                    public void onFocusedChanged(boolean b) {
                        this.setMessage((Boolean) handler.getConfig().getValue(option) == true ? trueText : falseText);
                        super.onFocusedChanged(b);
                    }
                });
            } else if (value instanceof String) {
                element=this.addButton(new TextFieldWidget(this.textRenderer, this.width/2+10, y, buttonWidth, BUTTONHEIGHT, new LiteralText((String) value)) {
                    @Override
                    public void onFocusedChanged(boolean b) {
                        if (b) {
                            LOGGER.info("value to textfield");
                            this.setText((String) handler.getConfig().getValue(option));
                        } else {
                            LOGGER.info("textfield to value");
                            handler.getConfig().setValue(option, this.getText());
                        }
                        super.onFocusedChanged(b);
                    }
                    @Override
                    public boolean charTyped(char chr, int keyCode) {
                        boolean result = super.charTyped(chr, keyCode);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, option, this.getText()));
                        return result;
                    }

                    @Override
                    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, option, this.getText()));
                        return result;
                    }
                    
                });
                ((TextFieldWidget) element).setMaxLength(120);
                element.changeFocus(false);
            } else if (value instanceof ConfigurationMinecraftColor
                   ||  value instanceof Integer && (Integer) handler.getConfig().getMin(option) == 0 && (Integer) handler.getConfig().getMax(option) == 15) {
                // ok, this is quite hacky; assume selections from 0..15 are color indexes ...
                if (value instanceof Integer) {
                    handler.getConfig().setValue(option, new ConfigurationMinecraftColor((Integer)value));
                }
                element=this.addButton(new AbstractButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, null) {
                            @Override
                            public void onClick(double x, double y) {
                                enableColorSelector(option, this);
                            }
                            @Override
                            public void setMessage(Text ignored) {
                                Object o = handler.getConfig().getValue(option);
                                int newIndex = ((ConfigurationMinecraftColor)o).colorIndex;
                                super.setMessage(new TranslatableText("de.guntram.mcmod.fabrictools.color").formatted(Formatting.byColorIndex(newIndex)));
                            }
                });
                element.setMessage(null);
            } else if (value instanceof ConfigurationTrueColor
                    || value instanceof Integer && (Integer) handler.getConfig().getMin(option) == 0 && (Integer) handler.getConfig().getMax(option) == 0xffffff) {
                element=this.addButton(new AbstractButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, null) {
                            @Override
                            public void onClick(double x, double y) {
                                enableColorPicker(option, this);
                            }
                            @Override
                            public void setMessage(Text ignored) {
                                Object o = handler.getConfig().getValue(option);
                                int rgb = ((ConfigurationTrueColor)o).getInt();
                                super.setMessage(new TranslatableText("de.guntram.mcmod.fabrictools.color").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
                            }
                });
                element.setMessage(null);
            } else if (value instanceof Integer || value instanceof Float || value instanceof Double) {
                element=this.addButton(new GuiSlider(this.width/2+10, y, handler.getConfig(), option));
            } else {
                LogManager.getLogger().warn(modName +" has option "+option+" with data type "+value.getClass().getName());
                continue;
            }
            this.addButton(new AbstractButtonWidget(this.width/2+10+buttonWidth+10, y, BUTTONHEIGHT, BUTTONHEIGHT, new LiteralText("")) {
                @Override
                public void onClick(double x, double y) {
                    handler.getConfig().setValue(option, handler.getConfig().getDefault(option));
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, option, handler.getConfig().getDefault(option)));
                    element.changeFocus(false);
                }
            });
        }
        maxScroll = (this.options.size())*LINEHEIGHT - (this.height - TOP_BAR_SIZE - BOTTOM_BAR_SIZE) + LINEHEIGHT;
        if (maxScroll < 0) {
            maxScroll = 0;
        }
        
        colorSelector.init();
        this.addButton(colorSelector);
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

        if (!colorSelector.visible) {
            int y = TOP_BAR_SIZE + LINEHEIGHT/2 - scrollAmount;
            for (int i=0; i<this.options.size(); i++) {
                if (y > TOP_BAR_SIZE - LINEHEIGHT/2 && y < height - BOTTOM_BAR_SIZE) {
                    textRenderer.draw(stack, new TranslatableText(options.get(i)).asOrderedText(), this.width / 2 -155, y+4, 0xffffff);
                    ((AbstractButtonWidget)this.buttons.get(i*2+1)).y = y;                                          // config elem
                    ((AbstractButtonWidget)this.buttons.get(i*2+1)).render(stack, mouseX, mouseY, partialTicks);
                    ((AbstractButtonWidget)this.buttons.get(i*2+2)).y = y;                                        // reset button
                    ((AbstractButtonWidget)this.buttons.get(i*2+2)).render(stack, mouseX, mouseY, partialTicks);
                }
                y += LINEHEIGHT;
            }

            y = TOP_BAR_SIZE + LINEHEIGHT/2 - scrollAmount;
            for (String text: options) {
                if (y > TOP_BAR_SIZE - LINEHEIGHT/2 && y < height - BOTTOM_BAR_SIZE) {
                    if (mouseX>this.width/2-155 && mouseX<this.width/2 && mouseY>y && mouseY<y+BUTTONHEIGHT) {
                        TranslatableText tooltip=new TranslatableText(handler.getConfig().getTooltip(text));
                        if (textRenderer.getWidth(tooltip)<=250) {
                            renderTooltip(stack, tooltip, 0, mouseY);
                        } else {
                            List<OrderedText> lines = textRenderer.wrapLines(tooltip, 250);
                            renderOrderedTooltip(stack, lines, 0, mouseY);
                        }
                    }
                }
                y+=LINEHEIGHT;
            }

            if (maxScroll > 0) {
                // fill(stack, width-5, TOP_BAR_SIZE, width, height - BOTTOM_BAR_SIZE, 0xc0c0c0);
                int pos = (int)((height - TOP_BAR_SIZE - BOTTOM_BAR_SIZE - BUTTONHEIGHT) * ((float)scrollAmount / maxScroll));
                // fill(stack, width-5, pos, width, pos+BUTTONHEIGHT, 0x303030);
                this.client.getTextureManager().bindTexture(WIDGETS_LOCATION);
                drawTexture(stack, width-5, pos+TOP_BAR_SIZE, 0, 66, 4, 20);
            }
        } else {
            colorSelector.render(stack, mouseX, mouseY, partialTicks);
        }
        this.client.getTextureManager().bindTexture(DrawableHelper.BACKGROUND_TEXTURE);
        RenderSystem.disableDepthTest();
        drawTexture(stack, 0, 0, 0, 0, width, TOP_BAR_SIZE);
        drawTexture(stack, 0, height-BOTTOM_BAR_SIZE, 0, 0, width, BOTTOM_BAR_SIZE);

        drawCenteredString(stack, textRenderer, screenTitle, this.width/2, (TOP_BAR_SIZE - textRenderer.fontHeight)/2, 0xffffff);
        ((AbstractButtonWidget)this.buttons.get(0)).render(stack, mouseX, mouseY, partialTicks);
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
    
    private void enableColorSelector(String option, AbstractButtonWidget element) {
        for (int i=1; i<buttons.size(); i++) {
            buttons.get(i).visible = false;
        }
        colorSelector.setCurrentColor((ConfigurationMinecraftColor) handler.getConfig().getValue(option));
        colorSelector.visible = true;
        colorSelector.setLink(option, element);
    }

    private void enableColorPicker(String option, AbstractButtonWidget element) {
        for (int i=1; i<buttons.size(); i++) {
            buttons.get(i).visible = false;
        }
        colorPicker.setCurrentColor((ConfigurationMinecraftColor) handler.getConfig().getValue(option));
        colorPicker.visible = true;
        colorPicker.setLink(option, element);
    }
    
    private void disableColorSelectorAndPicker() {
        for (int i=1; i<buttons.size(); i++) {
            buttons.get(i).visible = true;
        }
        colorSelector.visible = false;
        colorPicker.visible = false;
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
    
    private class ColorSelector extends AbstractButtonWidget {
        
        private ColorButton buttons[];
        private ConfigurationMinecraftColor currentColor;
        private String option;
        private AbstractButtonWidget element;

        private int standardColors[] = { 
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };

        public ColorSelector(Text message) {
            super(0, 0, 120, 120, message);
            buttons = new ColorButton[16];
        }
        
        public void init() {
            Text buttonText = new LiteralText("");
            this.x = (GuiModOptions.this.width - width) / 2;
            this.y = (GuiModOptions.this.height - height) / 2;
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
            handler.onConfigChanging(new OnConfigChangingEvent(modName, option, currentColor));
            System.out.println("set "+option+" to "+color);
            handler.getConfig().setValue(option, currentColor);
            element.setMessage(null);
            disableColorSelectorAndPicker();
        }
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
                super.renderBg(stack, client, mouseX, mouseY);

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
    
    private class ColorPicker extends AbstractButtonWidget {
        
        public ColorPicker(int x, int y, int width, int height, Text message) {
            super(x, y, width, height, message);
        }
        
    }
}
