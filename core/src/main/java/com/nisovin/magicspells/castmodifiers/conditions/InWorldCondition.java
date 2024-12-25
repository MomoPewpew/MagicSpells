package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class InWorldCondition extends Condition {

	private String world = "";

	@Override
	public boolean initialize(String var) {
		world = var;
		return true;
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return checkWorld(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return checkWorld(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return checkWorld(data.location());
	}

	private boolean checkWorld(Location location) {
		return location.getWorld().getName().equalsIgnoreCase(world);
	}

}
