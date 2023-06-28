package de.guntram.mcmod.fabrictools;

import de.guntram.mcmod.fabrictools.IConfiguration;
import de.guntram.mcmod.fabrictools.ModConfigurationHandler;
import de.guntram.mcmod.fabrictools.Types.ConfigurationMinecraftColor;
import de.guntram.mcmod.fabrictools.Types.ConfigurationTrueColor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public class GuiModOptions {

    public static Screen getGuiModOptions(Screen parent, String modname, ModConfigurationHandler handler) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setSavingRunnable( () -> {
                    handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(modname));
                })
                .setParentScreen(parent)
                .setTitle(Text.of(modname + " Configuration"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        IConfiguration ic = handler.getIConfig();
        ConfigCategory clientCategory = builder.getOrCreateCategory(Text.translatable("General"));
        for (String option: ic.getKeys()) {
            Object value = ic.getValue(option);
            if (value == null) {
                continue;
            } else if (ic.isSelectList(option)) {
                String[] list = ic.getListOptions(option);
                clientCategory.addEntry(entryBuilder.startSelector(Text.translatable(option), list, list[(int)value])
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
            } else if (value instanceof Boolean) {
                clientCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable(option), (boolean) value)
                        .setDefaultValue((boolean) ic.getDefault(option))
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
            } else if (value instanceof String) {
                clientCategory.addEntry(entryBuilder.startStrField(Text.translatable(option), (String) value)
                        .setDefaultValue((String) value)
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
/* Seems like Cloth Config doesn't have anything for this
            } else if (value instanceof ConfigurationMinecraftColor
                   ||  value instanceof Integer && (int) ic.getMin(option) == 0 && (int) ic.getMax(option) == 15) {
                clientCategory.addEntry(entryBuilder.startColorField(Text.translatable(option), (int) value)
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());

 */
            } else if (value instanceof ConfigurationTrueColor
                   ||  value instanceof Integer && (int) ic.getMin(option) == 0 && (int) ic.getMax(option) == 0xffffff
            ) {
                clientCategory.addEntry(entryBuilder.startColorField(Text.translatable(option), (int) value)
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
            } else if (value instanceof Integer) {
                clientCategory.addEntry(entryBuilder.startIntSlider(Text.translatable(option), (int) value, (int) ic.getMin(option), (int) ic.getMax(option))
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
            } else if (value instanceof Float) {
                clientCategory.addEntry(entryBuilder.startFloatField(Text.translatable(option), (float) value)
                        .setMin((float) ic.getMin(option))
                        .setMax((float) ic.getMax(option))
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
            } else if (value instanceof Float || value instanceof Double) {
                clientCategory.addEntry(entryBuilder.startDoubleField(Text.translatable(option), (double) value)
                        .setMin((double) ic.getMin(option))
                        .setMax((double) ic.getMax(option))
                        .setTooltip(Text.translatable(ic.getTooltip(option)))
                        .setSaveConsumer(newValue -> ic.setValue(option, newValue))
                        .build());
            } else {
                System.err.println("n.i. " + value.getClass().getCanonicalName());
            }
        }
        return builder.build();
    }
}
