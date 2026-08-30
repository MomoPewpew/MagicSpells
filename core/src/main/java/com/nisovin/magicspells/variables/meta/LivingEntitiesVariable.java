package com.nisovin.magicspells.variables.meta;

import org.bukkit.World;
import org.bukkit.entity.Player;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.listeners.LivingEntityCounter;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class LivingEntitiesVariable extends MetaVariable {

	@Override
	public String getStringValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return String.valueOf(getLivingEntityCount(p.getWorld()));
		return null;
	}

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return getLivingEntityCount(p.getWorld());
		return 0;
	}

	private static int getLivingEntityCount(World world) {
		return LivingEntityCounter.getCount(world);
	}

}
