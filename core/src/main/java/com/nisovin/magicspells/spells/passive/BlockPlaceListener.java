package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of blocks to accept
public class BlockPlaceListener extends PassiveListener {

	private final EnumSet<Material> materials = EnumSet.noneOf(Material.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			Material m = Util.getMaterial(s);
			if (m == null) continue;
			materials.add(m);
		}
	}

	@OverridePriority
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player player = event.getPlayer();
		if (!hasSpell(player) || !canTrigger(player)) return;

		Block block = event.getBlock();
		if (!materials.isEmpty() && !materials.contains(block.getType())) return;

		boolean casted = passiveSpell.activate(new SpellData(player, block.getLocation().add(0.5, 0.5, 0.5)));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
