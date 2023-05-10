package de.guntram.mcmod.fabrictools;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import de.guntram.mcmod.fabrictools.client.gui.GuiTestGBfabrictools;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_H;

public class TestGBfabrictools implements ClientModInitializer {
    public static final String MODID = "testfabrictools";
    public static final String MODNAME = "Test Fabric Tools";

    public static TestGBfabrictools instance;
    private static de.guntram.mcmod.fabrictools.handler.ConfigurationHandler confHandler;
    private static String changedWindowTitle;
    private KeyBinding showHide;

    @Override
    public void onInitializeClient() {
        setKeyBindings();
        confHandler= de.guntram.mcmod.fabrictools.handler.ConfigurationHandler.getInstance();
        ConfigurationProvider.register(MODNAME, confHandler);
        confHandler.load(ConfigurationProvider.getSuggestedFile(MODID));
        changedWindowTitle=null;
    }

    public static void setWindowTitle(String s) {
        changedWindowTitle=s;
    }

    public static String getWindowTitle() {
        return changedWindowTitle;
    }

    public void processKeyBinds() {
        if (showHide.wasPressed()) {
            GuiTestGBfabrictools.toggleVisibility();
        }
    }

    public void setKeyBindings() {
        final String category="key.categories.fabrictools";
        KeyBindingHelper.registerKeyBinding(showHide = new KeyBinding("key.fabrictools.showhide", InputUtil.Type.KEYSYM, GLFW_KEY_H, category));
        ClientTickEvents.END_CLIENT_TICK.register(e->processKeyBinds());
    }
}

