package com.nisovin.magicspells.spells.passive;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;

// Optional trigger variable of a comma separated list of blocks to accept
public class BlockBreakListener extends PassiveListener {

	private final Set<BlockData> blockDatas = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			try {
				BlockData m = Bukkit.createBlockData(s.toLowerCase());
				blockDatas.add(m);
			} catch (Exception e) {
				MagicSpells.error("Invalid block type on blockbreak trigger '" + var + "' on passive spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player player = event.getPlayer();
		if (!hasSpell(player) || !canTrigger(player)) return;

		Block block = event.getBlock();

		if (block == null) return;

		BlockData blockData = block.getBlockData();

		if (blockData == null) return;

		boolean match = false;
		for (BlockData matches : blockDatas) {
			if (blockData.matches(matches)) match = true;
		}

		if (!blockDatas.isEmpty() && !match) return;

		boolean casted = passiveSpell.activate(player, block.getLocation().add(0.5, 0.5, 0.5));
		if (cancelDefaultAction(casted)) event.setCancelled(true);

	}

}
