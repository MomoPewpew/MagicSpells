package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.SpellCastResult;

public class ConsumeListener implements Listener {

	private Map<CastItem, Spell> consumeCastItems = new HashMap<>();
	private Map<String, Long> lastCast = new HashMap<>();
	
	public ConsumeListener() {
		for (Spell spell : MagicSpells.getSpells().values()) {
			CastItem[] items = spell.getConsumeCastItems();
			if (items.length <= 0) continue;
			for (CastItem item : items) {
				Spell old = consumeCastItems.put(item, spell);
				if (old == null) continue;
				MagicSpells.error("The spell '" + spell.getInternalName() + "' has same consume-cast-item as '" + old.getInternalName() + "'!");
			}
		}
	}
	
	public boolean hasConsumeCastItems() {
		return !consumeCastItems.isEmpty();
	}
	
	@EventHandler
	public void onConsume(final PlayerItemConsumeEvent event) {
		ItemStack item = event.getItem();
		CastItem castItem = new CastItem(item);
		final Spell spell = consumeCastItems.get(castItem);
		if (spell == null) return;

		Player player = event.getPlayer();
		Long lastCastTime = lastCast.get(player.getName());
		if (lastCastTime != null && lastCastTime + MagicSpells.getGlobalCooldown() > System.currentTimeMillis()) return;
		lastCast.put(player.getName(), System.currentTimeMillis());

		if (MagicSpells.getSpellbook(player).canCast(spell)) {
			PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
			String[] args = null;
			if (container.has(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING)) {
				args = new String[] {container.get(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING)};
			}

			SpellCastResult result = spell.cast(player, 1.0F, args);
			if (result.state != SpellCastState.NORMAL) event.setCancelled(true);
		}
	}

}
