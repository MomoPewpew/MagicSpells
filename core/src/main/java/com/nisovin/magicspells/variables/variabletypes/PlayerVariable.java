package com.nisovin.magicspells.variables.variabletypes;

import java.util.Map;

import org.bukkit.entity.Player;

import java.util.HashMap;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.Variable;

public class PlayerVariable extends Variable {

	private final Map<String, Double> map = new HashMap<>();

	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);

		double min = getMinValue(p);
		double max = getMaxValue(p);

		if (amount > max) amount = max;
		else if (amount < min) amount = min;
		map.put(player, amount);
		if (objective == null) return;
		objective.getScore(player).setScore((int) amount);
	}

	@Override
	public double getValue(String player) {
		return map.getOrDefault(player, defaultValue);
	}

	@Override
	public void reset(String player) {
		map.remove(player);
		if (objective == null) return;
		objective.getScore(player).setScore((int) defaultValue);
	}

}
