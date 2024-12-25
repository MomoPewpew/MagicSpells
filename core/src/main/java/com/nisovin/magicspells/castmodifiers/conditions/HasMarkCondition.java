package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.MarkSpell;
import com.nisovin.magicspells.util.SpellData;

public class HasMarkCondition extends Condition {

	private MarkSpell spell;
	
	@Override
	public boolean initialize(String var) {
		Spell s = MagicSpells.getSpellByInternalName(var);
		if (s == null) return false;
		if (!(s instanceof MarkSpell)) return false;

		spell = (MarkSpell) s;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return hasMark(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return hasMark(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean hasMark(LivingEntity target) {
		return spell.getMarks().containsKey(target.getUniqueId());
	}

}
