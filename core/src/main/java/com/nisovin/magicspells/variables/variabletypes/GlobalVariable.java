package com.nisovin.magicspells.variables.variabletypes;

import com.nisovin.magicspells.variables.Variable;

public class GlobalVariable extends Variable {

	private double value = 0;
	
	@Override
	protected void init() {
		value = defaultValue;
	}

	@Override
	public void set(String player, double amount) {
		double min = getMinValue(null);
		double max = getMaxValue(null);

		if (amount > max) amount = max;
		else if (amount < min) amount = min;
		value = amount;
	}

	@Override
	public double getValue(String player) {
		return value;
	}

	@Override
	public void reset(String player) {
		value = defaultValue;
	}

}
