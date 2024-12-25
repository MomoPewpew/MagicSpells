package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class DayCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return checkTime(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return checkTime(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return checkTime(data.location());
	}

	private boolean checkTime(Location location) {
		long time = location.getWorld().getTime();
		return !(time > 13000 && time < 23000);
	}

}
