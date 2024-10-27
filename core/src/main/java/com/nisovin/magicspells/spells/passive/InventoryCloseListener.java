package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable that may contain a comma separated list of inventory names to trigger on
public class InventoryCloseListener extends PassiveListener {

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
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!inventoryNames.contains(Util.getStringFromComponent(event.getView().title()))) return;

		HumanEntity caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		passiveSpell.activate(new SpellData(caster));
	}

}
