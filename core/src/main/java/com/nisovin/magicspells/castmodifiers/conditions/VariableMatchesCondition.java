package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class VariableMatchesCondition extends Condition {

	private String variable;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		variable = var;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return variableMatches(data.caster(), null);
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return variableMatches(data.caster(), data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean variableMatches(LivingEntity caster, LivingEntity target) {
		if (!(caster instanceof Player pl)) return false;
		String name = null;
		if (target instanceof Player t) name = t.getName();
		// Check against normal (default)
		return Objects.equals(
				MagicSpells.getVariableManager().getStringValue(variable, pl),
				MagicSpells.getVariableManager().getStringValue(variable, name)
		);
	}

}
