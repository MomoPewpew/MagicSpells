package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
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
	public boolean checkCaster(SpellData data) {
		return casting(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return casting(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean casting(LivingEntity target) {
		if (filter == null) return MagicSpells.plugin.delayedSpellCasts.keySet().contains(target.getUniqueId());
		else return MagicSpells.plugin.delayedSpellCasts.get(target.getUniqueId()) != null && filter.check(MagicSpells.plugin.delayedSpellCasts.get(target.getUniqueId()).spellCast.getSpell());
	}
	
}
