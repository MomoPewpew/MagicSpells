package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.VelocitySpell;
import com.nisovin.magicspells.util.SpellData;

public class VelocityActiveCondition extends Condition {

	private VelocitySpell velocitySpell;

	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (!(spell instanceof VelocitySpell)) return false;
		velocitySpell = (VelocitySpell) spell;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return isJumping(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return isJumping(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isJumping(LivingEntity target) {
		return velocitySpell.isJumping(target);
	}

}
