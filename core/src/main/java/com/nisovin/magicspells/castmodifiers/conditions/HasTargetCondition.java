package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class HasTargetCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return hasTarget(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return hasTarget(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean hasTarget(LivingEntity target) {
		if (!(target instanceof Creature creature)) return false;
		LivingEntity t = creature.getTarget();
		return t != null && t.isValid();
	}

}
