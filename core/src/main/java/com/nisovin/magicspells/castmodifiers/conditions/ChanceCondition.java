package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;

import com.nisovin.magicspells.util.SpellData;
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
	public boolean checkCaster(SpellData data) {
		return chance(data);
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return chance(data);
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return chance(data);
	}

	private boolean chance(SpellData data) {
		double c = chance.get(data) / 100;
		return c >= 0 && (c == 1 || ThreadLocalRandom.current().nextDouble() < c);
	}

}