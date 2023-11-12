package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.AsyncChatEvent;

import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class ChatListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(final AsyncChatEvent event) {
		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		passiveSpell.activate(caster);
	}

}
