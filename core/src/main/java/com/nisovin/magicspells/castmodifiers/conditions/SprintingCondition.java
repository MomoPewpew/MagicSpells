package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class SprintingCondition extends Condition {
	
	@Override
	public boolean initialize(String var) {
		return true;
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return isSprinting(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return isSprinting(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isSprinting(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		return pl.isSprinting();
	}
	
}
