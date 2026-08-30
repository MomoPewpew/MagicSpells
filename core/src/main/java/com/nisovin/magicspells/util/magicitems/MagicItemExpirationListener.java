package com.nisovin.magicspells.util.magicitems;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.util.magicitems.MagicItemBehaviors.ExpirationResult;

import net.sneakycharactermanager.paper.handlers.character.LoadCharacterEvent;

public class MagicItemExpirationListener implements Listener {

	private static MagicItemExpirationListener instance;

	private CharacterExpirationHandler characterExpirationHandler;

	public MagicItemExpirationListener() {
		instance = this;
		if (CompatBasics.pluginEnabled("SneakyCharacterManager"))
			characterExpirationHandler = new CharacterExpirationHandler();
	}

	public static MagicItemExpirationListener getInstance() {
		return instance;
	}

	public void joinOrLoadCharacter(Player player) {
		PlayerInventory inv = player.getInventory();
		processInventory(inv);
		ItemStack[] armor = inv.getArmorContents();
		processInventoryContents(armor);
		inv.setArmorContents(armor);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onJoin(PlayerJoinEvent event) {
		joinOrLoadCharacter(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onInvOpen(InventoryOpenEvent event) {
		processInventory(event.getInventory());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onClick(PlayerInteractEvent event) {
		if (!event.hasItem())
			return;
		ItemStack item = event.getItem();
		ExpirationResult result = MagicItemBehaviors.updateExpiresLineIfNeeded(item);
		if (result == ExpirationResult.EXPIRED) {
			event.getPlayer().getEquipment().setItemInMainHand(null);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPickup(EntityPickupItemEvent event) {
		processItemDrop(event.getItem());
		if (event.getItem().isDead())
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDrop(PlayerDropItemEvent event) {
		processItemDrop(event.getItemDrop());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onItemSpawn(ItemSpawnEvent event) {
		processItemDrop(event.getEntity());
		if (event.getEntity().isDead())
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onItemSwap(PlayerSwapHandItemsEvent event) {
		ItemStack item = event.getOffHandItem();
		ExpirationResult result = MagicItemBehaviors.updateExpiresLineIfNeeded(item);
		if (result == ExpirationResult.EXPIRED) {
			event.getPlayer().getEquipment().setItemInMainHand(null);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onHotbarScroll(PlayerItemHeldEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
		ExpirationResult result = MagicItemBehaviors.updateExpiresLineIfNeeded(item);
		if (result == ExpirationResult.EXPIRED) {
			event.getPlayer().getInventory().setItem(event.getNewSlot(), null);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		ExpirationResult result = MagicItemBehaviors.updateExpiresLineIfNeeded(item);
		if (result == ExpirationResult.EXPIRED) {
			event.setCurrentItem(null);
			event.setCancelled(true);
		}
	}

	private void processInventory(Inventory inv) {
		ItemStack[] contents = inv.getContents();
		processInventoryContents(contents);
		inv.setContents(contents);
	}

	private void processInventoryContents(ItemStack[] contents) {
		for (int i = 0; i < contents.length; i++) {
			ExpirationResult result = MagicItemBehaviors.updateExpiresLineIfNeeded(contents[i]);
			if (result == ExpirationResult.EXPIRED)
				contents[i] = null;
		}
	}

	private boolean processItemDrop(Item drop) {
		ItemStack item = drop.getItemStack();
		ExpirationResult result = MagicItemBehaviors.updateExpiresLineIfNeeded(item);
		if (result == ExpirationResult.UPDATE)
			drop.setItemStack(item);
		else if (result == ExpirationResult.EXPIRED) {
			drop.remove();
			return true;
		}
		return false;
	}

	private class CharacterExpirationHandler implements Listener {

		private CharacterExpirationHandler() {
			MagicSpells.registerEvents(this);
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onCharacterLoad(LoadCharacterEvent event) {
			if (!event.isCancelled()) {
				MagicSpells.scheduleDelayedTask(() -> {
					joinOrLoadCharacter(event.getPlayer());
				}, 1);
			}
		}
	}

}
