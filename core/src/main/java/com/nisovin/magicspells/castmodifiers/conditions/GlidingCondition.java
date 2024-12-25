package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class GlidingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return isGliding(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return isGliding(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isGliding(LivingEntity target) {
		return target.isGliding();
	}

}
