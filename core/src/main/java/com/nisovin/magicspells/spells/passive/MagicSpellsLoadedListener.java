package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class MagicSpellsLoadedListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onLoaded(MagicSpellsLoadedEvent e) {
		Util.forEachLivingInLoadedChunks(livingEntity -> {
			if (hasSpell(livingEntity) && canTrigger(livingEntity)) passiveSpell.activate(livingEntity);
		});
	}

}
