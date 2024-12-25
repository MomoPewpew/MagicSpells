package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class AgeCondition extends Condition {
	
	private boolean passBaby = false;
	private boolean passAdult = false;
	
	@Override
	public boolean initialize(String var) {
		if (var != null) {
			if (var.equalsIgnoreCase("baby")) {
				passBaby = true;
				return true;
			} else if (var.equalsIgnoreCase("adult")) {
				passAdult = true;
				return true;
			}
		}
		passBaby = true;
		passAdult = true;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return age(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return age(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean age(LivingEntity target) {
		if (!(target instanceof Ageable t)) return false;
		boolean adult = t.isAdult();
		return adult ? passAdult : passBaby;
	}

}
