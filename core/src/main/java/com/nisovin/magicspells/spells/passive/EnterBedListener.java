package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedEnterEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class EnterBedListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onDeath(PlayerBedEnterEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player player = event.getPlayer();
		if (!hasSpell(player) || !canTrigger(player)) return;

		boolean casted = passiveSpell.activate(new SpellData(player));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
