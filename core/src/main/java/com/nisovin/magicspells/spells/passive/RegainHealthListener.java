package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class RegainHealthListener extends PassiveListener {

	private final EnumSet<RegainReason> reasons = EnumSet.noneOf(RegainReason.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] data = var.split(",");
		for (String datum : data) {
			try {
				reasons.add(RegainReason.valueOf(datum.toUpperCase()));
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid health regain reason '" + datum + "' in regainhealth trigger on passive spell '" + passiveSpell.getName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onRegainHealth(EntityRegainHealthEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity caster)) return;
		if (!canTrigger(caster) || !hasSpell(caster)) return;
		if (!reasons.isEmpty() && !reasons.contains(event.getRegainReason())) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
