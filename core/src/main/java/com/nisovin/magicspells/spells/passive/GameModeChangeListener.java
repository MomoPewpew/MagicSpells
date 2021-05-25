package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;

public class GameModeChangeListener extends PassiveListener {

	Map<GameMode, List<PassiveSpell>> spellMap = new HashMap<>();

	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) return;

		GameMode gameMode;
		try {
			gameMode = GameMode.valueOf(var.toUpperCase());
			List<PassiveSpell> list = spellMap.computeIfAbsent(gameMode, c -> new ArrayList<>());
			list.add(spell);
		} catch (Exception e) {
			// ignored
		}
	}

	@OverridePriority
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		Player p = event.getPlayer();
		List<PassiveSpell> spells = spellMap.get(event.getNewGameMode());
		Spellbook spellbook = MagicSpells.getSpellbook(p);
		for (PassiveSpell spell : spells) {
			if (!isCancelStateOk(spell, event.isCancelled())) continue;
			if (!spellbook.hasSpell(spell)) continue;
			boolean casted = spell.activate(p);
			if (PassiveListener.cancelDefaultAction(spell, casted)) event.setCancelled(true);
		}
	}

}