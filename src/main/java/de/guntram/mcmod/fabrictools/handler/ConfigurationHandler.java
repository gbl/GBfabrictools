package de.guntram.mcmod.fabrictools.handler;

import de.guntram.mcmod.fabrictools.client.gui.Choice;
import de.guntram.mcmod.fabrictools.ConfigChangedEvent;
import de.guntram.mcmod.fabrictools.Configuration;
import de.guntram.mcmod.fabrictools.ModConfigurationHandler;
import java.io.File;

import de.guntram.mcmod.fabrictools.TestGBfabrictools;
import net.minecraft.util.Formatting;

public class ConfigurationHandler implements ModConfigurationHandler
{
    private static ConfigurationHandler instance;

    private Configuration config;
    private String configFileName;

    private int choice=0;
    private boolean bool_test;
    private int int_test;
    private String test_string;
    private Formatting tooltipColor;
    private int color1;
    private int color2;

    public static ConfigurationHandler getInstance() {
        if (instance==null)
            instance=new ConfigurationHandler();
        return instance;
    }
    
    public void load(final File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            configFileName=configFile.getPath();
            loadConfig();
        }
    }
    
    public static String getConfigFileName() {
        return getInstance().configFileName;
    }

    @Override
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(TestGBfabrictools.MODNAME)) {
            loadConfig();
        }
    }
    
    @Override
    public void onConfigChanging(ConfigChangedEvent.OnConfigChangingEvent event) {
        if (event.getModID().equals(TestGBfabrictools.MODNAME)) {
            switch (event.getItem()) {
                case "fabrictools.config.choice": choice=(int)(Integer)(event.getNewValue()); break;
                case "fabrictools.config.bool_test": bool_test=(boolean)(Boolean)(event.getNewValue()); break;
                case "fabrictools.config.test_string": test_string=event.getNewValue().toString(); break;
            }
        }
    }
    
    private void loadConfig() {
        
        choice=config.getSelection("fabrictools.config.choice", Configuration.CATEGORY_CLIENT, 0,
                new String[] {
                    "fabrictools.config.choice_one",
                    "fabrictools.config.choice_two",
                    "fabrictools.config.choice_three",
                    "fabrictools.config.choice_four",
                }, 
                "fabrictools.config.tt.choice");
        bool_test = config.getBoolean("fabrictools.config.bool_test",
                Configuration.CATEGORY_CLIENT, true,
                "fabrictools.config.tt.bool_test");
        int_test = config.getInt("fabrictools.config.int_test",
                Configuration.CATEGORY_CLIENT, 10, 0, 100,
                "fabrictools.config.tt.int_test");
        test_string = config.getString("fabrictools.config.test_string",
                Configuration.CATEGORY_CLIENT, "test string",
                "fabrictools.config.tt.test_string");
        color1 = config.getInt("fabrictools.config.color1",
                Configuration.CATEGORY_CLIENT, color1, 0, 15, "fabrictools.config.tt.color1");
        color2 = config.getInt("fabrictools.config.color2",
                Configuration.CATEGORY_CLIENT, color2, 0, 0xffffff, "fabrictools.config.tt.color2");
        if (config.hasChanged())
            config.save();
    }
    
    public static Formatting getTooltipColor() {
        return getInstance().tooltipColor;
    }
    
    public static Choice getChoice() {
        return Choice.values()[getInstance().choice];
    }
    
    @Override
    public Configuration getConfig() {
        return getInstance().config;
    }
    
    public static boolean getBool_test() {
        return getInstance().bool_test;
    }
    
    public static int getInt_test() { return getInstance().int_test; }
    
}
