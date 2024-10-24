package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.COLOR;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.POTION_DATA;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.POTION_EFFECTS;

public class PotionHandler {

	public static final String POTION_EFFECT_CONFIG_NAME = POTION_EFFECTS.toString();
	public static final String POTION_DATA_CONFIG_NAME = POTION_DATA.toString();
	public static final String POTION_COLOR_CONFIG_NAME = "potion-color";

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		if (config.isList(POTION_EFFECT_CONFIG_NAME)) {
			potionMeta.clearCustomEffects();
			List<String> potionEffectStrings = config.getStringList(POTION_EFFECT_CONFIG_NAME);
			List<PotionEffect> potionEffects = new ArrayList<>();

			for (String potionEffect : potionEffectStrings) {
				PotionEffect eff = Util.buildPotionEffect(potionEffect);
				if (eff == null) continue;

				potionMeta.addCustomEffect(eff, true);
				potionEffects.add(eff);
			}

			if (!potionEffects.isEmpty()) data.setAttribute(POTION_EFFECTS, potionEffects);
		}

		if (config.isString(POTION_COLOR_CONFIG_NAME)) {
			try {
				int color = Integer.parseInt(config.getString(POTION_COLOR_CONFIG_NAME, "").replace("#", ""), 16);
				Color c = Color.fromRGB(color);

				potionMeta.setColor(c);
				data.setAttribute(COLOR, c);
			} catch (NumberFormatException e) {
				DebugHandler.debugNumberFormat(e);
			}
		}

		if (config.isString(POTION_DATA_CONFIG_NAME)) {
			String potionDataString = config.getString(POTION_DATA_CONFIG_NAME, "");
			String[] args = potionDataString.split(" ");

			boolean extended = false, upgraded = false;
			PotionType potionType;
			try {
				potionType = PotionType.valueOf(args[0].toUpperCase());

				if (args.length > 1) {
					if (args[1].equalsIgnoreCase("extended")) extended = true;
					else if (args[1].equalsIgnoreCase("upgraded")) upgraded = true;
				}

				PotionData potionData = new PotionData(potionType, extended, upgraded);

				potionMeta.setBasePotionData(potionData);
				data.setAttribute(POTION_DATA, potionData);
			} catch (IllegalArgumentException e) {
				DebugHandler.debugIllegalArgumentException(e);
			}
		}
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		if (data.hasAttribute(POTION_EFFECTS)) {
			potionMeta.clearCustomEffects();
			((List<PotionEffect>) data.getAttribute(POTION_EFFECTS)).forEach(potionEffect -> potionMeta.addCustomEffect(potionEffect, true));
		}
		if (data.hasAttribute(COLOR)) potionMeta.setColor((Color) data.getAttribute(COLOR));
		if (data.hasAttribute(POTION_DATA)) potionMeta.setBasePotionData((PotionData) data.getAttribute(POTION_DATA));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		data.setAttribute(POTION_DATA, potionMeta.getBasePotionData());
		if (potionMeta.hasCustomEffects()) {
			List<PotionEffect> effects = potionMeta.getCustomEffects();
			if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
		}
		if (potionMeta.hasColor()) data.setAttribute(COLOR, potionMeta.getColor());
	}

	public static PotionData getPotionData(ItemMeta meta) {
		if (!(meta instanceof PotionMeta potionMeta)) return null;
		return potionMeta.getBasePotionData();
	}
	
}
