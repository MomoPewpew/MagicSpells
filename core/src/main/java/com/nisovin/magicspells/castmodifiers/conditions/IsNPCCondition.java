package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.util.SpellData;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class IsNPCCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return isNPC(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return isNPC(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isNPC(LivingEntity target) {
		return target.hasMetadata("NPC");
	}

}
