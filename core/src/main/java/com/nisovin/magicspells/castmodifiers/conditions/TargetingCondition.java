package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class TargetingCondition extends Condition {

	private Set<EntityType> allowedTypes;
	private boolean anyType = false;
	private boolean targetingCaster = false;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) {
			anyType = true;
			return true;
		}
		if (var.equalsIgnoreCase("caster")) {
			targetingCaster = true;
			return true;
		}
		
		String[] entityTypes = var.split(",");
		allowedTypes = new HashSet<>();
		for (String type: entityTypes) {
			EntityType entityType = MobUtil.getEntityType(type);
			if (entityType != null) allowedTypes.add(entityType);
		}
		return !allowedTypes.isEmpty();
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return false;
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return targeting(data.caster(), data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean targeting(LivingEntity caster, LivingEntity target) {
		if (!(target instanceof Creature c)) return false;
		LivingEntity creatureTarget = c.getTarget();
		if (creatureTarget != null) {
			if (anyType) return true;
			if (targetingCaster && creatureTarget.equals(caster)) return true;
			if (allowedTypes.contains(creatureTarget.getType())) return true;
		}
		return false;
	}

}
