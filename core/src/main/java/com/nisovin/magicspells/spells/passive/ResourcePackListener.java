package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable should be set to one of the following
// loaded,successfully_loaded
// declined
// failed,failed_download
// accepted
public class ResourcePackListener extends PassiveListener {

	private final EnumSet<Status> packStatus = EnumSet.noneOf(Status.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim().toUpperCase();

			switch (s) {
				case "LOADED":
					packStatus.add(Status.SUCCESSFULLY_LOADED);
					break;
				case "FAILED":
					packStatus.add(Status.FAILED_DOWNLOAD);
					break;
				default:
					try {
						Status status = Status.valueOf(s);
						packStatus.add(status);
					} catch (IllegalArgumentException e) {
						MagicSpells.error("Invalid resource pack status '" + s + "' in resourcepack trigger on passive spell '" + passiveSpell.getInternalName() + "'");
					}
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onPlayerResourcePack(PlayerResourcePackStatusEvent event) {
		if (!packStatus.isEmpty() && !packStatus.contains(event.getStatus())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		passiveSpell.activate(new SpellData(event.getPlayer()));
	}

}
