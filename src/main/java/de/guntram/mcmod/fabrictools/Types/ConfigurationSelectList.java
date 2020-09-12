package de.guntram.mcmod.fabrictools.Types;

import de.guntram.mcmod.fabrictools.ConfigurationItem;

public class ConfigurationSelectList extends ConfigurationItem {
    
    final String[] options;
    
    public ConfigurationSelectList(String key, String toolTip, String[] options, Object value, Object defaultValue) {
        super(key, toolTip, value, defaultValue);
        this.options = options;
    }
    
    public String[] getOptions() {
        return options;
    }
}
