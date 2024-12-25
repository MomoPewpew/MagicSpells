package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class HealthCondition extends OperatorCondition {

	private int health = 0;
	private boolean percent = false;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		String number = var.substring(1);

		try {
			if (number.endsWith("%")) {
				percent = true;
				number = number.replace("%", "");
			}
			health = Integer.parseInt(number);
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return health(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return health(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean health(LivingEntity target) {
		double hp = target.getHealth();
		double percentHp = hp / Util.getMaxHealth(target) * 100D;
		if (equals) {
			if (percent) return percentHp == health;
			return hp == health;
		} else if (moreThan) {
			if (percent) return percentHp > health;
			return hp > health;
		} else if (lessThan) {
			if (percent) return percentHp < health;
			return hp < health;
		}
		return false;
	}

}
