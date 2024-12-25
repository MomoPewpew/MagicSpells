package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class LightLevelCondition extends OperatorCondition {

	private byte level = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			level = Byte.parseByte(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return lightLevel(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return lightLevel(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return lightLevel(data.location());
	}

	private boolean lightLevel(Location location) {
		if (equals) return location.getBlock().getLightLevel() == level;
		else if (moreThan) return location.getBlock().getLightLevel() > level;
		else if (lessThan) return location.getBlock().getLightLevel() < level;
		return false;
	}

}
