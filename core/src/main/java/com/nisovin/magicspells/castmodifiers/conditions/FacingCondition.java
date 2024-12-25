package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class FacingCondition extends Condition {

	private String direction;
	
	@Override
	public boolean initialize(String var) {
		direction = var;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return getDirection(data.caster().getLocation()).equals(direction);
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return getDirection(data.target().getLocation()).equals(direction);
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}
	
	private String getDirection(Location loc) {
        float y = loc.getYaw();
        if (y < 0) y += 360;
        y %= 360;
        if (y <= 45 || y >= 315) return "south";
        if (y >= 45 && y <= 135) return "west";
        if (y >= 135 && y <= 225) return "north";
		return "east";
   }

}
