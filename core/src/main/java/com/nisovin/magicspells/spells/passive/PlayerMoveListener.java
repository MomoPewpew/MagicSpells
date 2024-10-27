package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PlayerMoveListener extends PassiveListener {

	private double tolerance = -1;

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		try {
			tolerance = Double.parseDouble(var);
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
		}
	}

	@OverridePriority
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(event.getPlayer()) || !canTrigger(caster)) return;
		if (tolerance >= 0 && LocationUtil.distanceLessThan(event.getFrom(), event.getTo(), tolerance)) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
