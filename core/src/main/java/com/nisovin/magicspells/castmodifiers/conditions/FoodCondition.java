package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class FoodCondition extends OperatorCondition {

	private int food = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			food = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return food(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return food(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean food(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		if (equals) return pl.getFoodLevel() == food;
		else if (moreThan) return pl.getFoodLevel() > food;
		else if (lessThan) return pl.getFoodLevel() < food;
		return false;
	}

}
