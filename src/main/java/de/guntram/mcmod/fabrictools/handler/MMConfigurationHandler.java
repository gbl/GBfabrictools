package de.guntram.mcmod.fabrictools.handler;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.guntram.mcmod.fabrictools.TestGBfabrictools;
import de.guntram.mcmod.fabrictools.ConfigurationProvider;
import de.guntram.mcmod.fabrictools.GuiModOptions;

public class MMConfigurationHandler implements ModMenuApi
{
    @Override
    public ConfigScreenFactory getModConfigScreenFactory() {
        return screen -> new GuiModOptions(screen, TestGBfabrictools.MODNAME, ConfigurationProvider.getHandler(TestGBfabrictools.MODNAME));
    }
}
