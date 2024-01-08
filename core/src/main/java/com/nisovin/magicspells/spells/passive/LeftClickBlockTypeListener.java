package com.nisovin.magicspells.spells.passive;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;

// Trigger variable accepts a comma separated list of blocks to accept
public class LeftClickBlockTypeListener extends PassiveListener {

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
				MagicSpells.error("Invalid block type on leftclickblocktype trigger '" + var + "' on passive spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onLeftClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		if (!isCancelStateOk(isCancelled(event))) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		Block block = event.getClickedBlock();
		if (block == null) return;

		BlockData blockData = block.getBlockData();

		if (blockData == null) return;

		boolean match = false;
		for (BlockData matches : blockDatas) {
			if (blockData.matches(matches)) match = true;
		}

		if (!blockDatas.isEmpty() && !match) return;
		boolean casted = passiveSpell.activate(event.getPlayer(), block.getLocation().add(0.5, 0.5, 0.5));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean isCancelled(PlayerInteractEvent event) {
		return event.useInteractedBlock() == Event.Result.DENY;
	}

}
