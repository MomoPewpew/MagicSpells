package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class RainingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return isRaining(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return isRaining(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return isRaining(data.location());
	}

	private boolean isRaining(Location location) {
		return location.getWorld().hasStorm();
	}

}
