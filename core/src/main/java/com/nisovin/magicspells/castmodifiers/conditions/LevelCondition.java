package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class LevelCondition extends OperatorCondition {

	private int level = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			level = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return level(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return level(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean level(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		if (equals) return pl.getLevel() == level;
		else if (moreThan) return pl.getLevel() > level;
		else if (lessThan) return pl.getLevel() < level;
		return false;
	}

}
