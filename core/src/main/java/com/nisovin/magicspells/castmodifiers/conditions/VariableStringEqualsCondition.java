package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class VariableStringEqualsCondition extends Condition {

	private String variable;
	private String value;
	private boolean isVariable;

	@Override
	public boolean initialize(String var) {
		String[] split = var.split(":",2);

		//Were two parts of this modifier created?
		if (split.length != 2) return false;

		variable = split[0]; //The variable that is being checked
		value = split[1]; //The value that the variable is being checked for

		//Variable cannot be null or empty.
		if (variable.isEmpty()) {
			MagicSpells.error("No variable stated for comparison within this modifier!");
			return false;
		}

		isVariable = MagicSpells.getVariableManager().getVariables().containsKey(value);
		//Translates "null" string to empty.
		if (!isVariable && value.equals("null")) value = "";

		//If everything checks out, will continue.
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return isEqualFor(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return isEqualFor(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean isEqualFor(LivingEntity target) {
		if (!(target instanceof Player player)) return false;
		return Objects.equals(
				MagicSpells.getVariableManager().getStringValue(variable, player),
				isVariable ? MagicSpells.getVariableManager().getStringValue(value, player) : value
		);
	}

}
