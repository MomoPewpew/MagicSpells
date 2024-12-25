package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class ThunderingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return thundering(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return thundering(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return thundering(data.location());
	}

	private boolean thundering(Location location) {
		return location.getWorld().isThundering();
	}

}
