package com.nisovin.magicspells.variables.variabletypes;

import com.nisovin.magicspells.variables.Variable;
import net.coreprotect.CoreProtect;
import org.bukkit.Bukkit;

public class GlobalVariable extends Variable {

	private double value = 0;
	private static final double EPSILON = 1e-10;
	
	@Override
	protected void init() {
		value = defaultValue;
	}

	@Override
	public void set(String player, double amount) {
		double current = value;

		double min = getMinValue(null);
		double max = getMaxValue(null);

		if (amount > max) amount = max;
		else if (amount < min) amount = min;
		value = amount;

		double change = amount - current;
		if (Math.abs(change) > EPSILON && logInCoreprotect && Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) CoreProtect.getInstance().getAPI().logCommand(null, "/ms_var " + name + ((change < 0D) ? " " : " +") + change);
	}

	@Override
	public double getValue(String player) {
		return value;
	}

	@Override
	public void reset(String player) {
		double current = value;

		value = defaultValue;

		double change = defaultValue - current;
		if (Math.abs(change) > EPSILON && logInCoreprotect && Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) CoreProtect.getInstance().getAPI().logCommand(null, "/ms_var " + name + ((change < 0D) ? " " : " +") + change);
	}

}
