package com.nisovin.magicspells.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;

public class MagicPlayerListener implements Listener {
	
	private final MagicSpells plugin;
	
	public MagicPlayerListener(MagicSpells plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		// Setup spell book
		Spellbook spellbook = new Spellbook(player);
		MagicSpells.getSpellbooks().put(player.getName(), spellbook);
		
		// Setup mana bar
		if (MagicSpells.getManaHandler() != null) MagicSpells.getManaHandler().createManaBar(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Spellbook spellbook = MagicSpells.getSpellbooks().remove(event.getPlayer().getName());
		if (spellbook != null) spellbook.destroy();
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		if (!MagicSpells.arePlayerSpellsSeparatedPerWorld()) return;
		Player player = event.getPlayer();
		MagicSpells.debug("Player '" + player.getName() + "' changed from world '" + event.getFrom().getName() + "' to '" + player.getWorld().getName() + "', reloading spells");
		MagicSpells.getSpellbook(player).reload();
	}
	
}
