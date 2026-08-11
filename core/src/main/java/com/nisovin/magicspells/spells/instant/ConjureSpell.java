package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.ConjureItemEvent;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.compat.BagOfHoldingCompat;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.managers.AttributeManager;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.command.TomeSpell;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.command.ScrollSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

import net.sneakycharactermanager.paper.handlers.character.LoadCharacterEvent;

public class ConjureSpell extends InstantSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static ExpirationHandler expirationHandler = null;
	private static SoulboundHandler soulboundHandler = null;

	private int delay;
	private int pickupDelay;
	private int requiredSlot;
	private int preferredSlot;

	private double expiration;

	private float randomVelocity;

	private boolean soulbound;
	private boolean offhand;
	private boolean autoEquip;
	private boolean stackExisting;
	private boolean itemHasGravity;
	private boolean addToInventory;
	private boolean addToBagOfHolding;
	private boolean addToEnderChest;
	private boolean ignoreMaxStackSize;
	private boolean powerAffectsChance;
	private boolean dropIfInventoryFull;
	private boolean powerAffectsQuantity;
	private boolean forceUpdateInventory;
	private boolean calculateDropsIndividually;
	private boolean saveConjurerName;
	private boolean safeEnchants;

	private ConfigData<Map<Enchantment, Integer>> enchantments;
	private ConfigData<Set<AttributeManager.AttributeInfo>> attributes;

	private ConfigData<List<String>> itemListData;
	private ConfigData<Boolean> omitSelectedSlot;

	public ConjureSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		delay = getConfigInt("delay", -1);
		pickupDelay = getConfigInt("pickup-delay", 0);
		requiredSlot = getConfigInt("required-slot", -1);
		preferredSlot = getConfigInt("preferred-slot", -1);

		expiration = getConfigDouble("expiration", 0L);

		randomVelocity = getConfigFloat("random-velocity", 0F);

		soulbound = getConfigBoolean("soulbound", false);
		offhand = getConfigBoolean("offhand", false);
		autoEquip = getConfigBoolean("auto-equip", false);
		stackExisting = getConfigBoolean("stack-existing", true);
		itemHasGravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", false);
		addToBagOfHolding = getConfigBoolean("add-to-bag-of-holding", true);
		addToEnderChest = getConfigBoolean("add-to-ender-chest", false);
		ignoreMaxStackSize = getConfigBoolean("ignore-max-stack-size", false);
		powerAffectsChance = getConfigBoolean("power-affects-chance", true);
		dropIfInventoryFull = getConfigBoolean("drop-if-inventory-full", true);
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", false);
		forceUpdateInventory = getConfigBoolean("force-update-inventory", true);
		calculateDropsIndividually = getConfigBoolean("calculate-drops-individually", true);
		saveConjurerName = getConfigBoolean("save-conjurer-name", false);
		safeEnchants = getConfigBoolean("safe-enchants", true);

		itemListData = getConfigDataStringList("items", null);
		omitSelectedSlot = getConfigDataBoolean("omit-selected-slot", false);

		List<String> enchantmentList = getConfigStringList("enchantments", null);
		if (enchantmentList != null && !enchantmentList.isEmpty()) {
			enchantments = ConfigDataUtil.getEnchantmentsConfigData(enchantmentList);
		}

		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) {
			attributes = MagicSpells.getAttributeManager().getAttributesConfigData(attributeList,
					internalName + ".attributes");
		}

		pickupDelay = Math.max(pickupDelay, 0);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (expiration > 0 && expirationHandler == null)
			expirationHandler = new ExpirationHandler();
		if (soulbound && soulboundHandler == null)
			soulboundHandler = new SoulboundHandler();

	}

	private Object[] processItemList(List<String> itemList) {

		ItemStack[] itemTypes = null;
		int[] itemMinQuantities = null;
		int[] itemMaxQuantities = null;
		double[] itemChances = null;

		if (itemList != null && !itemList.isEmpty()) {
			itemTypes = new ItemStack[itemList.size()];
			itemMinQuantities = new int[itemList.size()];
			itemMaxQuantities = new int[itemList.size()];
			itemChances = new double[itemList.size()];

			for (int i = 0; i < itemList.size(); i++) {
				try {
					String str = itemList.get(i);

					int brackets = 0;
					int closedBrackets = 0;
					for (int j = 0; j < str.length(); j++) {
						char ch = str.charAt(j);
						if (ch == '{')
							brackets++;
						if (ch == '}')
							closedBrackets++;
					}

					// checks if all brackets are properly closed
					if (brackets != closedBrackets) {
						MagicSpells
								.error("ConjureSpell '" + internalName + "' has an invalid item defined (e1): " + str);
						continue;
					}

					brackets = 0;
					closedBrackets = 0;

					String[] data = str.split(" ");
					String[] conjureData = null;

					StringBuilder itemData = new StringBuilder();

					for (int j = 0; j < data.length; j++) {
						for (char ch : data[j].toCharArray()) {
							if (ch == '{')
								brackets++;
							if (ch == '}')
								closedBrackets++;
						}

						itemData.append(data[j]).append(" ");
						// magicItemData is ready, add the conjureData
						if (brackets == closedBrackets) {
							int dataLeft = data.length - j - 1;
							conjureData = new String[dataLeft];

							// fill the conjureData array with stuff like amount and chance
							for (int d = 0; d < dataLeft; d++) {
								conjureData[d] = data[j + d + 1];
							}
							break;
						}
					}

					String strItemData = itemData.toString().trim();

					if (strItemData.startsWith("TOME:")) {
						String[] tomeData = strItemData.split(":");
						TomeSpell tomeSpell = (TomeSpell) MagicSpells.getSpellByInternalName(tomeData[1]);
						Spell spell = MagicSpells.getSpellByInternalName(tomeData[2]);
						int uses = tomeData.length > 3 ? Integer.parseInt(tomeData[3].trim()) : -1;
						itemTypes[i] = tomeSpell.createTome(spell, uses, null);
					} else if (strItemData.startsWith("SCROLL:")) {
						String[] scrollData = strItemData.split(":");
						ScrollSpell scrollSpell = (ScrollSpell) MagicSpells.getSpellByInternalName(scrollData[1]);
						Spell spell = MagicSpells.getSpellByInternalName(scrollData[2]);
						int uses = scrollData.length > 3 ? Integer.parseInt(scrollData[3].trim()) : -1;
						itemTypes[i] = scrollSpell.createScroll(spell, uses, null);
					} else {
						MagicItem magicItem = MagicItems.getMagicItemFromString(strItemData);
						if (magicItem == null)
							continue;
						itemTypes[i] = magicItem.getItemStack();
					}

					int minAmount = 1;
					int maxAmount = 1;

					double chance = 100;

					// add default values if there arent any specified
					if (conjureData == null) {
						itemMinQuantities[i] = minAmount;
						itemMaxQuantities[i] = maxAmount;
						itemChances[i] = chance;
						continue;
					}

					// parse minAmount, maxAmount
					if (conjureData.length >= 1) {
						String[] amount = conjureData[0].split("-");
						if (amount.length == 1) {
							minAmount = Integer.parseInt(amount[0].trim());
							maxAmount = minAmount;
						} else if (amount.length >= 2) {
							minAmount = Integer.parseInt(amount[0].trim());
							maxAmount = Integer.parseInt(amount[1].trim()) + 1;
						}
					}

					// parse chance
					if (conjureData.length >= 2) {
						chance = Double.parseDouble(conjureData[1].replace("%", "").trim());
					}

					itemMinQuantities[i] = minAmount;
					itemMaxQuantities[i] = maxAmount;
					itemChances[i] = chance;

				} catch (Exception e) {
					MagicSpells.error(
							"ConjureSpell '" + internalName + "' has specified invalid item (e2): " + itemList.get(i));
					itemTypes[i] = null;
				}
			}
		}
		itemList = null;
		return new Object[] { itemTypes, itemMinQuantities, itemMaxQuantities, itemChances };
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {

		if (itemListData == null)
			return PostCastAction.ALREADY_HANDLED;
		if (state == SpellCastState.NORMAL && caster instanceof Player) {
			if (delay >= 0)
				MagicSpells.scheduleDelayedTask(() -> conjureItems((Player) caster, power, args), delay);
			else if (!conjureItems((Player) caster, power, args))
				return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;

	}

	private boolean conjureItems(Player player, float power, String[] args) {
		boolean succes = true;

		SpellData spellData = new SpellData(player, power, args);
		List<String> itemList = this.itemListData.get(spellData);

		Object[] itemResults = processItemList(itemList);

		ItemStack[] itemTypes = (ItemStack[]) itemResults[0];
		int[] itemMinQuantities = (int[]) itemResults[1];
		int[] itemMaxQuantities = (int[]) itemResults[2];
		double[] itemChances = (double[]) itemResults[3];

		List<ItemStack> items = new ArrayList<>();
		if (calculateDropsIndividually)
			individual(items, spellData, itemTypes, itemChances, itemMinQuantities, itemMaxQuantities);
		else
			together(items, spellData, itemTypes, itemChances, itemMinQuantities, itemMaxQuantities);

		Location loc = player.getEyeLocation().add(player.getLocation().getDirection());
		boolean updateInv = false;
		boolean depositedToBag = false;
		for (ItemStack itemOrg : items) {
			if (itemOrg == null)
				continue;
			ItemStack item = itemOrg.clone();

			if (saveConjurerName || soulbound || item.getMaxStackSize() == 1) {
				ItemMeta meta = item.getItemMeta();
				if (saveConjurerName || item.getMaxStackSize() == 1) {
					meta.getPersistentDataContainer().set(new NamespacedKey(MagicSpells.getInstance(), "creator_name"),
							PersistentDataType.STRING, player.getName());
				}
				if (soulbound) {
					meta.getPersistentDataContainer().set(
							new NamespacedKey(MagicSpells.getInstance(), "soulbound_owner"), PersistentDataType.STRING,
							player.getUniqueId().toString());
				}
				item.setItemMeta(meta);
			}

			boolean added = false;
			PlayerInventory inv = player.getInventory();
			if (autoEquip && item.getAmount() == 1) {
				if (item.getType().name().endsWith("HELMET") && InventoryUtil.isNothing(inv.getHelmet())) {
					inv.setHelmet(item);
					added = true;
				} else if (item.getType().name().endsWith("CHESTPLATE")
						&& InventoryUtil.isNothing(inv.getChestplate())) {
					inv.setChestplate(item);
					added = true;
				} else if (item.getType().name().endsWith("LEGGINGS") && InventoryUtil.isNothing(inv.getLeggings())) {
					inv.setLeggings(item);
					added = true;
				} else if (item.getType().name().endsWith("BOOTS") && InventoryUtil.isNothing(inv.getBoots())) {
					inv.setBoots(item);
					added = true;
				}
			}

			// Prefer bag of holding (soft dep) before ender chest / inventory / drop.
			// Commit bag only after the remainder is fully placed so we never leave a partial bag deposit.
			int bagTake = 0;
			String bagItemId = null;
			ItemStack placeItem = item;
			if (!added && addToBagOfHolding) {
				bagItemId = BagOfHoldingCompat.resolveItemId(item);
				if (bagItemId != null && BagOfHoldingCompat.isAutopickupEnabled(player, bagItemId)) {
					bagTake = Math.min(item.getAmount(), BagOfHoldingCompat.getRemainingCapacity(player, bagItemId));
					if (bagTake > 0) {
						int remainder = item.getAmount() - bagTake;
						if (remainder <= 0) {
							placeItem = null;
						} else {
							placeItem = item.clone();
							placeItem.setAmount(remainder);
						}
					}
				}
			}

			if (!added && placeItem != null) {
				if (addToEnderChest)
					added = Util.addToInventory(player, player.getEnderChest(), placeItem, stackExisting,
							ignoreMaxStackSize);
				if (!added && addToInventory) {

					ItemStack preferredItem = null;
					if (preferredSlot >= 0) {
						preferredItem = inv.getItem(preferredSlot);
					}

					if (offhand)
						player.getEquipment().setItemInOffHand(placeItem);
					else if (requiredSlot >= 0) {
						ItemStack old = inv.getItem(requiredSlot);
						if (old != null && Util.isSimilarNoFlags(placeItem, old))
							placeItem.setAmount(placeItem.getAmount() + old.getAmount());
						inv.setItem(requiredSlot, placeItem);
						added = true;
						updateInv = true;
					} else if (preferredSlot >= 0 && InventoryUtil.isNothing(preferredItem)) {
						inv.setItem(preferredSlot, placeItem);
						added = true;
						updateInv = true;
					} else if (preferredSlot >= 0 && Util.isSimilarNoFlags(placeItem, preferredItem)
							&& preferredItem.getAmount() + placeItem.getAmount() < placeItem.getType().getMaxStackSize()) {
						placeItem.setAmount(placeItem.getAmount() + preferredItem.getAmount());
						inv.setItem(preferredSlot, placeItem);
						added = true;
						updateInv = true;
					} else {
						Set<Integer> omittedSlots = null;
						if (Boolean.TRUE.equals(omitSelectedSlot.get(spellData))) {
							omittedSlots = Collections.singleton(player.getInventory().getHeldItemSlot());
						}
						added = Util.addToInventory(player, inv, placeItem, stackExisting, ignoreMaxStackSize, omittedSlots);
						if (added)
							updateInv = true;
					}
				}
				if (!added && (dropIfInventoryFull || !addToInventory)) {
					int amt = placeItem.getAmount();
					while (amt > 0) {
						ItemStack drop = placeItem.clone();
						drop.setAmount(Math.min(drop.getMaxStackSize(), amt));

						Item i = player.getWorld().dropItem(loc, drop);

						PlayerDropItemEvent event = new PlayerDropItemEvent(player, i);
						EventUtil.call(event);

						if (!event.isCancelled()) {
							i.setItemStack(drop);
							i.setPickupDelay(pickupDelay);
							i.setGravity(itemHasGravity);
							UUID uuid = player.getUniqueId();
							i.setThrower(uuid);
							playSpellEffects(EffectPosition.SPECIAL, i);
						}

						amt -= drop.getMaxStackSize();
					}
					added = true;
				}
			}

			if (bagTake > 0 && bagItemId != null) {
				if (placeItem == null || added) {
					int deposited = BagOfHoldingCompat.deposit(player, bagItemId, bagTake);
					added = deposited >= bagTake;
					if (deposited > 0) depositedToBag = true;
				}
			} else if (added) {
				updateInv = true;
			}

			if (!added)
				succes = false;
			else
				EventUtil.call(new ConjureItemEvent(player, item));
		}

		if (succes) {
			if (updateInv && forceUpdateInventory)
				player.updateInventory();
			if (depositedToBag) BagOfHoldingCompat.playPickupFeedback(player);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return succes;
	}

	private void individual(List<ItemStack> items, SpellData spellData, ItemStack[] itemTypes, double[] itemChances,
			int[] itemMinQuantities, int[] itemMaxQuantities) {
		float power = spellData.power();
		for (int i = 0; i < itemTypes.length; i++) {
			double r = random.nextDouble() * 100;
			if (powerAffectsChance)
				r = r / power;
			if (itemTypes[i] != null && r < itemChances[i])
				addItem(i, items, spellData, itemTypes, itemMinQuantities, itemMaxQuantities);
		}
	}

	private void together(List<ItemStack> items, SpellData spellData, ItemStack[] itemTypes, double[] itemChances,
			int[] itemMinQuantities, int[] itemMaxQuantities) {
		double r = random.nextDouble() * Arrays.stream(itemChances).sum();
		double m = 0;
		for (int i = 0; i < itemTypes.length; i++) {
			if (itemTypes[i] != null && r < itemChances[i] + m) {
				addItem(i, items, spellData, itemTypes, itemMinQuantities, itemMaxQuantities);
				return;
			} else
				m += itemChances[i];
		}
	}

	private void addItem(int i, List<ItemStack> items, SpellData spellData, ItemStack[] itemTypes,
			int[] itemMinQuantities, int[] itemMaxQuantities) {
		float power = spellData.power();
		int quant = itemMinQuantities[i];
		if (itemMaxQuantities[i] > itemMinQuantities[i])
			quant = random.nextInt(itemMaxQuantities[i] - itemMinQuantities[i]) + itemMinQuantities[i];
		if (powerAffectsQuantity)
			quant = Math.round(quant * power);
		if (quant > 0) {
			ItemStack item = itemTypes[i].clone();
			item.setAmount(quant);
			applyEnchantments(item, spellData);
			applyAttributes(item, spellData);
			if (expiration > 0)
				expirationHandler.addExpiresLine(item, expiration);
			items.add(item);
		}
	}

	private void applyEnchantments(ItemStack item, SpellData spellData) {
		if (enchantments == null)
			return;
		Map<Enchantment, Integer> resolved = enchantments.get(spellData);
		if (resolved == null || resolved.isEmpty())
			return;
		for (Map.Entry<Enchantment, Integer> entry : resolved.entrySet()) {
			Enchantment enchant = entry.getKey();
			int level = entry.getValue();
			if (!enchant.canEnchantItem(item))
				continue;
			if (safeEnchants && level > enchant.getMaxLevel())
				level = enchant.getMaxLevel();
			if (level <= 0)
				item.removeEnchantment(enchant);
			else if (safeEnchants)
				item.addEnchantment(enchant, level);
			else
				item.addUnsafeEnchantment(enchant, level);
		}
	}

	private void applyAttributes(ItemStack item, SpellData spellData) {
		if (attributes == null)
			return;
		Set<AttributeManager.AttributeInfo> resolved = attributes.get(spellData);
		if (resolved == null || resolved.isEmpty())
			return;
		MagicSpells.getAttributeManager().addItemAttributes(item, resolved);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return conjureItemsAtLocation(target, power, caster, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return conjureItemsAtLocation(target, power, null, null);
	}

	private boolean conjureItemsAtLocation(Location location, float power, @NotNull LivingEntity player,
			@NotNull LivingEntity target) {

		SpellData spellData = new SpellData(player, location, power);
		List<String> itemList = this.itemListData.get(spellData);

		Object[] itemResults = processItemList(itemList);
		ItemStack[] itemTypes = (ItemStack[]) itemResults[0];
		int[] itemMinQuantities = (int[]) itemResults[1];
		int[] itemMaxQuantities = (int[]) itemResults[2];
		double[] itemChances = (double[]) itemResults[3];

		List<ItemStack> items = new ArrayList<>();
		if (calculateDropsIndividually)
			individual(items, spellData, itemTypes, itemChances, itemMinQuantities, itemMaxQuantities);
		else
			together(items, spellData, itemTypes, itemChances, itemMinQuantities, itemMaxQuantities);

		Location loc = location.clone();
		if (!BlockUtils.isAir(loc.getBlock().getType()))
			loc.add(0, 1, 0);
		if (!BlockUtils.isAir(loc.getBlock().getType()))
			loc.add(0, 1, 0);
		for (ItemStack item : items) {

			if (player != null && soulbound) {
				ItemMeta meta = item.getItemMeta();
				meta.getPersistentDataContainer().set(new NamespacedKey(MagicSpells.getInstance(), "soulbound_owner"),
						PersistentDataType.STRING, player.getUniqueId().toString());
				item.setItemMeta(meta);
			}

			int amt = item.getAmount();
			while (amt > 0) {
				ItemStack drop = item.clone();
				drop.setAmount(Math.min(drop.getMaxStackSize(), amt));

				Item i = player.getWorld().dropItem(loc, drop);

				PlayerDropItemEvent event = null;

				if (player != null && player instanceof Player pl) {
					event = new PlayerDropItemEvent(pl, i);
					EventUtil.call(event);
				}

				if (event == null || !event.isCancelled()) {
					i.setItemStack(drop);
					i.setPickupDelay(pickupDelay);
					if (randomVelocity > 0) {
						Vector v = new Vector(random.nextDouble() - 0.5, random.nextDouble() / 2,
								random.nextDouble() - 0.5);
						v.normalize().multiply(randomVelocity);
						i.setVelocity(v);
					}
					i.setGravity(itemHasGravity);
					if (player != null)
						i.setThrower(player.getUniqueId());
					playSpellEffects(EffectPosition.SPECIAL, i);
					EventUtil.call(new ConjureItemEvent(player, item));
				}

				amt -= drop.getMaxStackSize();
			}
		}
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target))
			return false;

		if (target instanceof Player player)
			conjureItems(player, power, null);
		else
			return conjureItemsAtLocation(target.getLocation(), power, caster, target);

		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target))
			return false;

		if (target instanceof Player player)
			conjureItems(player, power, null);
		else
			return conjureItemsAtLocation(target.getLocation(), power, null, target);

		return true;
	}

	public void turnOff() {
		expirationHandler = null;
		soulboundHandler = null;
	}

	private int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getPickupDelay() {
		return pickupDelay;
	}

	public void setPickupDelay(int pickupDelay) {
		this.pickupDelay = pickupDelay;
	}

	public int getRequiredSlot() {
		return requiredSlot;
	}

	public void setRequiredSlot(int requiredSlot) {
		this.requiredSlot = requiredSlot;
	}

	public int getPreferredSlot() {
		return preferredSlot;
	}

	public void setPreferredSlot(int preferredSlot) {
		this.preferredSlot = preferredSlot;
	}

	public double getExpiration() {
		return expiration;
	}

	public void setExpiration(double expiration) {
		this.expiration = expiration;
	}

	public float getRandomVelocity() {
		return randomVelocity;
	}

	public void setRandomVelocity(float randomVelocity) {
		this.randomVelocity = randomVelocity;
	}

	public boolean shouldConjureInOffhand() {
		return offhand;
	}

	public void setConjureInOffhand(boolean offhand) {
		this.offhand = offhand;
	}

	public boolean shouldAutoEquip() {
		return autoEquip;
	}

	public void setAutoEquip(boolean autoEquip) {
		this.autoEquip = autoEquip;
	}

	public boolean shouldStackExisting() {
		return stackExisting;
	}

	public void setStackExisting(boolean stackExisting) {
		this.stackExisting = stackExisting;
	}

	public boolean shouldItemHaveGravity() {
		return itemHasGravity;
	}

	public void setItemHasGravity(boolean itemHasGravity) {
		this.itemHasGravity = itemHasGravity;
	}

	public boolean shouldAddToInventory() {
		return addToInventory;
	}

	public void setAddToInventory(boolean addToInventory) {
		this.addToInventory = addToInventory;
	}

	public boolean shouldAddToBagOfHolding() {
		return addToBagOfHolding;
	}

	public void setAddToBagOfHolding(boolean addToBagOfHolding) {
		this.addToBagOfHolding = addToBagOfHolding;
	}

	public boolean shouldAddToEnderChest() {
		return addToEnderChest;
	}

	public void setAddToEnderChest(boolean addToEnderChest) {
		this.addToEnderChest = addToEnderChest;
	}

	public boolean shouldIgnoreMaxStackSize() {
		return ignoreMaxStackSize;
	}

	public void setIgnoreMaxStackSize(boolean ignoreMaxStackSize) {
		this.ignoreMaxStackSize = ignoreMaxStackSize;
	}

	public boolean shouldPowerAffectChance() {
		return powerAffectsChance;
	}

	public void setPowerAffectsChance(boolean powerAffectsChance) {
		this.powerAffectsChance = powerAffectsChance;
	}

	public boolean shouldDropIfInventoryFull() {
		return dropIfInventoryFull;
	}

	public void setDropIfInventoryFull(boolean dropIfInventoryFull) {
		this.dropIfInventoryFull = dropIfInventoryFull;
	}

	public boolean shouldPowerAffectQuantity() {
		return powerAffectsQuantity;
	}

	public void setPowerAffectsQuantity(boolean powerAffectsQuantity) {
		this.powerAffectsQuantity = powerAffectsQuantity;
	}

	public boolean shouldForceUpdateInventory() {
		return forceUpdateInventory;
	}

	public void setForceUpdateInventory(boolean forceUpdateInventory) {
		this.forceUpdateInventory = forceUpdateInventory;
	}

	public boolean shouldCalculateDropsIndividually() {
		return calculateDropsIndividually;
	}

	public void setCalculateDropsIndividually(boolean calculateDropsIndividually) {
		this.calculateDropsIndividually = calculateDropsIndividually;
	}

	private static class ExpirationHandler implements Listener {

		private static CharacterExpirationHandler characterExpirationHandler = null;

		private ExpirationHandler() {
			MagicSpells.registerEvents(this);

			if (CompatBasics.pluginEnabled("SneakyCharacterManager")) {
				characterExpirationHandler = new CharacterExpirationHandler();
			}
		}

		private void addExpiresLine(ItemStack item, double expireMilis) {
			ItemMeta meta = item.getItemMeta();
			List<Component> lore = null;
			if (meta.hasLore())
				lore = meta.lore();
			if (lore == null)
				lore = new ArrayList<>();

			long expiresAt = System.currentTimeMillis() + (long) expireMilis;
			lore.add(Util.getMiniMessage(getExpiresText(expiresAt)));
			meta.getPersistentDataContainer().set(new NamespacedKey(MagicSpells.getInstance(), "expires_at"),
					PersistentDataType.LONG, expiresAt);

			meta.lore(lore);
			item.setItemMeta(meta);
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onJoin(PlayerJoinEvent event) {
			joinOrLoadCharacter(event.getPlayer());
		}

		private void joinOrLoadCharacter(Player player) {
			PlayerInventory inv = player.getInventory();
			processInventory(inv);
			ItemStack[] armor = inv.getArmorContents();
			processInventoryContents(armor);
			inv.setArmorContents(armor);
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
			ExpirationResult result = updateExpiresLineIfNeeded(item);
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
			ExpirationResult result = updateExpiresLineIfNeeded(item);
			if (result == ExpirationResult.EXPIRED) {
				event.getPlayer().getEquipment().setItemInMainHand(null);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onHotbarScroll(PlayerItemHeldEvent event) {
			ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
			ExpirationResult result = updateExpiresLineIfNeeded(item);
			if (result == ExpirationResult.EXPIRED) {
				event.getPlayer().getInventory().setItem(event.getNewSlot(), null);
			}
		}

		@EventHandler(priority = EventPriority.MONITOR)
		private void onInventoryClick(InventoryClickEvent event) {
			ItemStack item = event.getCurrentItem();
			ExpirationResult result = updateExpiresLineIfNeeded(item);
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
				ExpirationResult result = updateExpiresLineIfNeeded(contents[i]);
				if (result == ExpirationResult.EXPIRED)
					contents[i] = null;
			}
		}

		private boolean processItemDrop(Item drop) {
			ItemStack item = drop.getItemStack();
			ExpirationResult result = updateExpiresLineIfNeeded(item);
			if (result == ExpirationResult.UPDATE)
				drop.setItemStack(item);
			else if (result == ExpirationResult.EXPIRED) {
				drop.remove();
				return true;
			}
			return false;
		}

		private ExpirationResult updateExpiresLineIfNeeded(ItemStack item) {
			if (item == null)
				return ExpirationResult.NO_UPDATE;
			if (!item.hasItemMeta())
				return ExpirationResult.NO_UPDATE;

			ItemMeta meta = item.getItemMeta();

			Long expiresAt = meta.getPersistentDataContainer()
					.get(new NamespacedKey(MagicSpells.getInstance(), "expires_at"), PersistentDataType.LONG);
			if (expiresAt == null)
				return ExpirationResult.NO_UPDATE;

			if (expiresAt < System.currentTimeMillis())
				return ExpirationResult.EXPIRED;

			if (!meta.hasLore())
				return ExpirationResult.NO_UPDATE;
			List<Component> lore = meta.lore();

			if (lore != null && lore.size() > 0
					&& ((TextComponent) lore.get(lore.size() - 1)).content().contains("Expires in ")) {
				lore.set(lore.size() - 1, Util.getMiniMessage(getExpiresText(expiresAt)));
				meta.lore(lore);
				item.setItemMeta(meta);
				return ExpirationResult.UPDATE;
			}
			return ExpirationResult.NO_UPDATE;
		}

		private String getExpiresText(long expiresAt) {
			if (expiresAt < System.currentTimeMillis())
				return ChatColor.GRAY + "Expired";
			double hours = (expiresAt - System.currentTimeMillis()) / ((double) TimeUtil.MILLISECONDS_PER_HOUR);
			if (hours / 24 >= 15)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((long) hours / TimeUtil.HOURS_PER_WEEK)
						+ ChatColor.GRAY + " weeks";
			if (hours / 24 >= 3)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((long) hours / TimeUtil.HOURS_PER_DAY)
						+ ChatColor.GRAY + " days";
			if (hours >= 2)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + (long) hours + ChatColor.GRAY + " hours";
			if (hours >= 1)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + '1' + ChatColor.GRAY + " hour";
			double minutes = (expiresAt - System.currentTimeMillis()) / ((double) TimeUtil.MILLISECONDS_PER_MINUTE);
			if (minutes >= 2)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + (long) minutes + ChatColor.GRAY + " minutes";
			if (minutes >= 1)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + '1' + ChatColor.GRAY + " minute";
			double seconds = (expiresAt - System.currentTimeMillis()) / ((double) TimeUtil.MILLISECONDS_PER_SECOND);
			if (seconds >= 2)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + (long) seconds + ChatColor.GRAY + " seconds";
			return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + '1' + ChatColor.GRAY + " second";
		}

	}

	private static class CharacterExpirationHandler implements Listener {

		private CharacterExpirationHandler() {
			MagicSpells.registerEvents(this);
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onCharacterLoad(LoadCharacterEvent event) {
			if (!event.isCancelled()) {
				MagicSpells.scheduleDelayedTask(() -> {
					expirationHandler.joinOrLoadCharacter(event.getPlayer());
				}, 1);
			}
		}
	}

	private static class SoulboundHandler implements Listener {

		private SoulboundHandler() {
			MagicSpells.registerEvents(this);
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onJoin(PlayerJoinEvent event) {
			processInventory(event.getPlayer());
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onInvOpen(InventoryOpenEvent event) {
			if (event.getPlayer() instanceof Player player) {
				Inventory inv = event.getInventory();
				if (inv instanceof PlayerInventory || inv.getType() == InventoryType.ENDER_CHEST) {
					processInventory(player, inv);
				}
			}
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		private void onClick(InventoryClickEvent event) {
			if (!(event.getWhoClicked() instanceof Player player))
				return;
			if (isInvalidSoulbound(event.getCurrentItem(), player) || isInvalidSoulbound(event.getCursor(), player)) {
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		private void onDrag(InventoryDragEvent event) {
			if (!(event.getWhoClicked() instanceof Player player))
				return;
			if (isInvalidSoulbound(event.getOldCursor(), player)) {
				event.setCancelled(true);
				return;
			}
			for (ItemStack item : event.getNewItems().values()) {
				if (isSoulbound(item)) {
					// This is more complex to cancel partially, so just cancel if any soulbound
					// item is involved and invalid
					if (isInvalidSoulbound(item, player)) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		private void onPickup(EntityPickupItemEvent event) {
			ItemStack item = event.getItem().getItemStack();
			if (!isSoulbound(item))
				return;

			if (!(event.getEntity() instanceof Player player) || isInvalidSoulbound(item, player)) {
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onInteract(PlayerInteractEvent event) {
			if (!event.hasItem())
				return;
			if (isInvalidSoulbound(event.getItem(), event.getPlayer())) {
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onItemSwap(PlayerSwapHandItemsEvent event) {
			if (isInvalidSoulbound(event.getOffHandItem(), event.getPlayer())
					|| isInvalidSoulbound(event.getMainHandItem(), event.getPlayer())) {
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onHotbarScroll(PlayerItemHeldEvent event) {
			ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
			if (isInvalidSoulbound(item, event.getPlayer())) {
				event.setCancelled(true);
			}
		}

		private void processInventory(Player player) {
			processInventory(player, player.getInventory());
			processInventory(player, player.getEnderChest());
		}

		private void processInventory(Player player, Inventory inv) {
			ItemStack[] contents = inv.getContents();
			boolean changed = false;
			for (int i = 0; i < contents.length; i++) {
				if (isInvalidSoulbound(contents[i], player)) {
					contents[i] = null;
					changed = true;
				}
			}
			if (changed)
				inv.setContents(contents);
		}

		private boolean isSoulbound(ItemStack item) {
			if (item == null || !item.hasItemMeta())
				return false;
			return item.getItemMeta().getPersistentDataContainer()
					.has(new NamespacedKey(MagicSpells.getInstance(), "soulbound_owner"), PersistentDataType.STRING);
		}

		private boolean isInvalidSoulbound(ItemStack item, Player player) {
			if (item == null || !item.hasItemMeta())
				return false;
			if (Perm.NO_SOULBOUND.has(player) && player.getGameMode() == GameMode.CREATIVE)
				return false;
			String ownerUuid = item.getItemMeta().getPersistentDataContainer()
					.get(new NamespacedKey(MagicSpells.getInstance(), "soulbound_owner"), PersistentDataType.STRING);
			if (ownerUuid == null)
				return false;
			return !ownerUuid.equals(player.getUniqueId().toString());
		}

	}

	private enum ExpirationResult {

		NO_UPDATE,
		UPDATE,
		EXPIRED

	}

}
