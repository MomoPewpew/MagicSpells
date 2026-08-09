package com.nisovin.magicspells.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.managers.AlteredBlockManager;

public class AlteredBlockListener implements Listener {

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		AlteredBlockManager manager = MagicSpells.getAlteredBlockManager();
		if (manager == null) return;

		for (Entity entity : event.getChunk().getEntities()) {
			if (!entity.getScoreboardTags().contains(MagicSpells.ALTERED_BLOCK_TAG)) continue;
			if (manager.isTracked(entity.getLocation().getBlock())) continue;
			manager.restoreFromMarker(entity);
		}
	}

}
