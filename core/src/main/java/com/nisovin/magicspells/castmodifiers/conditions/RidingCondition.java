package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class RidingCondition extends Condition {

	private EntityType entityType;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return true;
		entityType = MobUtil.getEntityType(var);
		return entityType != null;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return isRiding(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return isRiding(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isRiding(LivingEntity target) {
		Entity vehicle = target.getVehicle();
		if (vehicle == null) return false;
		return entityType == null || vehicle.getType() == entityType;
	}

}
