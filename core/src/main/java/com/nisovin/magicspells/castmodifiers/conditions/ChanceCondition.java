package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.castmodifiers.Condition;

public class ChanceCondition extends Condition {

	private ConfigData<Double> chance;

	@Override
	public boolean initialize(String var) {
		chance = ConfigDataUtil.getDouble(var);
		return chance != null;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return chance(caster, null);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return chance(caster, target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return chance(caster, null);
	}

	private boolean chance(LivingEntity caster, LivingEntity target) {
		double c = chance.get(caster, target, 1F, null) / 100;
		return c >= 0 && (c == 1 || ThreadLocalRandom.current().nextDouble() < c);
	}

}