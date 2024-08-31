package com.nisovin.magicspells.util.itemreader;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.CUSTOM_MODEL_DATA;

public class CustomModelDataHandler {

	private static final String CONFIG_NAME = CUSTOM_MODEL_DATA.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		Integer customModelData = null;
		if (config.isInt(CONFIG_NAME)) {
			customModelData = config.getInt(CONFIG_NAME);
		} else if (config.isString(CONFIG_NAME)) {
			try {
				customModelData = Integer.valueOf(config.getString(CONFIG_NAME));
			} catch (NumberFormatException ignored) {}
		} else return;


		meta.setCustomModelData(customModelData);
		data.setAttribute(CUSTOM_MODEL_DATA, customModelData);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (data.hasAttribute(CUSTOM_MODEL_DATA)) meta.setCustomModelData((int) data.getAttribute(CUSTOM_MODEL_DATA));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (meta.hasCustomModelData()) data.setAttribute(CUSTOM_MODEL_DATA, meta.getCustomModelData());
	}

}
