package com.nisovin.magicspells.variables.variabletypes;

import java.util.Map;

import net.coreprotect.CoreProtect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.Variable;

public class PlayerVariable extends Variable {

	private final Map<String, Double> map = new HashMap<>();
	private static final double EPSILON = 1e-10;

	@Override
	public void set(String player, double amount) {
		Double current = map.get(player);

		Player p = PlayerNameUtils.getPlayerExact(player);

		double min = getMinValue(p);
		double max = getMaxValue(p);

		if (amount > max) amount = max;
		else if (amount < min) amount = min;
		map.put(player, amount);

		if (current != null) {
			double change = amount - current;

			if (Math.abs(change) > EPSILON && logInCoreprotect && Bukkit.getPluginManager().isPluginEnabled("CoreProtect"))
				CoreProtect.getInstance().getAPI().logCommand(p, "/ms_var " + name + ((change < 0D) ? " " : " +") + change);
		}

		if (objective == null) return;
		objective.getScore(player).setScore((int) amount);
	}

	@Override
	public double getValue(String player) {
		return map.getOrDefault(player, defaultValue);
	}

	@Override
	public void reset(String player) {
		double current = map.getOrDefault(player, defaultValue);

		Player p = PlayerNameUtils.getPlayerExact(player);

		map.remove(player);

		double change = defaultValue - current;
		if (Math.abs(change) > EPSILON && logInCoreprotect && Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) CoreProtect.getInstance().getAPI().logCommand(p, "/ms_var " + name + ((change < 0D) ? " " : " +") + change);

		if (objective == null) return;
		objective.getScore(player).setScore((int) defaultValue);
	}

}
