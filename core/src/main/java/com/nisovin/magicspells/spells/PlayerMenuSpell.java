package com.nisovin.magicspells.spells;

import java.util.*;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PlayerMenuSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, MenuData> menuData;

	private final int delay;
	private final String title;
	private final double radius;
	private final boolean stayOpen;
	private final boolean addOpener;
	private final String skullName;
	private final String skullNameOffline;
	private final String skullNameRadius;
	private final List<String> skullLore;
	private final String spellRangeName;
	private final String spellOfflineName;
	private final boolean castSpellsOnTarget;
	private final String spellOnLeftName;
	private final String spellOnRightName;
	private final String spellOnMiddleName;
	private final String spellOnSneakLeftName;
	private final String spellOnSneakRightName;
	private final List<String> playerModifiersStrings;
	private final String variableTarget;

	private Subspell spellOffline;
	private Subspell spellRange;
	private Subspell spellOnLeft;
	private Subspell spellOnRight;
	private Subspell spellOnMiddle;
	private Subspell spellOnSneakLeft;
	private Subspell spellOnSneakRight;
	private ModifierSet playerModifiers;

	private final ItemStack previousPageItem;
	private final ItemStack nextPageItem;

	public PlayerMenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		delay = getConfigInt("delay", 0);
		title = getConfigString("title", "PlayerMenuSpell '" + internalName + "'");
		radius = getConfigDouble("radius", 0);
		stayOpen = getConfigBoolean("stay-open", false);
		addOpener = getConfigBoolean("add-opener", false);
		skullName = getConfigString("skull-name", "&6%t");
		skullNameOffline = getConfigString("skull-name-offline", "&4%t");
		skullNameRadius = getConfigString("skull-name-radius", "&4%t &3out of radius.");
		skullLore = getConfigStringList("skull-lore", null);
		spellOfflineName = getConfigString("spell-offline", "");
		spellRangeName = getConfigString("spell-range", "");
		castSpellsOnTarget = getConfigBoolean("cast-spells-on-target", true);
		spellOnLeftName = getConfigString("spell-on-left", "");
		spellOnRightName = getConfigString("spell-on-right", "");
		spellOnMiddleName = getConfigString("spell-on-middle", "");
		spellOnSneakLeftName = getConfigString("spell-on-sneak-left", "");
		spellOnSneakRightName = getConfigString("spell-on-sneak-right", "");
		playerModifiersStrings = getConfigStringList("player-modifiers", null);
		variableTarget = getConfigString("variable-target", null);

		previousPageItem = createItem("previous-page-item", "Previous Page");
		nextPageItem = createItem("next-page-item", "Next Page");
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (playerModifiersStrings == null || playerModifiersStrings.isEmpty()) return;
		playerModifiers = new ModifierSet(playerModifiersStrings, this);
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "PlayerMenuSpell '" + internalName + "' has an invalid ";
		spellOffline = initSubspell(spellOfflineName, error + "spell-offline defined!");
		spellRange = initSubspell(spellRangeName, error + "spell-range defined!");
		spellOnLeft = initSubspell(spellOnLeftName, error + "spell-on-left defined!");
		spellOnRight = initSubspell(spellOnRightName, error + "spell-on-right defined!");
		spellOnMiddle = initSubspell(spellOnMiddleName, error + "spell-on-middle defined!");
		spellOnSneakLeft = initSubspell(spellOnSneakLeftName, error + "spell-on-sneak-left defined!");
		spellOnSneakRight = initSubspell(spellOnSneakRightName, error + "spell-on-sneak-right defined!");

		menuData = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power, args);
			if (targetInfo.noTarget()) return noTarget(caster, args, targetInfo);
			Player target = targetInfo.target();

			openDelay(caster, target, power, args);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!(target instanceof Player player) || !validTargetList.canTarget(caster, target)) return false;
		openDelay(caster, player, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!(target instanceof Player player) || !validTargetList.canTarget(target)) return false;
		openDelay(null, player, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;
		Player player = Bukkit.getPlayer(args[0]);
		if (player == null) return false;
		openDelay(null, player, 1, null);
		return true;
	}

	private ItemStack createItem(String path, String defaultName) {
		ItemStack item = null;
		if (isConfigSection(path)) {
			MagicItem magicItem = MagicItems.getMagicItemFromSection(getConfigSection(path));
			if (magicItem != null) item = magicItem.getItemStack();
		} else {
			MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString(path, ""));

			if (magicItem == null) {
				item = new ItemStack(Material.GREEN_WOOL);
				ItemMeta itemMeta = item.getItemMeta();

				itemMeta.setDisplayName("§6§t" + defaultName);
				item.setItemMeta(itemMeta);
			} else {
				item = magicItem.getItemStack();
			}
		}
		return item;
	}

	private void openDelay(LivingEntity caster, Player opener, float power, String[] args) {
		MenuData data = new MenuData(new SpellData(caster, opener, power, args), 0);
		menuData.put(opener.getUniqueId(), data);

		if (delay > 0) MagicSpells.scheduleDelayedTask(() -> open(opener, data, 0), delay);
		else open(opener, data, 0);
	}

	private Component translate(Player player, Player target, String string) {
		if (target != null) string = string.replaceAll("%t", target.getName());
		string = string.replaceAll("%a", player.getName());
		return Util.getMiniMessageWithVars(player, string);
	}

	private void processClickSpell(Subspell subspell, Player caster, Player target, float power) {
		if (subspell == null) return;
		if (castSpellsOnTarget && subspell.isTargetedEntitySpell()) {
			subspell.castAtEntity(caster, target, power);
			return;
		}
		subspell.cast(caster, power);
	}

	private void open(Player opener, MenuData data, int page) {
		SpellData spellData = data.spellData();

		List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
		if (!addOpener) players.remove(opener);
		if (playerModifiers != null) players.removeIf(player -> !playerModifiers.check(player));
		if (radius > 0) players.removeIf(player -> opener.getLocation().distance(player.getLocation()) > radius);

		int size = (int) Math.ceil(Math.min((players.size()+1), 54) / 9.0) * 9;
		Inventory inv = Bukkit.createInventory(opener, size, Component.text(internalName));

		for (int i = (page * 52); i < Math.min(players.size(), (page + 1) * 52); i++) {
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			ItemMeta itemMeta = head.getItemMeta();
			SkullMeta skullMeta = (SkullMeta) itemMeta;
			if (skullMeta == null) continue;
			skullMeta.setOwningPlayer(players.get(i));
			itemMeta.displayName(translate(opener, players.get(i), skullName));
			if (skullLore != null) {
				List<Component> lore = new ArrayList<>();
				for (String loreLine : skullLore) {
					lore.add(translate(opener, players.get(i), loreLine));
				}
				itemMeta.lore(lore);
			}
			head.setItemMeta(skullMeta);
			inv.setItem(i%52, head);
		}

		if (page > 0) {
			inv.setItem(52, previousPageItem);
		}

		if (players.size() > ((page + 1) * 52)) {
			inv.setItem(53, nextPageItem);
		}

		opener.openInventory(inv);
		Util.setInventoryTitle(opener, title);

		if (spellData.caster() != null) playSpellEffects(spellData.caster(), spellData.target(), spellData);
		else playSpellEffects(EffectPosition.TARGET, spellData.target(), spellData);

		MenuData newMenuData = new MenuData(spellData, page);
		menuData.put(opener.getUniqueId(), newMenuData);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		menuData.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onItemClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (!Util.getStringFromComponent(event.getView().title()).equals(internalName)) return;
		event.setCancelled(true);
		ItemStack item = event.getCurrentItem();
		if (item == null) return;
		ItemMeta itemMeta = item.getItemMeta();

		MenuData data = menuData.get(player.getUniqueId());
		SpellData spellData = data.spellData();

		if (event.getRawSlot() == 52) {
			open(player, data, data.page() - 1);
			return;
		} else if (event.getRawSlot() == 53) {
			open(player, data, data.page() + 1);
			return;
		}

		SkullMeta skullMeta = (SkullMeta) itemMeta;
		if (skullMeta == null) return;
		OfflinePlayer target = skullMeta.getOwningPlayer();
		float power = spellData == null ? 1f : spellData.power();
		if (target == null || !target.isOnline()) {
			itemMeta.displayName(translate(player, null, skullNameOffline));
			if (spellOffline != null) spellOffline.cast(player, power);
			if (stayOpen) item.setItemMeta(itemMeta);
			else {
				player.closeInventory();
				menuData.remove(player.getUniqueId());
				playSpellEffects(EffectPosition.DISABLED, player, spellData);
			}
			return;
		} else {
			itemMeta.displayName(translate(player, (Player) target, skullName));
			item.setItemMeta(itemMeta);
		}
		Player targetPlayer = (Player) target;
		if (radius > 0  && targetPlayer.getLocation().distance(player.getLocation()) > radius) {
			itemMeta.displayName(translate(player, targetPlayer, skullNameRadius));
			if (spellRange != null) spellRange.cast(player, power);
			if (stayOpen) item.setItemMeta(itemMeta);
			else {
				player.closeInventory();
				menuData.remove(player.getUniqueId());
				playSpellEffects(EffectPosition.DISABLED, player, spellData);
			}
			return;
		}
		switch (event.getClick()) {
			case LEFT -> processClickSpell(spellOnLeft, player, targetPlayer, power);
			case RIGHT -> processClickSpell(spellOnRight, player, targetPlayer, power);
			case MIDDLE -> processClickSpell(spellOnMiddle, player, targetPlayer, power);
			case SHIFT_LEFT -> processClickSpell(spellOnSneakLeft, player, targetPlayer, power);
			case SHIFT_RIGHT -> processClickSpell(spellOnSneakRight, player, targetPlayer, power);
		}
		if (variableTarget != null && !variableTarget.isEmpty() && MagicSpells.getVariableManager().getVariable(variableTarget) != null) {
			MagicSpells.getVariableManager().set(variableTarget, player, target.getName());
		}
		if (stayOpen) open(player, data, data.page());
		else {
			player.closeInventory();
			menuData.remove(player.getUniqueId());
			playSpellEffects(EffectPosition.DISABLED, player, spellData);
		}
	}

	public record MenuData(SpellData spellData, int page) {

	}
}
