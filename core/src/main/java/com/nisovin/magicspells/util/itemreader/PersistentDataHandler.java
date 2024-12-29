package com.nisovin.magicspells.util.itemreader;

import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.MAGIC_ITEM_NAME;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.PERSISTENT_DATA;

public class PersistentDataHandler {

    private static final String CONFIG_NAME = PERSISTENT_DATA.toString();
    private static final NamespacedKey DATA_KEY = new NamespacedKey(MagicSpells.getInstance(), "magicspellsData");
    private static final NamespacedKey NAME_KEY = new NamespacedKey(MagicSpells.getInstance(), "magicitem");

    public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
        meta.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, config.getName());
        data.setAttribute(MAGIC_ITEM_NAME, config.getName());

        if (!config.isList(CONFIG_NAME)) return;
        if (!meta.getPersistentDataContainer().isEmpty()) return;

        List<String> pdcList = config.getStringList(CONFIG_NAME);

        meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.LIST.strings(), pdcList);
        data.setAttribute(PERSISTENT_DATA, pdcList);
    }

    public static void processItemMeta(ItemMeta meta, MagicItemData data) {
        if (data.hasAttribute(MAGIC_ITEM_NAME)) {
            meta.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, (String) data.getAttribute(MAGIC_ITEM_NAME));
        }

        if (data.hasAttribute(PERSISTENT_DATA)) {
            List<String> pdcList = (List<String>) data.getAttribute(PERSISTENT_DATA);
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.LIST.strings(), pdcList);
        }
    }

    public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(NAME_KEY, PersistentDataType.STRING)) {
            String magicitemName = container.get(NAME_KEY, PersistentDataType.STRING);
            data.setAttribute(MAGIC_ITEM_NAME, magicitemName);
        }

        if (container.has(DATA_KEY, PersistentDataType.LIST.strings())) {
            List<String> pdcList = container.get(DATA_KEY, PersistentDataType.LIST.strings());
            data.setAttribute(PERSISTENT_DATA, pdcList);
        }

    }

}
