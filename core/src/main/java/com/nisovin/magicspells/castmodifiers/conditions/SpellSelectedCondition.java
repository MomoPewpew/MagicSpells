package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.castmodifiers.Condition;

public class SpellSelectedCondition extends Condition {

	private SpellFilter filter;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		filter = SpellFilter.fromString(var);
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return spellSelected(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return spellSelected(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean spellSelected(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		Spellbook spellbook = MagicSpells.getSpellbook(pl);
		ItemStack item = pl.getInventory().getItemInMainHand();

		return filter != null && filter.check(spellbook.getActiveSpell(item));
	}

}
