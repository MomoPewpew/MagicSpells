package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class DistanceCondition extends OperatorCondition {

	private double distanceSq;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			distanceSq = Double.parseDouble(var.substring(1));
			distanceSq = distanceSq * distanceSq;
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return false;
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return distance(data.caster().getLocation(), data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return distance(data.caster().getLocation(), data.location());
	}

	private boolean distance(Location from, Location to) {
		if (from == null || to == null) return false;
		if (!from.getWorld().equals(to.getWorld())) return false;

		if (equals) return from.distanceSquared(to) == distanceSq;
		else if (moreThan) return from.distanceSquared(to) > distanceSq;
		else if (lessThan) return from.distanceSquared(to) < distanceSq;
		return false;
	}

}
