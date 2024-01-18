package com.nisovin.magicspells.spells.passive;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class DestroyItemListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();

    @Override
    public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			s = s.trim();

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid magic item '" + s + "' in destroyitem trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			items.add(itemData);
		}
    }

    @OverridePriority
	@EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
		if (!isCancelStateOk(event.isCancelled())) return;

        DamageCause cause = event.getCause();
        if (!(cause.equals(DamageCause.FIRE) || cause.equals(DamageCause.FIRE_TICK) || cause.equals(DamageCause.LAVA))) return;
        if (event.getDamage() < item.getHealth()) return;

        Player caster = Bukkit.getPlayer(item.getThrower());

        if (caster == null) return;

		if (!hasSpell(caster) || !canTrigger(caster)) return;

		if (!items.isEmpty()) {
			ItemStack itemStack = item.getItemStack();
			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(itemStack);
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
