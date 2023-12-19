package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class HeldBlockVariable extends MetaVariable {

	@Override
	public String getStringValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			ItemStack mainHandItem = p.getInventory().getItemInMainHand();
			if (mainHandItem != null && mainHandItem.getType().isBlock()) {
				return mainHandItem.getType().name();
			}
		}

		return null;
	}

	@Override
	public double getValue(String player) {
		return 0;
	}

}
