package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class PitchCondition extends OperatorCondition {

	private float pitch;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			pitch = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return pitch(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return pitch(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return pitch(data.location());
	}

	private boolean pitch(Location location) {
		if (equals) return location.getPitch() == pitch;
		else if (moreThan) return location.getPitch() > pitch;
		else if (lessThan) return location.getPitch() < pitch;
		return false;
	}

}
