package com.nisovin.magicspells.util.itemreader;

import com.nisovin.magicspells.util.magicitems.MagicItemData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.MAX_STACK_SIZE;

public class MaxStackSizeHandler {
    private static final String CONFIG_NAME = MAX_STACK_SIZE.toString();

    public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
        Integer maxStackSize = null;
        if (config.isInt(CONFIG_NAME)) {
            maxStackSize = config.getInt(CONFIG_NAME);
        } else if (config.isString(CONFIG_NAME)) {
            try {
                maxStackSize = Integer.valueOf(config.getString(CONFIG_NAME));
            } catch (NumberFormatException ignored) {}
        } else return;


        meta.setMaxStackSize(maxStackSize);
        data.setAttribute(MAX_STACK_SIZE, maxStackSize);
    }

    public static void processItemMeta(ItemMeta meta, MagicItemData data) {
        if (data.hasAttribute(MAX_STACK_SIZE)) meta.setMaxStackSize((int) data.getAttribute(MAX_STACK_SIZE));
    }

    public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
        if (meta.hasMaxStackSize()) data.setAttribute(MAX_STACK_SIZE, meta.getMaxStackSize());
    }

}


