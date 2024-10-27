package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import com.nisovin.magicspells.util.SpellData;
import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerEditBookEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable is optional
// If not specified, it will trigger on any book
// If specified, it should be a comma separated list of page text to trigger on
public class SignBookListener extends PassiveListener {

	private final Set<String> text = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		text.addAll(Arrays.asList(split));
	}

	@OverridePriority
	@EventHandler
	public void onBookEdit(PlayerEditBookEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!event.isSigning()) return;

		Player player = event.getPlayer();
		if (!hasSpell(player) || !canTrigger(player)) return;

		if (text.isEmpty()) {
			boolean casted = passiveSpell.activate(new SpellData(player));
			if (cancelDefaultAction(casted)) event.setCancelled(true);
			return;
		}

		for (Component page : event.getNewBookMeta().pages()) {
			if (!text.contains(Util.getStringFromComponent(page))) continue;
			boolean casted = passiveSpell.activate(new SpellData(player));
			if (cancelDefaultAction(casted)) event.setCancelled(true);
			return;
		}
	}

}
