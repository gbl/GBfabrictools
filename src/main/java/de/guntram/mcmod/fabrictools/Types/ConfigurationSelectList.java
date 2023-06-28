package de.guntram.mcmod.fabrictools.Types;

import de.guntram.mcmod.fabrictools.ConfigurationItem;
import java.util.function.Consumer;

public class ConfigurationSelectList extends ConfigurationItem {
    
    final String[] options;
    
    public ConfigurationSelectList(String key, String toolTip, String[] options, Object value, Object defaultValue) {
        this(key, toolTip, options, value, defaultValue, null);
    }

    public ConfigurationSelectList(String key, String toolTip, String[] options, Object value, Object defaultValue, Consumer<Object> changeHandler) {
        super(key, toolTip, value, defaultValue, 0, 0, changeHandler);
        this.options = options;
    }
    
    public String[] getOptions() {
        return options;
    }

    public void setValue(Object value) {
        if (value instanceof Integer) {
            super.setValue(value);
        } else {
            for (int i=0; i<options.length; i++) {
                if (options[i].equals(value)) {
                    super.setValue(i);
                    break;
                }
            }
        }
    }
}
