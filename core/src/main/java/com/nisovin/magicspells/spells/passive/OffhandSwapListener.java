package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class OffhandSwapListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onSwap(PlayerSwapHandItemsEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
