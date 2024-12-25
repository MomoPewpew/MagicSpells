package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.LeapSpell;
import com.nisovin.magicspells.util.SpellData;

public class LeapingCondition extends Condition {

	private LeapSpell leapSpell;
	
	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (!(spell instanceof LeapSpell)) return false;
		leapSpell = (LeapSpell) spell;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return isLeaping(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return isLeaping(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isLeaping(LivingEntity target) {
		return leapSpell.isJumping(target);
	}

}
