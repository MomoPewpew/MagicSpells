package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;

public class LeftClickListener implements Listener {

	private final Map<CastItem, Spell> leftClickCastItems = new HashMap<>();
	private final Map<String, Long> lastCast = new HashMap<>();

	public LeftClickListener() {
		for (Spell spell : MagicSpells.getSpells().values()) {
			CastItem[] items = spell.getLeftClickCastItems();
			if (items.length == 0) continue;

			for (CastItem item : items) {
				if (item == null) continue;

				Spell old = leftClickCastItems.put(item, spell);
				if (old != null)
					MagicSpells.error("The spell '" + spell.getInternalName() + "' has same left-click-cast-item as '" + old.getInternalName() + "'!");
			}
		}
	}

	public boolean hasLeftClickCastItems() {
		return !leftClickCastItems.isEmpty();
	}

	@EventHandler
	public void onLeftClick(final PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;

		ItemStack item = event.getItem();
		if (item == null) return;

		final Spell spell = leftClickCastItems.get(new CastItem(item));
		if (spell == null) return;

		Player player = event.getPlayer();
		Spellbook spellbook = MagicSpells.getSpellbook(player);

		if (!spellbook.hasSpell(spell) || !spellbook.canCast(spell)) return;

		if (!spell.isIgnoringGlobalCooldown()) {
			Long lastCastTime = lastCast.get(player.getName());
			if (lastCastTime != null && lastCastTime + MagicSpells.getGlobalCooldown() > System.currentTimeMillis()) return;
			lastCast.put(player.getName(), System.currentTimeMillis());
		}

		PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
		if (container.has(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING)) {
			final String[] args = new String[] {container.get(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING)};
			MagicSpells.scheduleDelayedTask(() -> spell.cast(player, 1.0F, args), 0);
		} else {
			MagicSpells.scheduleDelayedTask(() -> spell.cast(player), 0);
		}
	}

}
