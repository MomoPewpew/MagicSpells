package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellFilter;

public class CastingCondition extends Condition {
	
	private SpellFilter filter;
	
	@Override
	public boolean initialize(String var) {
		if (var != null && !var.isEmpty()) filter = SpellFilter.fromString(var);
		return true;
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return casting(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return casting(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean casting(LivingEntity target) {
		if (filter == null) return MagicSpells.plugin.delayedSpellCasts.keySet().contains(target.getUniqueId());
		else return MagicSpells.plugin.delayedSpellCasts.get(target.getUniqueId()) != null && filter.check(MagicSpells.plugin.delayedSpellCasts.get(target.getUniqueId()).spellCast.getSpell());
	}
	
}
