package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.VelocitySpell;

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
	public boolean check(LivingEntity caster) {
		return isJumping(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isJumping(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isJumping(LivingEntity target) {
		return velocitySpell.isJumping(target);
	}

}
