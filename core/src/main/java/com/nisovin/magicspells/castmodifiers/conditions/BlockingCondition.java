package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class BlockingCondition extends Condition {
	
	@Override
	public boolean initialize(String var) {
		return true;
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return blocking(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return blocking(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean blocking(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		return pl.isBlocking();
	}
	
}
