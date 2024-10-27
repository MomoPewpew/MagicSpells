package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class WorldChangeListener extends PassiveListener {

	private final Set<String> worldNames = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		for (String worldName : var.split(",")) {
			World world = Bukkit.getWorld(worldName);
			if (world == null) {
				MagicSpells.error("Invalid world '" + worldName + "' in worldchange trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			worldNames.add(worldName);
		}
	}

	@OverridePriority
	@EventHandler
	public void onWorldChange(PlayerTeleportEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		World worldFrom = event.getFrom().getWorld();
		if (worldFrom == null) return;

		World worldTo = event.getTo().getWorld();
		if (worldTo == null || worldFrom.equals(worldTo)) return;

		if (!worldNames.isEmpty() && !worldNames.contains(worldTo.getName())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(new SpellData(event.getPlayer()));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
