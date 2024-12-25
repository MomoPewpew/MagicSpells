package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class OxygenCondition extends OperatorCondition {
	
	private int oxygen;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			oxygen = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return oxygen(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return oxygen(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean oxygen(LivingEntity target) {
		if (equals) return target.getRemainingAir() == oxygen;
		else if (moreThan) return target.getRemainingAir() > oxygen;
		else if (lessThan) return target.getRemainingAir() < oxygen;
		return false;
	}
	
}

