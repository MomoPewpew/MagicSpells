package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class CustomNameVisibleCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return nameVisible(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return nameVisible(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean nameVisible(LivingEntity target) {
		return target.isCustomNameVisible();
	}

}
