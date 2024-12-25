package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class PermissionCondition extends Condition {

	private String perm;

	@Override
	public boolean initialize(String var) {
		perm = var;
		return true;
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return hasPermission(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return hasPermission(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean hasPermission(LivingEntity target) {
		return target.hasPermission(perm);
	}

}
