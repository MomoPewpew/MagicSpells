package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class OutsideCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return outside(data.caster(), null);
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return outside(data.target(), null);
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return outside(data.target(), data.location());
	}

	private boolean outside(LivingEntity target, Location location) {
		if (location != null) return location.getWorld().getHighestBlockYAt(location) <= location.getY();
		return target.getWorld().getHighestBlockYAt(target.getLocation()) <= target.getEyeLocation().getY();
	}

}
