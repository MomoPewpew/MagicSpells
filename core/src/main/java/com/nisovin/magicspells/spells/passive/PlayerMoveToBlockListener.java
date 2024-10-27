package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PlayerMoveToBlockListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(event.getPlayer()) || !canTrigger(caster) || !event.hasChangedBlock()) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
