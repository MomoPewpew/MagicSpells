package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class UnderWaterCondition extends Condition {
	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return data.caster().isUnderWater();
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return data.target().isUnderWater();
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return data.location().getBlock().isLiquid();
	}
}