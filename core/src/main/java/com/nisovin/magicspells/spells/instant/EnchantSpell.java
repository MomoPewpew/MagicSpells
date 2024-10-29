package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.handlers.EnchantmentHandler;

public class EnchantSpell extends InstantSpell {
	
	private final Map<Enchantment, Integer> enchantments;

	private boolean safeEnchants;
	
	public EnchantSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		enchantments = new HashMap<>();

		List<String> enchantmentList = getConfigStringList("enchantments", null);

		safeEnchants = getConfigBoolean("safe-enchants", true);

		if (enchantmentList != null && !enchantmentList.isEmpty()) {
			for (String string : enchantmentList) {
				Enchantment enchant = null;
				int level = 1;
				String[] str = string.split(" ");
				if (str[0] != null) enchant = EnchantmentHandler.getEnchantment(str[0]);
				if (str.length > 1 && str[1] != null) level = Integer.parseInt(str[1]);
				if (enchant != null) enchantments.put(enchant, level);
			}
		} else MagicSpells.error("EnchantSpell '" + internalName + "' has invalid enchantments defined!");
	}
	
	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
            assert data.caster() != null;
            ItemStack targetItem = data.caster().getEquipment().getItemInMainHand();
			if (targetItem == null) return PostCastAction.ALREADY_HANDLED;
			enchant(targetItem);
			playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void enchant(ItemStack item) {
		for (Enchantment e : enchantments.keySet()) {
			enchant(item, e, enchantments.get(e));
		}
	}
	
	private void enchant(ItemStack item, Enchantment enchant, int level) {
		if (!enchant.canEnchantItem(item)) return;
		if (safeEnchants && level > enchant.getMaxLevel()) level = enchant.getMaxLevel();
		if (level <= 0) item.removeEnchantment(enchant);
		else {
			if (safeEnchants) item.addEnchantment(enchant, level);
			else item.addUnsafeEnchantment(enchant, level);
		}
	}

	public Map<Enchantment, Integer> getEnchantments() {
		return enchantments;
	}

	public boolean allowUnsafeEnchants() {
		return !safeEnchants;
	}

	public void setSafeEnchants(boolean safeEnchants) {
		this.safeEnchants = safeEnchants;
	}

}
