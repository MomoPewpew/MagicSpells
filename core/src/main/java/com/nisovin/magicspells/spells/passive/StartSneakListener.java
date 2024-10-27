package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class StartSneakListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!event.isSneaking()) return;

		Player caster = event.getPlayer();
		if (!hasSpell(event.getPlayer()) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(new SpellData(event.getPlayer()));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
