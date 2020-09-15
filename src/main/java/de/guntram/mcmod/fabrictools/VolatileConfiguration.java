/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.fabrictools;

import com.google.common.collect.Lists;
import de.guntram.mcmod.fabrictools.Types.ConfigurationSelectList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gbl
 */
public class VolatileConfiguration implements Configuration {
    
    private Map<String, ConfigurationItem> items;
    
    private void VolatileConfiguration() {
        items = new HashMap<>();
    }
    
    public void addItem(ConfigurationItem item) {
        items.put(item.key, item);
    }

    @Override
    public List<String> getKeys() {
        return Lists.newArrayList(items.keySet());
    }

    @Override
    public Object getValue(String option) {
        return items.get(option).value;
    }

    @Override
    public boolean setValue(String option, Object value) {
        ConfigurationItem item = items.get(option);
        if (item == null) {
            return false;
        }
        item.value = value;
        return true;
    }

    @Override
    public Object getDefault(String option) {
        return items.get(option).defaultValue;
    }

    @Override
    public Object getMin(String option) {
        return items.get(option).minValue;
    }

    @Override
    public Object getMax(String option) {
        return items.get(option).maxValue;
    }

    @Override
    public String getTooltip(String option) {
        return items.get(option).toolTip;
    }

    @Override
    public boolean isSelectList(String option) {
        return items.get(option).defaultValue instanceof ConfigurationSelectList;
    }

    @Override
    public String[] getListOptions(String option) {
        ConfigurationItem item = items.get(option);
        if (item instanceof ConfigurationSelectList) {
            return ((ConfigurationSelectList)item).getOptions();
        }
        return null;
    }
}
