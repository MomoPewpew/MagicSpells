package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.itemreader.PotionHandler;
import com.nisovin.magicspells.util.itemreader.DurabilityHandler;
import com.nisovin.magicspells.util.itemreader.WrittenBookHandler;
import com.nisovin.magicspells.util.itemreader.LeatherArmorHandler;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.*;

public class CastItem {

	private Material type = null;
	private String name = null;

	private int amount = 0;
	private int durability = -1;
	private int customModelData = 0;
	private boolean unbreakable = false;

	private Color color = null;
	private PotionData potionData = null;
	private String title = null;
	private String author = null;

	private Map<Enchantment, Integer> enchants = null;
	private List<String> lore = null;

	public CastItem() {

	}

	public CastItem(ItemStack item) {
		if (item == null) throw new NullPointerException("itemStack");
		ItemMeta meta = item.getItemMeta();

		type = item.getType();
		if (isTypeValid()) {
			if (!MagicSpells.ignoreCastItemNames()) {
				String nameLocal = Util.getLegacyFromComponent(meta.displayName());
				if (!meta.hasDisplayName()) name = null;
				else if (MagicSpells.ignoreCastItemNameColors()) name = Util.decolorize(nameLocal);
				else name = nameLocal;
			}
			if (!MagicSpells.ignoreCastItemAmount()) amount = item.getAmount();
			if (!MagicSpells.ignoreCastItemDurability(type) && ItemUtil.hasDurability(type)) durability = DurabilityHandler.getDurability(meta);
			if (!MagicSpells.ignoreCastItemCustomModelData()) customModelData = ItemUtil.getCustomModelData(meta);
			if (!MagicSpells.ignoreCastItemBreakability()) unbreakable = meta.isUnbreakable();
			if (!MagicSpells.ignoreCastItemColor()) color = LeatherArmorHandler.getColor(meta);
			if (!MagicSpells.ignoreCastItemPotionType()) potionData = PotionHandler.getPotionData(meta);
			if (!MagicSpells.ignoreCastItemTitle()) title = WrittenBookHandler.getTitle(meta);
			if (!MagicSpells.ignoreCastItemAuthor()) author = WrittenBookHandler.getAuthor(meta);
			if (!MagicSpells.ignoreCastItemEnchants()) enchants = meta.getEnchants();
			if (!MagicSpells.ignoreCastItemLore() && meta.hasLore()) {
				List<String> loreLocal = new ArrayList<>();
				for (Component component : meta.lore()) {
					loreLocal.add(Util.getLegacyFromComponent(component));
				}
				lore = loreLocal;
			}
		}
	}

	public CastItem(String string) {
		MagicItemData data = MagicItems.getMagicItemDataFromString(string);
		if (data != null) {
			type = (Material) data.getAttribute(TYPE);
			if (isTypeValid()) {
				if (!MagicSpells.ignoreCastItemNames() && data.hasAttribute(NAME)) {
					String localName = Util.getLegacyFromComponent((Component) data.getAttribute(NAME));
					if (MagicSpells.ignoreCastItemNameColors()) name = Util.decolorize(localName);
					else name = localName;
				}

				if (!MagicSpells.ignoreCastItemAmount() && data.hasAttribute(AMOUNT))
					amount = (int) data.getAttribute(AMOUNT);

				if (!MagicSpells.ignoreCastItemDurability(type) && ItemUtil.hasDurability(type) && data.hasAttribute(DURABILITY))
					durability = (int) data.getAttribute(DURABILITY);

				if (!MagicSpells.ignoreCastItemCustomModelData() && data.hasAttribute(CUSTOM_MODEL_DATA))
					customModelData = (int) data.getAttribute(CUSTOM_MODEL_DATA);

				if (!MagicSpells.ignoreCastItemBreakability() && data.hasAttribute(UNBREAKABLE))
					unbreakable = (boolean) data.getAttribute(UNBREAKABLE);

				if (!MagicSpells.ignoreCastItemColor() && data.hasAttribute(COLOR))
					color = (Color) data.getAttribute(COLOR);

				if (!MagicSpells.ignoreCastItemPotionType() && data.hasAttribute(POTION_DATA))
					potionData = (PotionData) data.getAttribute(POTION_DATA);

				if (!MagicSpells.ignoreCastItemTitle() && data.hasAttribute(TITLE))
					title = (String) data.getAttribute(TITLE);

				if (!MagicSpells.ignoreCastItemAuthor() && data.hasAttribute(AUTHOR))
					author = (String) data.getAttribute(AUTHOR);

				if (!MagicSpells.ignoreCastItemEnchants() && data.hasAttribute(ENCHANTS))
					enchants = (Map<Enchantment, Integer>) data.getAttribute(ENCHANTS);

				if (!MagicSpells.ignoreCastItemLore() && data.hasAttribute(LORE)) {
					List<String> loreLocal = new ArrayList<>();
					for (Component component : (List<Component>) data.getAttribute(LORE)) {
						loreLocal.add(Util.getLegacyFromComponent(component));
					}
					lore = loreLocal;
				}
			}
		}
	}

	public boolean isTypeValid() {
		return type != null && !BlockUtils.isAir(type);
	}

	public Material getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CastItem item) return equalsCastItem(item);
		if (o instanceof ItemStack item) return equalsCastItem(new CastItem(item));
		return false;
	}

	public boolean equalsCastItem(CastItem i) {
		if (i == null) return false;

		return type == i.type
			&& (MagicSpells.ignoreCastItemDurability(type) || durability == i.durability)
			&& (MagicSpells.ignoreCastItemAmount() || amount == i.amount)
			&& (MagicSpells.ignoreCastItemNames() || Objects.equals(name, i.name))
			&& (MagicSpells.ignoreCastItemCustomModelData() || customModelData == i.customModelData)
			&& (MagicSpells.ignoreCastItemBreakability() || unbreakable == i.unbreakable)
			&& (MagicSpells.ignoreCastItemColor() || Objects.equals(color, i.color))
			&& (MagicSpells.ignoreCastItemPotionType() || Objects.equals(potionData, i.potionData))
			&& (MagicSpells.ignoreCastItemTitle() || Objects.equals(title, i.title))
			&& (MagicSpells.ignoreCastItemAuthor() || Objects.equals(author, i.author))
			&& (MagicSpells.ignoreCastItemEnchants() || Objects.equals(enchants, i.enchants))
			&& (MagicSpells.ignoreCastItemLore() || Objects.equals(lore, i.lore));
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, name, amount, durability, customModelData, unbreakable, color, potionData, title, author, enchants, lore);
	}

	@Override
	public String toString() {
		if (type == null) return "";

		StringBuilder output = new StringBuilder();
		boolean previous = false;

		output.append(type.name());

		if (!MagicSpells.ignoreCastItemNames() && name != null) {
			output.append("{");

			output
				.append("\"name\":\"")
				.append(TxtUtil.escapeJSON(Util.getLegacyFromMiniMessage(name)))
				.append('"');

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemAmount()) {
			if (previous) output.append(',');
			else output.append("{");

			output
				.append("\"amount\":")
				.append(amount);

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemDurability(type) && ItemUtil.hasDurability(type)) {
			if (previous) output.append(',');
			else output.append("{");

			output
				.append("\"durability\":")
				.append(durability);

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemCustomModelData()) {
			if (previous) output.append(',');
			else output.append("{");

			output
				.append("\"custommodeldata\":")
				.append(customModelData);

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemBreakability()) {
			if (previous) output.append(',');

			output
				.append("\"unbreakable\":")
				.append(unbreakable);

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemColor() && color != null) {
			if (previous) output.append(',');
            else output.append("{");

			String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

			output
				.append("\"color\":\"")
				.append(hex)
				.append('"');

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemPotionType() && potionData != null) {
			if (previous) output.append(',');
            else output.append("{");

			output
				.append("\"potiondata\":\"")
				.append(potionData.getType());

			if (potionData.isExtended()) output.append(" extended");
			else if (potionData.isUpgraded()) output.append(" upgraded");

			output.append('"');

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemTitle() && title != null) {
			if (previous) output.append(',');
            else output.append("{");

			output
				.append("\"title\":\"")
				.append(TxtUtil.escapeJSON(title))
				.append('"');

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemAuthor() && author != null) {
			if (previous) output.append(',');
            else output.append("{");

			output
				.append("\"author\":\"")
				.append(TxtUtil.escapeJSON(author))
				.append('"');

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemEnchants() && enchants != null) {
			if (previous) output.append(',');
            else output.append("{");

			boolean previousEnchantment = false;
			output.append("\"enchants\":{");
			for (Enchantment enchant : enchants.keySet()) {
				if (previousEnchantment) output.append(',');

				output
					.append('"')
					.append(enchant.getKey().getKey())
					.append("\":")
					.append(enchants.get(enchant));

				previousEnchantment = true;
			}
			output.append('}');

			previous = true;
		}

		if (!MagicSpells.ignoreCastItemLore() && lore != null) {
			if (previous) output.append(',');
            else output.append("{");

			boolean previousLore = false;
			output.append("\"lore\":[");
			for (String line : lore) {
				if (previousLore) output.append(',');

				output
					.append('"')
					.append(TxtUtil.escapeJSON(Util.getLegacyFromMiniMessage(line)))
					.append('"');

				previousLore = true;
			}

			output.append(']');
			previous = true;
		}

		if (previous) output.append("}");

		return output.toString();
	}

}
