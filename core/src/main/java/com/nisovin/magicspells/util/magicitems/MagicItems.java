package com.nisovin.magicspells.util.magicitems;

import java.util.*;

import net.kyori.adventure.text.Component;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ItemUtil;
import com.nisovin.magicspells.util.itemreader.*;
import com.nisovin.magicspells.util.AttributeUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.managers.AttributeManager;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.*;

public class MagicItems {

	private static final Map<String, MagicItem> magicItems = new HashMap<>();
	private static final Map<ItemStack, MagicItemData> itemStackCache = new HashMap<>();

	public static Map<String, MagicItem> getMagicItems() {
		return magicItems;
	}

	public static Collection<String> getMagicItemKeys() {
		return magicItems.keySet();
	}

	public static Collection<MagicItem> getMagicItemValues() {
		return magicItems.values();
	}

	public static MagicItem getMagicItemByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		return magicItems.get(internalName);
	}

	public static ItemStack getItemByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		if (magicItems.get(internalName).getItemStack() == null) return null;
		return magicItems.get(internalName).getItemStack().clone();
	}

	public static MagicItemData getMagicItemDataByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		return magicItems.get(internalName).getMagicItemData();
	}

	public static MagicItemData getMagicItemDataFromItemStack(ItemStack itemStack) {
		if (itemStack == null) return null;

		MagicItemData cached = itemStackCache.get(itemStack);
		// We can do this because itemStackCache doesn't have any null values
		if (cached != null) return cached;

		MagicItemData data = new MagicItemData();

		// type
		data.setAttribute(TYPE, itemStack.getType());

		if (itemStack.getType().isAir()) {
			itemStackCache.put(itemStack, data);
			return data;
		}

		// amount
		data.setAttribute(AMOUNT, itemStack.getAmount());

		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			itemStackCache.put(itemStack, data);
			return data;
		}

		// name
		NameHandler.processMagicItemData(meta, data);

		// durability
		if (ItemUtil.hasDurability(itemStack.getType())) DurabilityHandler.processMagicItemData(meta, data);

		// repairCost
		RepairableHandler.processMagicItemData(meta, data);

		// customModelData
		CustomModelDataHandler.processMagicItemData(meta, data);

		// power, fireworkEffects
		FireworkHandler.processMagicItemData(meta, data);

		// unbreakable
		data.setAttribute(UNBREAKABLE, meta.isUnbreakable());

		// tooltip
		boolean tooltip = true;
		for (ItemFlag itemFlag : ItemFlag.values()) {
			if (!meta.getItemFlags().contains(itemFlag)) tooltip = false;
		}
		data.setAttribute(HIDE_TOOLTIP, tooltip);

		// color
		LeatherArmorHandler.processMagicItemData(meta, data);

		// potion, potionEffects, potionColor
		PotionHandler.processMagicItemData(meta, data);

		// suspiciousStew
		SuspiciousStewHandler.processMagicItemData(meta, data);

		// fireworkEffect
		FireworkEffectHandler.processMagicItemData(meta, data);

		// skullOwner
		SkullHandler.processMagicItemData(meta, data);

		// author, title, pages
		WrittenBookHandler.processMagicItemData(meta, data);

		// enchantments
		Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());
		if (ItemUtil.hasFakeEnchantment(meta)) {
			enchants.remove(Enchantment.FROST_WALKER);

			data.setAttribute(FAKE_GLINT, true);
		}
		if (!enchants.isEmpty()) data.setAttribute(ENCHANTS, enchants);

		// attributes
		if (meta.hasAttributeModifiers()) {
			Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
			if (modifiers != null && !modifiers.isEmpty()) data.setAttribute(ATTRIBUTES, meta.getAttributeModifiers());
		}

		// lore
		if (meta.hasLore()) {
			List<Component> lore = meta.lore();
			if (lore != null && !lore.isEmpty()) data.setAttribute(LORE, lore);
		}

		// patterns
		BannerHandler.processMagicItemData(meta, data);

		// block data
		BlockDataHandler.processMagicItemData(meta, data, itemStack.getType());

		itemStackCache.put(itemStack, data);
		return data;
	}

	public static MagicItemData getMagicItemDataFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str).getMagicItemData();

		return MagicItemDataParser.parseMagicItemData(str);
	}

	public static MagicItem getMagicItemFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str);

		MagicItem magicItem;
		MagicItemData itemData = MagicItemDataParser.parseMagicItemData(str);
		if (itemData == null) return null;

		magicItem = getMagicItemFromData(itemData);
		return magicItem;
	}

	public static MagicItem getMagicItemFromData(MagicItemData data) {
		if (data == null) return null;

		Material type = (Material) data.getAttribute(TYPE);
		if (type == null) return null;

		ItemStack item = new ItemStack(type);

		if (type.isAir()) return new MagicItem(item, data);

		if (data.hasAttribute(AMOUNT)) {
			int amount = (int) data.getAttribute(AMOUNT);
			if (amount >= 1) item.setAmount(amount);
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return new MagicItem(item, data);

		// Name
		NameHandler.processItemMeta(meta, data);

		// Lore
		LoreHandler.processItemMeta(meta, data);

		// Custom Model Data
		CustomModelDataHandler.processItemMeta(meta, data);

		// Enchantments
		if (data.hasAttribute(ENCHANTS)) {
			Map<Enchantment, Integer> enchantments = (Map<Enchantment, Integer>) data.getAttribute(ENCHANTS);
			for (Enchantment enchant : enchantments.keySet()) {
				int level = enchantments.get(enchant);

				if (meta instanceof EnchantmentStorageMeta) ((EnchantmentStorageMeta) meta).addStoredEnchant(enchant, level, true);
				else meta.addEnchant(enchant, level, true);
			}


		}

		if (data.hasAttribute(FAKE_GLINT)) {
			boolean fakeGlint = (boolean) data.getAttribute(FAKE_GLINT);

			if (fakeGlint && !meta.hasEnchants()) {
				ItemUtil.addFakeEnchantment(meta);
			}
		}

		// Armor color
		LeatherArmorHandler.processItemMeta(meta, data);

		// Potion effects and potion color
		PotionHandler.processItemMeta(meta, data);

		// Skull owner
		SkullHandler.processItemMeta(meta, data);

		// Durability
		if (ItemUtil.hasDurability(type)) DurabilityHandler.processItemMeta(meta, data);

		// Repair cost
		RepairableHandler.processItemMeta(meta, data);

		// Written book
		WrittenBookHandler.processItemMeta(meta, data);

		// Banner
		BannerHandler.processItemMeta(meta, data);

		// Firework Star
		FireworkEffectHandler.processItemMeta(meta, data);

		// Firework
		FireworkHandler.processItemMeta(meta, data);

		// Suspicious Stew
		SuspiciousStewHandler.processItemMeta(meta, data);

		// Block Data
		BlockDataHandler.processItemMeta(meta, data);

		// Unbreakable
		if (data.hasAttribute(UNBREAKABLE))
			meta.setUnbreakable((boolean) data.getAttribute(UNBREAKABLE));

		// Hide tooltip
		if (data.hasAttribute(HIDE_TOOLTIP) && (boolean) data.getAttribute(HIDE_TOOLTIP))
			meta.addItemFlags(ItemFlag.values());

		// Set meta
		item.setItemMeta(meta);

		// Attributes
		AttributeManager attributeManager = MagicSpells.getAttributeManager();
		Multimap<Attribute, AttributeModifier> attributes = (Multimap<Attribute, AttributeModifier>) data.getAttribute(ATTRIBUTES);
		if (attributes != null) {
			for (Attribute attribute : attributes.keySet()) {
				Collection<AttributeModifier> attributeModifiers = attributes.get(attribute);
				for (AttributeModifier modifier : attributeModifiers) {
					attributeManager.addItemAttribute(item, attribute, modifier);
				}
			}
		}

		return new MagicItem(item, data);
	}

	public static MagicItem getMagicItemFromSection(ConfigurationSection section) {
		try {
			// It MUST have a type option
			if (!section.contains("type")) return null;

			// See if this is managed by an alternative reader
			ItemStack item = AlternativeReaderManager.deserialize(section);
			if (item != null) {
				MagicItem magicItem = new MagicItem(item, getMagicItemDataFromItemStack(item));

				if (section.isList("ignored-attributes")) {
					EnumSet<MagicItemAttribute> ignoredAttributes = magicItem.getMagicItemData().getIgnoredAttributes();
					List<String> ignoredAttributeStrings = section.getStringList("ignored-attributes");

					for (String attr : ignoredAttributeStrings) {
						try {
							ignoredAttributes.add(MagicItemAttribute.valueOf(attr.toUpperCase().replace("-", "_")));
						} catch (IllegalArgumentException e) {
							DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
						}
					}
				}

				if (section.isList("blacklisted-attributes")) {
					EnumSet<MagicItemAttribute> blacklistedAttributes = magicItem.getMagicItemData().getBlacklistedAttributes();
					List<String> blacklistedAttributeStrings = section.getStringList("blacklisted-attributes");

					for (String attr : blacklistedAttributeStrings) {
						try {
							blacklistedAttributes.add(MagicItemAttribute.valueOf(attr.toUpperCase().replace("-", "_")));
						} catch (IllegalArgumentException e) {
							DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
						}
					}
				}

				if (section.isBoolean("strict-enchants"))
					magicItem.getMagicItemData().setStrictEnchants(section.getBoolean("strict-enchants"));

				if (section.isBoolean("strict-block-data"))
					magicItem.getMagicItemData().setStrictBlockData(section.getBoolean("strict-block-data"));

				if (section.isBoolean("strict-durability"))
					magicItem.getMagicItemData().setStrictDurability(section.getBoolean("strict-durability"));

				if (section.isBoolean("strict-enchant-level"))
					magicItem.getMagicItemData().setStrictEnchantLevel(section.getBoolean("strict-enchant-level"));

				return magicItem;
			}

			MagicItemData itemData = new MagicItemData();

			String typeString = section.getString("type");
			Material type = Util.getMaterial(typeString);
			if (type == null) {
				DebugHandler.debugBadEnumValue(Material.class, typeString);
				return null;
			}

			item = new ItemStack(type);
			itemData.setAttribute(TYPE, type);

			if (type.isAir()) return new MagicItem(item, itemData);

			if (section.isInt("amount")) {
				int amount = section.getInt("amount");

				if (amount >= 1) {
					item.setAmount(amount);
					itemData.setAttribute(AMOUNT, amount);
				}
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) return new MagicItem(item, itemData);

			// Name
			NameHandler.process(section, meta, itemData);

			// Lore
			LoreHandler.process(section, meta, itemData);

			// CustomModelData
			CustomModelDataHandler.process(section, meta, itemData);

			// Enchants
			// <enchantmentName> <level>
			if (section.isList("enchants")) {
				List<String> enchants = section.getStringList("enchants");
				for (String enchant : enchants) {

					String[] data = enchant.split(" ");
					Enchantment e = EnchantmentHandler.getEnchantment(data[0]);
					if (e == null) {
						MagicSpells.error('\'' + data[0] + "' could not be connected to an enchantment");
						continue;
					}

					int level = 0;
					if (data.length > 1) {
						try {
							level = Integer.parseInt(data[1]);
						} catch (NumberFormatException ex) {
							DebugHandler.debugNumberFormat(ex);
						}
					}

					if (meta instanceof EnchantmentStorageMeta) ((EnchantmentStorageMeta) meta).addStoredEnchant(e, level, true);
					else meta.addEnchant(e, level, true);
				}

				if (meta instanceof EnchantmentStorageMeta storageMeta) {

					if (storageMeta.hasStoredEnchants()) {
						Map<Enchantment, Integer> enchantments = storageMeta.getStoredEnchants();
						if (!enchantments.isEmpty()) itemData.setAttribute(ENCHANTS, enchantments);
					}
				} else if (meta.hasEnchants()) {
					Map<Enchantment, Integer> enchantments = meta.getEnchants();
					if (!enchantments.isEmpty()) itemData.setAttribute(ENCHANTS, enchantments);
				}
			}

			if (section.isBoolean("fake-glint")) {
				boolean fakeGlint = section.getBoolean("fake-glint");

				if (fakeGlint && !meta.hasEnchants()) {
					ItemUtil.addFakeEnchantment(meta);
					itemData.setAttribute(FAKE_GLINT, true);
				}
			}

			// Armor color
			LeatherArmorHandler.process(section, meta, itemData);

			// Potion effects, color, type
			PotionHandler.process(section, meta, itemData);

			// Skull owner
			SkullHandler.process(section, meta, itemData);

			// Durability
			if (ItemUtil.hasDurability(type)) DurabilityHandler.process(section, meta, itemData);

			// Repair cost
			RepairableHandler.process(section, meta, itemData);

			// Written book
			WrittenBookHandler.process(section, meta, itemData);

			// Banner
			BannerHandler.process(section, meta, itemData);

			// Firework Star
			FireworkEffectHandler.process(section, meta, itemData);

			// Firework
			FireworkHandler.process(section, meta, itemData);

			// Suspicious Stew
			SuspiciousStewHandler.process(section, meta, itemData);

			// Block Data
			BlockDataHandler.process(section, meta, itemData, type);

			// Unbreakable
			if (section.isBoolean("unbreakable")) {
				boolean unbreakable = section.getBoolean("unbreakable");

				meta.setUnbreakable(unbreakable);
				itemData.setAttribute(UNBREAKABLE, unbreakable);
			}

			if (MagicSpells.hideMagicItemTooltips()) {
				meta.addItemFlags(ItemFlag.values());
				itemData.setAttribute(HIDE_TOOLTIP, true);
			} else if (section.isBoolean("hide-tooltip")) {
				boolean hideTooltip = section.getBoolean("hide-tooltip");

				if (hideTooltip) meta.addItemFlags(ItemFlag.values());
				itemData.setAttribute(HIDE_TOOLTIP, hideTooltip);
			}

			// Set meta
			item.setItemMeta(meta);

			// Attributes
			//<attribute name> <value> (operation) (slot)
			AttributeManager attributeManager = MagicSpells.getAttributeManager();
			if (section.isList("attributes")) {
				List<String> attributes = section.getStringList("attributes");
				Multimap<Attribute, AttributeModifier> itemAttributes = HashMultimap.create();
				for (String str : attributes) {
					String[] args = str.split(" ");
					if (args.length < 2) continue;

					Attribute attribute = AttributeUtil.getAttribute(args[0]);
					double value = Double.parseDouble(args[1]);

					AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
					if (args.length >= 3) operation = AttributeUtil.getOperation(args[2]);

					EquipmentSlot slot = null;
					if (args.length >= 4) {
						try {
							slot = EquipmentSlot.valueOf(args[3].toUpperCase());
						} catch (Exception ignored) {}
					}

					AttributeModifier modifier = new AttributeModifier(java.util.UUID.randomUUID(), args[0], value, operation, slot);
					attributeManager.addItemAttribute(item, attribute, modifier);
					itemAttributes.put(attribute, modifier);
				}

				if (!itemAttributes.isEmpty()) itemData.setAttribute(ATTRIBUTES, itemAttributes);
			}

			if (section.isList("ignored-attributes")) {
				List<String> ignoredAttributeStrings = section.getStringList("ignored-attributes");
				EnumSet<MagicItemAttribute> ignoredAttributes = itemData.getIgnoredAttributes();

				for (String attr : ignoredAttributeStrings) {
					try {
						ignoredAttributes.add(MagicItemAttribute.valueOf(attr.toUpperCase().replace("-", "_")));
					} catch (IllegalArgumentException e) {
						DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
					}
				}
			}

			if (section.isList("blacklisted-attributes")) {
				List<String> blacklistedAttributeStrings = section.getStringList("blacklisted-attributes");
				EnumSet<MagicItemAttribute> blacklistedAttributes = itemData.getBlacklistedAttributes();

				for (String attr : blacklistedAttributeStrings) {
					try {
						blacklistedAttributes.add(MagicItemAttribute.valueOf(attr.toUpperCase().replace("-", "_")));
					} catch (IllegalArgumentException e) {
						DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
					}
				}
			}

			if (section.isBoolean("strict-enchants"))
				itemData.setStrictEnchants(section.getBoolean("strict-enchants"));

			if (section.isBoolean("strict-block-data"))
				itemData.setStrictBlockData(section.getBoolean("strict-block-data"));

			if (section.isBoolean("strict-durability"))
				itemData.setStrictDurability(section.getBoolean("strict-durability"));

			if (section.isBoolean("strict-enchant-level"))
				itemData.setStrictEnchantLevel(section.getBoolean("strict-enchant-level"));

			return new MagicItem(item, itemData);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
