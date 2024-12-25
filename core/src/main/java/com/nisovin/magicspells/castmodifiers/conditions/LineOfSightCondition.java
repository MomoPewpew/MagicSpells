package com.nisovin.magicspells.castmodifiers.conditions;


import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class LineOfSightCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return false;
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null || data.caster() == null) return false;
		return data.caster().hasLineOfSight(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null || data.caster() == null) return false;
		return data.caster().hasLineOfSight(data.location());
	}

}
