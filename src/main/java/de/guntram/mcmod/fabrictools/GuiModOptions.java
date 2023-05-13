package de.guntram.mcmod.fabrictools;

import com.mojang.blaze3d.systems.RenderSystem;
import de.guntram.mcmod.fabrictools.ConfigChangedEvent.OnConfigChangingEvent;
import de.guntram.mcmod.fabrictools.GuiElements.ColorPicker;
import de.guntram.mcmod.fabrictools.GuiElements.ColorSelector;
import de.guntram.mcmod.fabrictools.GuiElements.GuiSlider;
import de.guntram.mcmod.fabrictools.GuiElements.GBButtonWidget;
import de.guntram.mcmod.fabrictools.Types.ConfigurationMinecraftColor;
import de.guntram.mcmod.fabrictools.Types.ConfigurationTrueColor;
import de.guntram.mcmod.fabrictools.Types.SliderValueConsumer;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import static net.minecraft.client.gui.widget.ClickableWidget.WIDGETS_TEXTURE;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuiModOptions extends Screen implements Supplier<Screen>, SliderValueConsumer {
    
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

    private boolean isDraggingScrollbar = false;
    private boolean mouseReleased = false;      // used to capture mouse release events when a child slider has the mouse dragging

    private int buttonWidth;
    private int scrollAmount;
    private int maxScroll;

    private static final Text trueText = Text.translatable("de.guntram.mcmod.fabrictools.true").formatted(Formatting.GREEN);
    private static final Text falseText = Text.translatable("de.guntram.mcmod.fabrictools.false").formatted(Formatting.RED);
    
    private ColorSelector colorSelector;
    private ColorPicker colorPicker;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiModOptions(Screen parent, String modName, ModConfigurationHandler confHandler) {
        super(Text.literal(modName));
        this.parent=parent;
        this.modName=modName;
        this.handler=confHandler;
        this.screenTitle=modName+" Configuration";
        this.options=handler.getIConfig().getKeys();
        this.LOGGER=LogManager.getLogger();
        this.colorSelector = new ColorSelector(this, Text.literal("Minecraft Color"));
        this.colorPicker = new ColorPicker(this, 0xffffff, Text.literal("RGB Color"));
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
        this.addDrawableChild(new GBButtonWidget(this.width / 2 - 100, this.height - 27, 200, BUTTONHEIGHT, Text.translatable("gui.done")) {
            @Override
            public void onClick(double x, double y) {

                if (colorSelector.visible) {
                    subscreenFinished();
                    return;
                } else if (colorPicker.visible) {
                    colorPicker.onDoneButton();
                    return;
                }

                handler.getConfig().save();
                handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(modName));
                client.setScreen(parent);
            }
        });

        int y=50-LINEHEIGHT;
        for (String option: options) {
            y+=LINEHEIGHT;
            Object value = handler.getIConfig().getValue(option);
            ClickableWidget element;
            if (value == null) {
                LogManager.getLogger().warn("value null, adding nothing");
                continue;
            } else if (handler.getIConfig().isSelectList(option)) {
                String[] options = handler.getIConfig().getListOptions(option);
                element = this.addDrawableChild(new GBButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, Text.translatable(options[(Integer)value])) {
                    @Override
                    public void onClick(double x, double y) {
                        int cur = (Integer) handler.getIConfig().getValue(option);
                        if (++cur == options.length) {
                            cur = 0;
                        }
                        onConfigChanging(option, cur);
                    }
                    @Override
                    public void setFocused(boolean focused) {
                        int cur = (Integer) handler.getIConfig().getValue(option);
                        this.setMessage(Text.translatable(options[cur]));
                        super.setFocused(focused);
                    }
                });
            } else if (value instanceof Boolean) {
                element = this.addDrawableChild(new GBButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, (Boolean) value ? trueText : falseText) {
                    @Override
                    public void onClick(double x, double y) {
                        if ((Boolean)(handler.getIConfig().getValue(option))) {
                            onConfigChanging(option, false);
                        } else {
                            onConfigChanging(option, true);
                        }
                    }
                    @Override
                    public void setFocused(boolean focused) {
                        this.setMessage((Boolean) handler.getIConfig().getValue(option) ? trueText : falseText);
                        super.setFocused(focused);
                    }
                });
            } else if (value instanceof String) {
                element=this.addDrawableChild(new TextFieldWidget(this.textRenderer, this.width/2+10, y, buttonWidth, BUTTONHEIGHT, Text.literal("")) {
                    @Override
                    public boolean charTyped(char chr, int modifiers) {
                        boolean retVal =  super.charTyped(chr, modifiers);
                        String currVal = this.getText().toString();
                        onConfigChanging(option, currVal);
                        return retVal;
                    }

                    @Override
                    public void setFocused(boolean focused) {
                        String currVal = (String) handler.getIConfig().getValue(option);
                        this.setText(currVal);
                        super.setFocused(focused);
                    }
                });
                ((TextFieldWidget) element).setMaxLength(120);
                ((TextFieldWidget) element).setText((String) value);

            } else if (value instanceof ConfigurationMinecraftColor
                    ||  value instanceof Integer && (Integer) handler.getIConfig().getMin(option) == 0 && (Integer) handler.getIConfig().getMax(option) == 15) {
                // upgrade int 0..15 from older mods to color
                if (value instanceof Integer) {
                    handler.getIConfig().setValue(option, new ConfigurationMinecraftColor((Integer)value));
                }
                element=this.addDrawableChild(new GBButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, ScreenTexts.EMPTY) {
                    @Override
                    public void onClick(double x, double y) {
                        enableColorSelector(option, this);
                    }
                    @Override
                    public void setMessage(Text ignored) {
                        Object o = handler.getIConfig().getValue(option);
                        int newIndex = ((ConfigurationMinecraftColor)o).colorIndex;
                        super.setMessage(Text.translatable("de.guntram.mcmod.fabrictools.color").formatted(Formatting.byColorIndex(newIndex)));
                    }
                    @Override
                    public void setFocused(boolean focused) {
                        setMessage(null);
                        super.setFocused(focused);
                    }
                });
                element.setMessage(ScreenTexts.EMPTY);
            } else if (value instanceof ConfigurationTrueColor
                    || value instanceof Integer && (Integer) handler.getIConfig().getMin(option) == 0 && (Integer) handler.getIConfig().getMax(option) == 0xffffff) {
                if (value instanceof Integer) {
                    handler.getIConfig().setValue(option, new ConfigurationTrueColor((Integer)value));
                }
                element=this.addDrawableChild(new GBButtonWidget(this.width/2+10, y, buttonWidth, BUTTONHEIGHT, ScreenTexts.EMPTY) {
                    @Override
                    public void onClick(double x, double y) {
                        enableColorPicker(option, this);
                    }
                    @Override
                    public void setMessage(Text ignored) {
                        Object o = handler.getIConfig().getValue(option);
                        int rgb = ((ConfigurationTrueColor)o).getInt();
                        super.setMessage(Text.translatable("de.guntram.mcmod.fabrictools.color").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
                    }

                    @Override
                    public void setFocused(boolean focused) {
                        setMessage(null);
                        super.setFocused(focused);
                    }
                });
                element.setMessage(ScreenTexts.EMPTY);
            } else if (value instanceof Integer || value instanceof Float || value instanceof Double) {
                element=this.addDrawableChild(new GuiSlider(this, this.width/2+10, y, this.buttonWidth, BUTTONHEIGHT, handler.getIConfig(), option) {
                    @Override
                    public void setFocused(boolean focused) {
                        double currVal = Double.parseDouble(handler.getIConfig().getValue(option).toString());
                        reinitialize(currVal);
                        super.setFocused(focused);
                    }
                });
            } else {
                LogManager.getLogger().warn(modName +" has option "+option+" with data type "+value.getClass().getName());
                continue;
            }
            // Add button to set to default
            this.addDrawableChild(new GBButtonWidget(this.width/2+10+buttonWidth+10, y, BUTTONHEIGHT, BUTTONHEIGHT, ScreenTexts.EMPTY) {
                @Override
                public void onClick(double x, double y) {
                    Object value = handler.getIConfig().getValue(option);
                    Object defValue = handler.getIConfig().getDefault(option);
                    if (value instanceof ConfigurationMinecraftColor) {
                        defValue = new ConfigurationMinecraftColor((int) defValue);
                    } else if (value instanceof ConfigurationTrueColor) {
                        defValue = new ConfigurationTrueColor((int) defValue);
                    }
                    onConfigChanging(option, defValue);
                    this.setFocused(false);
                    element.setFocused(true);
                }
            });
        }
        maxScroll = (this.options.size())*LINEHEIGHT - (this.height - TOP_BAR_SIZE - BOTTOM_BAR_SIZE) + LINEHEIGHT;
        if (maxScroll < 0) {
            maxScroll = 0;
        }
        colorSelector.init();
        colorPicker.init();
        this.addDrawableChild(colorSelector);
        this.addDrawableChild(colorPicker);
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        renderBackground(drawContext);
        MatrixStack stack = drawContext.getMatrices();
        if (colorSelector.visible) {
            colorSelector.render(drawContext, mouseX, mouseY, partialTicks);
        } else if (colorPicker.visible) {
            colorPicker.render(drawContext, mouseX, mouseY, partialTicks);
        } else {
            int y = TOP_BAR_SIZE + LINEHEIGHT/2 - scrollAmount;
            for (int i=0; i<this.options.size(); i++) {
                if (y > TOP_BAR_SIZE - LINEHEIGHT/2 && y < height - BOTTOM_BAR_SIZE) {
                    drawContext.drawText(textRenderer, Text.translatable(options.get(i)).asOrderedText(), this.width / 2 -155, y+4, 0xffffff, false);
                    ((ClickableWidget)this.children().get(i*2+1)).setY(y);                                          // config elem
                    ((ClickableWidget)this.children().get(i*2+1)).render(drawContext, mouseX, mouseY, partialTicks);
                    ((ClickableWidget)this.children().get(i*2+2)).setY(y);                                        // reset button
                    ((ClickableWidget)this.children().get(i*2+2)).render(drawContext, mouseX, mouseY, partialTicks);
                }
                y += LINEHEIGHT;
            }

            y = TOP_BAR_SIZE + LINEHEIGHT/2 - scrollAmount;
            for (String text: options) {
                if (y > TOP_BAR_SIZE - LINEHEIGHT/2 && y < height - BOTTOM_BAR_SIZE) {
                    if (mouseX>this.width/2-155 && mouseX<this.width/2 && mouseY>y && mouseY<y+BUTTONHEIGHT) {
                        String ttText = handler.getIConfig().getTooltip(text);
                        if (ttText == null || ttText.isEmpty()) {
                            y += LINEHEIGHT;
                            continue;
                        }
                        MutableText tooltip=Text.translatable(handler.getIConfig().getTooltip(text));
                        int width = textRenderer.getWidth(tooltip);
                        if (width == 0) {
                            // do nothing
                        } else if (width<=250) {
                            drawContext.drawTooltip(textRenderer, tooltip, 0, mouseY);
                        } else {
                            List<OrderedText> lines = textRenderer.wrapLines(tooltip, 250);
                            drawContext.drawOrderedTooltip(textRenderer, lines, 0, mouseY);
                        }
                    }
                }
                y+=LINEHEIGHT;
            }

            if (maxScroll > 0) {
                // fill(stack, width-5, TOP_BAR_SIZE, width, height - BOTTOM_BAR_SIZE, 0xc0c0c0);
                int pos = (int)((height - TOP_BAR_SIZE - BOTTOM_BAR_SIZE - BUTTONHEIGHT) * ((float)scrollAmount / maxScroll));
                // fill(stack, width-5, pos, width, pos+BUTTONHEIGHT, 0x303030);
                RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
                drawContext.drawTexture(WIDGETS_TEXTURE,width-5, pos+TOP_BAR_SIZE, 0, 66, 4, 20);
            }
        }
        RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.disableDepthTest();
        drawContext.drawTexture(OPTIONS_BACKGROUND_TEXTURE, 0, 0, 0, 0, width, TOP_BAR_SIZE);
        drawContext.drawTexture(OPTIONS_BACKGROUND_TEXTURE, 0, height-BOTTOM_BAR_SIZE, 0, 0, width, BOTTOM_BAR_SIZE);

        drawContext.drawText(textRenderer, screenTitle, this.width/2, (TOP_BAR_SIZE - textRenderer.fontHeight)/2, 0xffffff, false);
        ((ClickableWidget)this.children().get(0)).render(drawContext, mouseX, mouseY, partialTicks);
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

    private void enableColorSelector(String option, ClickableWidget element) {
        for (int i=1; i<children().size(); i++) {
            ((ClickableWidget)children().get(i)).visible = false;
        }
        colorSelector.setCurrentColor((ConfigurationMinecraftColor) handler.getIConfig().getValue(option));
        colorSelector.visible = true;
        colorSelector.setLink(option, element);
    }

    private void enableColorPicker(String option, ClickableWidget element) {
        for (int i=1; i<children().size(); i++) {
            ((ClickableWidget)children().get(i)).visible = false;
        }
        colorPicker.setCurrentColor((ConfigurationTrueColor) handler.getIConfig().getValue(option));
        colorPicker.visible = true;
        colorPicker.setLink(option, element);
    }

    public void subscreenFinished() {
        for (int i=1; i<children().size(); i++) {
            ((ClickableWidget)children().get(i)).visible = true;
        }
        colorSelector.visible = false;
        colorPicker.visible = false;
    }

    @Override
    public boolean wasMouseReleased() {
        boolean result = mouseReleased;
        mouseReleased = false;
        return result;
    }

    @Override
    public void setMouseReleased(boolean value) {
        mouseReleased = value;
    }

    @Override
    public void onConfigChanging(String option, Object value) {
        handler.getIConfig().setValue(option, value);
        if (value instanceof ConfigurationMinecraftColor) {
            value = ((ConfigurationMinecraftColor)value).colorIndex;
        } else if (value instanceof ConfigurationTrueColor) {
            value = ((ConfigurationTrueColor)value).getInt();
        }
        handler.onConfigChanging(new ConfigChangedEvent.OnConfigChangingEvent(modName, option, value));
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }
}