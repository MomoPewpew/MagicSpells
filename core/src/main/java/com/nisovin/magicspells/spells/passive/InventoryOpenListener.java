package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable that may contain a comma separated list of inventory names to trigger on
public class InventoryOpenListener extends PassiveListener {

	private final Set<String> inventoryNames = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			inventoryNames.add(s.trim());
		}
	}

	@OverridePriority
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		boolean inventoryMatches = false;

		if (event.getInventory().getHolder() instanceof ChestedHorse chestedHorse) {
			if (chestedHorse instanceof Donkey) {
				inventoryMatches = inventoryNames.contains("DONKEY");
			}
			if (chestedHorse instanceof Llama) {
				inventoryMatches = inventoryNames.contains("LLAMA");
			}
			if (chestedHorse instanceof Mule) {
				inventoryMatches = inventoryNames.contains("MULE");
			}
			if (chestedHorse instanceof TraderLlama) {
				inventoryMatches = inventoryNames.contains("TRADER_LLAMA");
			}
		}

		if (!inventoryNames.contains(Util.getStringFromComponent(event.getView().title())) && !inventoryMatches) return;

		HumanEntity caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
