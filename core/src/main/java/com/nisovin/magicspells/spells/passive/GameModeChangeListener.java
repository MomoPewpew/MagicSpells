package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class GameModeChangeListener extends PassiveListener {

	private final EnumSet<GameMode> gameModes = EnumSet.noneOf(GameMode.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();

			try {
				GameMode mode = GameMode.valueOf(s.toUpperCase());
				gameModes.add(mode);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid game mode '" + s + "' in gamemodechange trigger on passive spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		if (!gameModes.isEmpty() && !gameModes.contains(event.getNewGameMode())) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
