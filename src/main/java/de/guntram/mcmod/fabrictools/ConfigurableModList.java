/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.fabrictools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 *
 * @author gbl
 */
public class ConfigurableModList extends Screen {

    private final Screen parent;
    
    public ConfigurableModList(Screen parent) {
        super(Text.literal(I18n.translate("mod.options", new Object[0])));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        int y = 10;
        int x = 0;
        int pos = 0;
        int size = this.width / 4 - 20;

        List<String> sortedModNames = new ArrayList<>(ConfigurationProvider.getRegisteredMods());
        Collections.sort(sortedModNames);
        for (String modName: sortedModNames) {
            this.addDrawableChild(new ClickableWidget(x+10, y, size, 20, Text.literal(modName)) {
                @Override
                public void onClick(double x, double y) {
                    ModConfigurationHandler handler = ConfigurationProvider.getHandler(this.getMessage().getString());
                    if (handler != null) {
                        MinecraftClient.getInstance().setScreen(
                                new GuiModOptions(parent, this.getMessage().getString(), handler)
                        );
                    }
                }
                @Override
                protected void appendClickableNarrations(NarrationMessageBuilder narrationMessageBuilder) {
                }
                @Override
                public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                }
            });
            x += this.width / 4;
            if (++pos >= 4) {
                pos = 0;
                x = 0;
                y += 24;
            }
        }
    }
    
    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(stack);
        super.render(stack, mouseX, mouseY, partialTicks);
    }
}
