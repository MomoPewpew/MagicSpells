package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class AbsorptionCondition extends OperatorCondition {

	private float health = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			health = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return absorption(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return absorption(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean absorption(LivingEntity target) {
		if (equals) return target.getAbsorptionAmount() == health;
		else if (moreThan) return target.getAbsorptionAmount() > health;
		else if (lessThan) return target.getAbsorptionAmount() < health;
		return false;
	}
	
}
