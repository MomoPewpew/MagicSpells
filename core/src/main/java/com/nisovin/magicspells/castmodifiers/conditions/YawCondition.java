package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class YawCondition extends OperatorCondition {
	
	private float yaw;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			yaw = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return yaw(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return yaw(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return yaw(data.location());
	}

	private boolean yaw(Location location) {
		if (equals) return location.getYaw() == yaw;
		else if (moreThan) return location.getYaw() > yaw;
		else if (lessThan) return location.getYaw() < yaw;
		return false;
	}

}
