package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class VariableCondition extends OperatorCondition {

	private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[0-9a-zA-Z_]+");

	private String variable;
	private String variableCompared;
	private double value = 0;

	@Override
	public boolean initialize(String var) {
		Matcher matcher = VARIABLE_NAME_PATTERN.matcher(var);
		String variableName;

		if (matcher.find()) variableName = matcher.group();
		else return false;

		String number = var.substring(variableName.length());
		if (number.length() < 2 || !super.initialize(number)) return false;

		variable = variableName;

		try {
			value = Double.parseDouble(number.substring(1));
			return true;
		} catch (NumberFormatException e) {
			variableCompared = number.substring(1);
			if (MagicSpells.getVariableManager().getVariable(variableCompared) != null) return true;
			else variableCompared = null;

			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return variableType(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return variableType(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return variableType(location);
	}

	private boolean variableType(LivingEntity target) {
		String targetName = target instanceof Player ? target.getName() : target.getName();
		if (variableCompared != null) return variable(target, MagicSpells.getVariableManager().getValue(variableCompared, targetName));
		return variable(target, value);
	}

	private boolean variableType(Location location) {
		if (variableCompared != null) return variable(location, MagicSpells.getVariableManager().getValueAtLocation(variableCompared, location));
		return variable(location, value);
	}

	private boolean variable(Object target, double v) {
		double val;
		if (target instanceof Player pl) val = MagicSpells.getVariableManager().getValue(variable, pl);
		else if (target instanceof Location loc) val = MagicSpells.getVariableManager().getValueAtLocation(variable, loc);
		else if (target instanceof LivingEntity le) val = MagicSpells.getVariableManager().getValue(variable, le.getName());
		else return false;
		
		if (equals) return val == v;
		if (moreThan) return val > v;
		if (lessThan) return val < v;
		return false;
	}

}
