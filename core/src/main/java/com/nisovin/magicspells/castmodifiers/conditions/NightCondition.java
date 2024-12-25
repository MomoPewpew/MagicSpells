package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class NightCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return night(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return night(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return night(data.location());
	}

	private boolean night(Location location) {
		long time = location.getWorld().getTime();
		return time > 13000 && time < 23000;
	}
	
}
