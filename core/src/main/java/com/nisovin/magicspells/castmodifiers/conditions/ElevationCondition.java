package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class ElevationCondition extends OperatorCondition {

	private double y;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			y = Double.parseDouble(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return elevation(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return elevation(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return elevation(data.location());
	}

	private boolean elevation(Location location) {
		if (equals) return location.getY() == y;
		else if (moreThan) return location.getY() > y;
		else if (lessThan) return location.getY() < y;
		return false;
	}

}
