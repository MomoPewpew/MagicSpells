package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class QuitListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		passiveSpell.activate(new SpellData(caster));
	}

}
