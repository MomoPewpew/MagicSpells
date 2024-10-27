package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of teleport causes to accept
public class TeleportListener extends PassiveListener {

	private final EnumSet<TeleportCause> teleportCauses = EnumSet.noneOf(TeleportCause.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			try {
				TeleportCause cause = TeleportCause.valueOf(s.trim().toUpperCase());
				teleportCauses.add(cause);
			} catch (IllegalArgumentException e) {
				// Keep to support old usage
				String compat = s.replace("_", "");
				boolean found = false;

				for (TeleportCause cause : TeleportCause.values()) {
					if (!cause.name().replace("_", "").equals(compat)) continue;
					teleportCauses.add(cause);
					found = true;
					break;
				}

				if (!found)
					MagicSpells.error("Invalid teleport cause '" + s + "' in teleport trigger on passive spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		if (!teleportCauses.isEmpty() && !teleportCauses.contains(event.getCause())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
