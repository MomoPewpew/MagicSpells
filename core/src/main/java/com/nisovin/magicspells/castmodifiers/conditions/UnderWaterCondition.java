package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class UnderWaterCondition extends Condition {
	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return caster.isUnderWater();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return target.isUnderWater();
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}
}