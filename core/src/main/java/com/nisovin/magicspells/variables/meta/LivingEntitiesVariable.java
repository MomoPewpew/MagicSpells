package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class LivingEntitiesVariable extends MetaVariable {

	@Override
	public String getStringValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return String.valueOf(p.getLocation().getWorld().getLivingEntities().size());
		return null;
	}

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getLocation().getWorld().getLivingEntities().size();
		return 0;
	}

}
