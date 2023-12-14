package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class ChanceCondition extends Condition {

	private double chance;
	private Variable chanceVariable;

	@Override
	public boolean initialize(String var) {
		try {
			chance = Double.parseDouble(var) / 100D;
			chanceVariable = null;
			return chance >= 0D && chance <= 1D;
		} catch (NumberFormatException e) {
			chanceVariable = MagicSpells.getVariableManager().getVariable(var);
			if (chanceVariable == null) {
				DebugHandler.debugNumberFormat(e);
				return false;
			} else {
				chance = 0;
				return true;
			}
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return chance(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return chance(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return chance(caster);
	}

	private boolean chance(LivingEntity caster) {
		if (chanceVariable != null) {
			if (caster == null) chance = chanceVariable.getValue((Player) null) / 100D;
			else if (caster instanceof Player player) chance = chanceVariable.getValue(player) / 100D;
		}
		return chance != 0 && (chance == 1 || ThreadLocalRandom.current().nextDouble() < chance);
	}

}
