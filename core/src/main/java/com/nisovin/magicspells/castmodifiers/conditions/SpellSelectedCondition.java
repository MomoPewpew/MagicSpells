package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
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
	public boolean check(LivingEntity caster) {
		return spellSelected(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return spellSelected(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return spellSelected(caster);
	}

	private boolean spellSelected(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		Spellbook spellbook = MagicSpells.getSpellbook(pl);
		ItemStack item = pl.getInventory().getItemInMainHand();

		return filter != null && filter.check(spellbook.getActiveSpell(item));
	}

}
