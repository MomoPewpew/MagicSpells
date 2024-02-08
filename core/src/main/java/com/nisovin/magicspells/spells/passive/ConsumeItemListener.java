package com.nisovin.magicspells.spells.passive;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class ConsumeItemListener extends PassiveListener{

	private final Set<MagicItemData> items = new HashSet<>();

    @Override
    public void initialize(String var) {

        if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			s = s.trim();

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid magic item '" + s + "' in consumeitem trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			items.add(itemData);
		}
	}
    
    @OverridePriority
	@EventHandler
    public void onConsume(final PlayerItemConsumeEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

        Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

        if (!items.isEmpty()) {
			ItemStack item = event.getItem();
			if (item == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
			if (itemData == null || !contains(itemData)) return;
		}

        boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

    private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.matches(itemData)) return true;
		}
		return false;
	}

}
