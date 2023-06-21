package com.nisovin.magicspells.spells.buff;

import java.util.*;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class ArmorSpell extends BuffSpell {

	private final Set<UUID> entities;

	private boolean permanent;
	private boolean replace;
	private boolean revert;

	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private String strHasArmor;

	private Component hiddenLore;

	public ArmorSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		hiddenLore = getInvisibleLore("MSArmorItem");

		permanent = getConfigBoolean("permanent", false);
		replace = getConfigBoolean("replace", false);
		revert = getConfigBoolean("revert", false);

		helmet = getItem(getConfigString("helmet", ""));
		chestplate = getItem(getConfigString("chestplate", ""));
		leggings = getItem(getConfigString("leggings", ""));
		boots = getItem(getConfigString("boots", ""));

		strHasArmor = getConfigString("str-has-armor", "You cannot cast this spell if you are wearing armor.");

		entities = new HashSet<>();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!permanent) registerEvents(new ArmorListener());
	}

	public static Component getInvisibleLore(String s) {
		String lore = "";
		for (char c : s.toCharArray()) lore += ChatColor.COLOR_CHAR + "" + c;
		return Util.getMiniMessage(lore);
	}

	private ItemStack getItem(String s) {
		if (s.isEmpty()) return null;

		MagicItem magicItem = MagicItems.getMagicItemFromString(s);
		if (magicItem == null) return null;

		ItemStack item = magicItem.getItemStack();
		if (item == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("ItemStack is null");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return null;
		}

		item.setAmount(1);

		if (!permanent) {
			ItemMeta meta = item.getItemMeta();
			List<Component> lore = meta.lore();
			if (lore == null) lore = new ArrayList<>();
			lore.add(hiddenLore);
			meta.lore(lore);
			item.setItemMeta(meta);
		}

		return item;
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		EntityEquipment inv = entity.getEquipment();
		if (inv == null) return false;

		if (!replace && ((helmet != null && inv.getHelmet() != null) || (chestplate != null && inv.getChestplate() != null) || (leggings != null && inv.getLeggings() != null) || (boots != null && inv.getBoots() != null))) {
			// error
			if (entity instanceof Player) sendMessage(strHasArmor, entity, args);
			return false;
		}

		setArmor(inv);

		if (!permanent) entities.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		return castBuff(entity, power, args);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (!entities.remove(entity.getUniqueId())) return;
		if (!entity.isValid()) return;
		EntityEquipment inv = entity.getEquipment();
		if(revert) revertArmor(inv, entity.getUniqueId());
		else removeArmor(inv);
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			if (!entity.isValid()) continue;
			EntityEquipment inv = ((LivingEntity) entity).getEquipment();
			if(revert) revertArmor(inv, entity.getUniqueId());
			else removeArmor(inv);
		}
		entities.clear();
	}

	private static class EquipStore {
		public ItemStack helmet;
		public ItemStack chest;
		public ItemStack legs;
		public ItemStack boots;
	}
	private final Map<UUID, EquipStore> equipStore = new HashMap<>();

	private void setArmor(EntityEquipment inv) {
		EquipStore eStore = new EquipStore();
		if (helmet != null) {
			eStore.helmet = inv.getHelmet();
			if (replace) inv.setHelmet(null);
			inv.setHelmet(helmet.clone());
		}

		if (chestplate != null) {
			eStore.chest = inv.getChestplate();
			if (replace) inv.setChestplate(null);
			inv.setChestplate(chestplate.clone());
		}

		if (leggings != null) {
			eStore.legs = inv.getLeggings();
			if (replace) inv.setLeggings(null);
			inv.setLeggings(leggings.clone());
		}

		if (boots != null) {
			eStore.boots = inv.getBoots();
			if (replace) inv.setBoots(null);
			inv.setBoots(boots.clone());
		}
		equipStore.put(inv.getHolder().getUniqueId(), eStore);
	}

	private void removeArmor(EntityEquipment inv) {
		ItemStack invHelmet = inv.getHelmet();
		if (helmet != null && invHelmet != null && invHelmet.getType() == helmet.getType()) {
			inv.setHelmet(null);
		}

		ItemStack invChestplate = inv.getChestplate();
		if (chestplate != null && invChestplate != null && invChestplate.getType() == chestplate.getType()) {
			inv.setChestplate(null);
		}

		ItemStack invLeggings = inv.getLeggings();
		if (leggings != null && invLeggings != null && invLeggings.getType() == leggings.getType()) {
			inv.setLeggings(null);
		}

		ItemStack invBoots = inv.getBoots();
		if (boots != null && invBoots != null && invBoots.getType() == boots.getType()) {
			inv.setBoots(null);
		}
	}

	private void revertArmor(EntityEquipment inv, UUID id){
		if(equipStore.containsKey(id)){
			EquipStore eStore = equipStore.remove(id);
			if(eStore.helmet != null){
				inv.setHelmet(null);
				inv.setHelmet(eStore.helmet);
			}
			if(eStore.chest != null){
				inv.setChestplate(null);
				inv.setChestplate(eStore.chest);
			}
			if(eStore.legs != null){
				inv.setLeggings(null);
				inv.setLeggings(eStore.legs);
			}
			if(eStore.boots != null){
				inv.setBoots(null);
				inv.setBoots(eStore.boots);
			}
		}
		removeArmor(inv);
	}

	private class ArmorListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onEntityDamage(EntityDamageEvent event) {
			Entity entity = event.getEntity();
			if (!(entity instanceof LivingEntity livingEntity)) return;
			if (!isActive(livingEntity)) return;
			if (livingEntity.getNoDamageTicks() >= 10) return;
			addUseAndChargeCost(livingEntity);
		}

		@EventHandler(ignoreCancelled=true)
		public void onInventoryClick(InventoryClickEvent event) {
			if (event.getSlotType() != SlotType.ARMOR) return;
			HumanEntity entity = event.getWhoClicked();
			if (!(entity instanceof Player p)) return;
			if (!isActive(p)) return;
			event.setCancelled(true);
		}

		@EventHandler
		public void onArmorSwap(PlayerInteractEvent event){
			Player player = event.getPlayer();
			PlayerInventory inventory = player.getInventory();
			//Messy but it works!
			if(!(inventory.getItemInMainHand().getType().toString().toLowerCase().contains("chestplate")
			|| inventory.getItemInMainHand().getType().toString().toLowerCase().contains("boots")
			|| inventory.getItemInMainHand().getType().toString().toLowerCase().contains("helmet")
			|| inventory.getItemInMainHand().getType().toString().toLowerCase().contains("legging")
			|| inventory.getItemInOffHand().getType().toString().toLowerCase().contains("chestplate")
					|| inventory.getItemInOffHand().getType().toString().toLowerCase().contains("boots")
					|| inventory.getItemInOffHand().getType().toString().toLowerCase().contains("helmet")
					|| inventory.getItemInOffHand().getType().toString().toLowerCase().contains("legging"))) return;
			if(!isActive(player)) return;
			event.setCancelled(true);

		}

		@EventHandler
		public void onEntityDeath(EntityDeathEvent event) {
			if (permanent) return;
			Iterator<ItemStack> drops = event.getDrops().iterator();
			while (drops.hasNext()) {
				ItemStack drop = drops.next();
				if (drop == null) continue;
				List<Component> lore = drop.lore();
				if (lore == null) continue;
				if (!lore.get(lore.size() - 1).equals(hiddenLore)) continue;
				drops.remove();
			}
		}

		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;
			if (isExpired(player)) return;

			final EntityEquipment inv = player.getEquipment();
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> setArmor(inv));
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;

			if (cancelOnLogout) turnOff(player);
			else removeArmor(player.getEquipment());
		}

		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;

			if (!isExpired(player)) setArmor(player.getEquipment());
			else turnOff(player);
		}

	}

	public Set<UUID> getEntities() {
		return entities;
	}

	public boolean isPermanent() {
		return permanent;
	}

	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}

	public boolean shouldReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}

	public ItemStack getHelmet() {
		return helmet;
	}

	public void setHelmet(ItemStack helmet) {
		this.helmet = helmet;
	}

	public ItemStack getChestplate() {
		return chestplate;
	}

	public void setChestplate(ItemStack chestplate) {
		this.chestplate = chestplate;
	}

	public ItemStack getLeggings() {
		return leggings;
	}

	public void setLeggings(ItemStack leggings) {
		this.leggings = leggings;
	}

	public ItemStack getBoots() {
		return boots;
	}

	public void setBoots(ItemStack boots) {
		this.boots = boots;
	}

	public String getHasArmorMessage() {
		return strHasArmor;
	}

	public void setHasArmorMessage(String strHasArmor) {
		this.strHasArmor = strHasArmor;
	}

	public Component getHiddenLore() {
		return hiddenLore;
	}

	public void setHiddenLore(Component hiddenLore) {
		this.hiddenLore = hiddenLore;
	}

}

