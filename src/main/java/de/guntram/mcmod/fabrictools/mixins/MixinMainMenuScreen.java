/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.fabrictools.mixins;

import de.guntram.mcmod.fabrictools.ConfigurableModList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.MainMenuScreen;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.RecipeBookButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 *
 * @author gbl
 */
@Mixin(MainMenuScreen.class)
public class MixinMainMenuScreen extends Screen {
    MixinMainMenuScreen() { super(null); }

    @Inject(method="init", at=@At("RETURN"))
    public void addConfigScreen(CallbackInfo ci) {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
            this.addButton(new RecipeBookButtonWidget(this.width - 24, 8,
                    20, 20, 0, 0, 0, new Identifier("textures/item/written_book.png"), 20, 20, (buttonWidget_1) -> {
               this.minecraft.openScreen(new ConfigurableModList((Screen)this));
            }));
        }
    }
}
