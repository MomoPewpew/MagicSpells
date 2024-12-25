package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class ReceivingRedstoneCondition extends OperatorCondition {
	
	private int level = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			level = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return signal(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return signal(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return signal(data.location());
	}

	private boolean signal(Location location) {
		if (equals) return location.getBlock().getBlockPower() == level;
		else if (moreThan) return location.getBlock().getBlockPower() > level;
		else if (lessThan) return location.getBlock().getBlockPower() < level;
		return false;
	}
	
}
