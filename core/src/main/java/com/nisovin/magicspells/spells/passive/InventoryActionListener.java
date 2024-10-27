package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class InventoryActionListener extends PassiveListener {

	private final EnumSet<InventoryAction> actions = EnumSet.noneOf(InventoryAction.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.replace(" ", "").split(",");
		for (String s : split) {
			try {
				InventoryAction action = InventoryAction.valueOf(s.toUpperCase());
				actions.add(action);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid inventory action '" + s + "' in inventory trigger on passive spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		if (!actions.isEmpty() && !actions.contains(InventoryAction.OPEN)) return;

		HumanEntity caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	@OverridePriority
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!actions.isEmpty() && !actions.contains(InventoryAction.CLOSE)) return;

		HumanEntity caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		passiveSpell.activate(new SpellData(caster));
	}

	private enum InventoryAction {

		OPEN,
		CLOSE

	}

}
