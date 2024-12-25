package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class StormCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return stormy(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return stormy(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return stormy(data.location());
	}

	private boolean stormy(Location location) {
		return location.getWorld().hasStorm() || location.getWorld().isThundering();
	}

}
