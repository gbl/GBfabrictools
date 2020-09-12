/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.fabrictools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.guntram.mcmod.fabrictools.Types.ConfigurationMinecraftColor;
import de.guntram.mcmod.fabrictools.Types.ConfigurationSelectList;
import de.guntram.mcmod.fabrictools.Types.ConfigurationTrueColor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resource.language.I18n;

/**
 *
 * @author gbl
 */
public class Configuration {
    
    public final static int CATEGORY_CLIENT = 0;            // ignored, forge compat.
    public final static int CATEGORY_GENERAL = 1;           // ignored, forge compat.

    private static final java.lang.reflect.Type MAP_TYPE = new TypeToken<Map<String, ConfigurationItem>>() {}.getType();
    private Map<String, ConfigurationItem> items;
    private boolean wasChanged;
    private File configFile;

    public Configuration(File configFile) {
        this.configFile=configFile;
        items=new HashMap<>();
        try (JsonReader reader = new JsonReader(new FileReader(configFile))) {
            Gson gson=new Gson();
            items=gson.fromJson(reader, MAP_TYPE);
        } catch (FileNotFoundException ex) {
            // do nothing, probably first time starting
        } catch (IOException ex) {
            System.err.println("Trying to load config file "+configFile.getAbsolutePath()+":");
            ex.printStackTrace(System.err);
        } catch (JsonSyntaxException ex) {
            System.err.println("Syntax error in config file "+configFile.getAbsolutePath()+" - using defaults");
            ex.printStackTrace(System.err);
        }
        wasChanged=false;
    }
    
    public boolean hasChanged() {
        return wasChanged;
    }
    
    public void save() {
        // NB we're saving all class elements, but we'll replace all but value
        // with defaults from code when reading, effectively ignoring everything
        // except value. This is for the benefit of users who want to edit
        // the json file itself.
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(items, writer);
        } catch (IOException ex) {
            System.err.println("Trying to save config file "+configFile.getAbsolutePath()+":");
            ex.printStackTrace(System.err);
        }        
    }

    public int getInt(String description, int category, int defVal, int min, int max, String toolTip) {
        return (int) (Integer) getValue(description, category, defVal, min, max, toolTip, Integer.class);
    }

    public float getFloat(String description, int category, float defVal, float min, float max, String toolTip) {
        return (float) (Float) getValue(description, category, defVal, min, max, toolTip, Float.class);
    }

    public boolean getBoolean(String description, int category, boolean defVal, String toolTip) {
        return (boolean) (Boolean) getValue(description, category, defVal, toolTip, Boolean.class);
    }
    
    public String getString(String description, int category, String defVal, String toolTip) {
        return (String) getValue(description, category, defVal, toolTip, String.class);
    }
    
    public int getIndexedColor(String description, int category, int defIndex, String toolTip) {
        return (int) getValue(description, category, defIndex, 0, 15, toolTip, ConfigurationMinecraftColor.class);
    }

    public int getTrueColor(String description, int category, int defIndex, String toolTip) {
        return (int) getValue(description, category, defIndex, 0, 0xffffff, toolTip, ConfigurationTrueColor.class);
    }
    
    public int getSelection(String description, int category, int defVal, String[] options, String toolTip) {
        ConfigurationItem item=items.get(description);
        if (item==null) {
            items.put(description, new ConfigurationSelectList(description, toolTip, options, defVal, defVal));
            wasChanged=true;
            return defVal;
        }
        else if (!(item instanceof ConfigurationSelectList)) {
            // e.g. we changed the definition from int to list
            // replace the item with a select list but use the item value
            ConfigurationSelectList list = new ConfigurationSelectList(description, toolTip, options, item.value, defVal);
            items.put(description, list);
        }
        return (int) getValue(description, category, defVal, 0, options.length-1, toolTip, Integer.class);
    }

    public Object getValue(String description, int category, Object defVal, String toolTip, Class clazz) {
        return getValue(description, category, defVal, null, null, toolTip, clazz);
    }
    
    public Object getValue(String description, int category, Object defVal, Object minVal, Object maxVal, String toolTip, Class clazz) {
        ConfigurationItem item=items.get(description);
        if (item==null) {
            items.put(description, new ConfigurationItem(description, toolTip, defVal, defVal, minVal, maxVal));
            wasChanged=true;
            return defVal;
        }
        
        // Always let code given meta info override config file values
        item.key=I18n.translate(description);
        item.minValue=minVal;
        item.maxValue=maxVal;
        item.toolTip=toolTip;
        item.defaultValue=defVal;

        if (item.value.getClass()==clazz) {
            return item.value;
        } else if (item.value.getClass() == Double.class && clazz==Integer.class) {
            // repair gson reading int as double
            int value=(int)(double)(Double) item.value;
            item.value = (Integer) value;
            return item.value;
        } else if (item.value.getClass() == Double.class && clazz==Float.class) {
            // repair gson reading int as double
            float value=(float)(double)(Double) item.value;
            item.value = (Float) value;
            return item.value;
        } else if (item.value.getClass() == Integer.class && clazz == ConfigurationMinecraftColor.class) {
            int result = (Integer) item.value;
            item.value = new ConfigurationMinecraftColor(result);
            return result;
        } else if (clazz == ConfigurationMinecraftColor.class) {
            int  result = ((ConfigurationMinecraftColor)item.value).colorIndex;
            return result;
        } else if (item.value.getClass() == Integer.class && clazz == ConfigurationTrueColor.class) {
            int result = (Integer) item.value;
            item.value = new ConfigurationTrueColor(result);
            return result;
        } else if (clazz == ConfigurationTrueColor.class) {
            ConfigurationTrueColor tC = ((ConfigurationTrueColor)item.value);
            Integer result = tC.red << 16 | tC.green << 8 | tC.blue << 0;
            return result;
        }
        item.value=defVal;
        wasChanged=true;
        return defVal;
    }
    
    public Object getValue(String description) {
        return items.get(description).value;
    }

    public Object getDefault(String description) {
        return items.get(description).defaultValue;
    }
    
    public Object getMin(String description) {
        return items.get(description).minValue;
    }

    public Object getMax(String description) {
        return items.get(description).maxValue;
    }
    
    public String getTooltip(String description) {
        return items.get(description).toolTip;
    }
    
    public boolean isSelectList(String description) {
        return items.get(description) instanceof ConfigurationSelectList;
    }
    
    public String[] getListOptions(String description) {
        return ((ConfigurationSelectList) items.get(description)).getOptions();
    }
    
    /**
     * Forgets about a config parameter that was, for example, present in older versions.
     * @param item
     */
    public void forget(String item) {
        items.remove(item);
        wasChanged = true;
    }
    
    public void migrate(String oldKey, String newKey) {
        ConfigurationItem oldItem = items.get(oldKey);
        items.remove(oldKey);
        if (oldItem != null) {
            oldItem.key = newKey;
            items.put(newKey, oldItem);
        }
        wasChanged = true;
    }

    public boolean setValue(String description, Object value) {
        ConfigurationItem item=items.get(description);
        if (item==null)
            return false;
        item.value=value;
        wasChanged=true;
        return true;
    }
    
    public List getKeys() {
        List list=new ArrayList(items.keySet());
        Collections.sort(list);
        return list;
    }
}
