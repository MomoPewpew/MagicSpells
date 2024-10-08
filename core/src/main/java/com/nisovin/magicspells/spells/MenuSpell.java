package com.nisovin.magicspells.spells;

import java.util.*;

import co.aikar.commands.ACFUtil;

import com.nisovin.magicspells.util.config.ConfigData;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class MenuSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private final Map<String, MenuOption> options = new LinkedHashMap<>();
	private final Map<UUID, MenuData> menuData = new HashMap<>();

	private int size;

	private final String title;
	private final int delay;
	private final ItemStack filler;
	private final boolean stayOpenNonOption;
	private final boolean bypassNormalCast;
	private final boolean requireEntityTarget;
	private final boolean requireLocationTarget;
	private final boolean targetOpensMenuInstead;

	public MenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		title = getConfigString("title", "Window Title " + spellName);
		delay = getConfigInt("delay", 0);
		filler = createItem("filler");
		stayOpenNonOption = getConfigBoolean("stay-open-non-option", false);
		bypassNormalCast = getConfigBoolean("bypass-normal-cast", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		requireLocationTarget = getConfigBoolean("require-location-target", false);
		targetOpensMenuInstead = getConfigBoolean("target-opens-menu-instead", false);

		Set<String> optionKeys = getConfigKeys("options");
		if (optionKeys == null) {
			MagicSpells.error("MenuSpell '" + spellName + "' has no menu options!");
			return;
		}
		int maxSlot = (getConfigInt("min-rows", 1) * 9) - 1;
		for (String optionName : optionKeys) {
            String path = "options." + optionName + ".";

            List<Integer> slots = getConfigIntList(path + "slots", new ArrayList<>());
            if (slots.isEmpty()) slots.add(getConfigInt(path + "slot", -1));

            List<Integer> validSlots = new ArrayList<>();
            for (int slot : slots) {
                if (slot < 0 || slot > 53) {
                    MagicSpells.error("MenuSpell '" + internalName + "' a slot defined which is out of bounds for '" + optionName + "': " + slot);
                    continue;
                }
                validSlots.add(slot);
                if (slot > maxSlot) maxSlot = slot;
            }
            if (validSlots.isEmpty()) {
                MagicSpells.error("MenuSpell '" + internalName + "' has no slots defined for: " + optionName);
                continue;
            }

            ConfigData<ConfigurationSection> itemSection = null;
			ConfigData<String> itemString = null;
            if (isConfigSection(path + "item")) {
                itemSection = getConfigDataConfigurationSection(path + "item", null);
            } else {
                itemString = getConfigDataString(path + "item", null);
            }

            List<String> itemList = getConfigStringList(path + "items", null);
            List<ItemStack> items = new ArrayList<>();
            if (itemString == null && itemSection == null) {
                // If no items are defined, exit.
                if (itemList == null) {
                    MagicSpells.error("MenuSpell '" + internalName + "' has no items defined for: " + optionName);
                    continue;
                }
                // Otherwise process item list.
                for (String itemName : itemList) {
                    MagicItem magicItem = MagicItems.getMagicItemFromString(itemName);
                    if (magicItem == null) {
                        MagicSpells.error("MenuSpell '" + internalName + "' has an invalid item listed in '" + optionName + "': " + itemName);
                        continue;
                    }
                    ItemStack itemStack = magicItem.getItemStack();
                    if (itemStack == null) {
                        MagicSpells.error("MenuSpell '" + internalName + "' has an invalid item listed in '" + optionName + "': " + itemName);
                        continue;
                    }
                    items.add(itemStack);
                }
                // Skip if list was invalid.
                if (items.isEmpty()) {
                    MagicSpells.error("MenuSpell '" + internalName + "' has no items defined for: " + optionName);
                    continue;
                }
            }

            MenuOption option = new MenuOption();
            option.menuOptionName = optionName;
            option.slots = validSlots;
            option.itemSection = itemSection;
            option.itemString = itemString;
            option.items = items;
            option.quantity = getConfigString(path + "quantity", "");
            option.spellName = getConfigString(path + "spell", "");
            option.spellRightName = getConfigString(path + "spell-right", "");
            option.spellMiddleName = getConfigString(path + "spell-middle", "");
            option.spellSneakLeftName = getConfigString(path + "spell-sneak-left", "");
            option.spellSneakRightName = getConfigString(path + "spell-sneak-right", "");
            option.power = getConfigFloat(path + "power", 1);
            option.modifierList = getConfigStringList(path + "modifiers", null);
            option.stayOpen = getConfigBoolean(path + "stay-open", false);
			option.varModsClick = getConfigStringList(path + "variable-mods-click", null);
			option.varModsClicked = getConfigStringList(path + "variable-mods-clicked", null);

            options.put(optionName, option);
        }
		size = (int) Math.ceil((maxSlot+1) / 9.0) * 9;
		if (options.isEmpty()) MagicSpells.error("MenuSpell '" + spellName + "' has no menu options!");
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		for (MenuOption option : options.values()) {
			if (option.modifierList != null) option.menuOptionModifiers = new ModifierSet(option.modifierList, this);
		}
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		for (MenuOption option : options.values()) {
			if (option.varModsClick != null && !option.varModsClick.isEmpty()) {
				option.variableModsClick = LinkedListMultimap.create();
				for (String s : option.varModsClick) {
					try {
						String[] data = s.split(" ", 2);
						String var = data[0];
						VariableMod varMod = new VariableMod(data[1]);
						option.variableModsClick.put(var, varMod);
					} catch (Exception e) {
						MagicSpells.error("Invalid variable-mods-click option for MenuSpell '" + internalName + "': " + s);
					}
				}
			}
			
			if (option.varModsClicked != null && !option.varModsClicked.isEmpty()) {
				option.variableModsClicked = LinkedListMultimap.create();
				for (String s : option.varModsClicked) {
					try {
						String[] data = s.split(" ", 2);
						String var = data[0];
						VariableMod varMod = new VariableMod(data[1]);
						option.variableModsClicked.put(var, varMod);
					} catch (Exception e) {
						MagicSpells.error("Invalid variable-mods-clicked option for MenuSpell '" + internalName + "': " + s);
					}
				}
			}
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		for (MenuOption option : options.values()) {
			option.spell = initSubspell(option.spellName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
			option.spellRight = initSubspell(option.spellRightName, "MenuSpell '" + internalName + "' has an invalid 'spell-right' defined for: " + option.menuOptionName);
			option.spellMiddle = initSubspell(option.spellMiddleName, "MenuSpell '" + internalName + "' has an invalid 'spell-middle' defined for: " + option.menuOptionName);
			option.spellSneakLeft = initSubspell(option.spellSneakLeftName, "MenuSpell '" + internalName + "' has an invalid 'spell-sneak-left' defined for: " + option.menuOptionName);
			option.spellSneakRight = initSubspell(option.spellSneakRightName, "MenuSpell '" + internalName + "' has an invalid 'spell-sneak-right' defined for: " + option.menuOptionName);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			LivingEntity target = null;
			Location locTarget = null;
			Player opener = player;

			if (requireEntityTarget) {
				if (targetOpensMenuInstead) {
					TargetInfo<Player> info = getTargetedPlayer(player, power, args);
					if (info.noTarget()) return noTarget(caster, args, info);

					opener = info.target();
					power = info.power();
				} else {
					TargetInfo<LivingEntity> info = getTargetedEntity(player, power, args);
					if (info.noTarget()) return noTarget(caster, args, info);

					target = info.target();
					power = info.power();
				}
			} else if (requireLocationTarget) {
				Block block = getTargetedBlock(player, power, args);
				if (block == null || BlockUtils.isAir(block.getType())) return noTarget(caster, args);

				locTarget = block.getLocation();
			}

			open(player, opener, target, locTarget, power, args);

			if (requireEntityTarget) {
				sendMessages(caster, targetOpensMenuInstead ? opener : target, args);
				return PostCastAction.NO_MESSAGES;
			}
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		if (!(caster instanceof Player opener)) return false;
		if (targetOpensMenuInstead) {
			if (!(target instanceof Player player)) return false;
			opener = player;
			target = null;
		}
		open((Player) caster, opener, target, null, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!targetOpensMenuInstead) return false;
		if (!validTargetList.canTarget(target)) return false;
		if (!(target instanceof Player player)) return false;
		open(null, player, null, null, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		if (!(caster instanceof Player player)) return false;
		open(player, player, null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;
		Player player = PlayerNameUtils.getPlayer(args[0]);
		String[] spellArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null;
		if (player != null) {
			open(null, player, null, null, 1, spellArgs);
			return true;
		}
		return false;
	}

	private ItemStack createItem(String path) {
		ItemStack item = null;
		if (isConfigSection(path)) {
			MagicItem magicItem = MagicItems.getMagicItemFromSection(getConfigSection(path));
			if (magicItem != null) item = magicItem.getItemStack();
		} else {
			MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString(path, ""));
			if (magicItem != null) item = magicItem.getItemStack();
		}
		return item;
	}

	private void open(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		if (delay < 0) {
			openMenu(caster, opener, entityTarget, locTarget, power, args);
			return;
		}
		MagicSpells.scheduleDelayedTask(() -> openMenu(caster, opener, entityTarget, locTarget, power, args), delay);
	}

	private void openMenu(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		menuData.put(opener.getUniqueId(), new MenuData(requireEntityTarget ? entityTarget : null, requireLocationTarget ? locTarget : null, power, args));

		Inventory inv = Bukkit.createInventory(opener, size, Component.text(internalName));
		applyOptionsToInventory(opener, inv, args);
		opener.openInventory(inv);
		Util.setInventoryTitle(opener, title);

		SpellData data = new SpellData(caster, entityTarget, power, args);
		if (entityTarget != null && caster != null) {
			playSpellEffects(caster, entityTarget, data);
			return;
		}
		playSpellEffects(EffectPosition.SPECIAL, opener, data);
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster, data);
		if (locTarget != null) playSpellEffects(EffectPosition.TARGET, locTarget, data);
	}

	private void applyOptionsToInventory(Player opener, Inventory inv, String[] args) {
		// Setup option items.
		for (MenuOption option : options.values()) {
			// Check modifiers.
			if (option.menuOptionModifiers != null) {
				MagicSpellsGenericPlayerEvent event = new MagicSpellsGenericPlayerEvent(opener);
				option.menuOptionModifiers.apply(event);
				if (event.isCancelled()) continue;
			}
			// Select and finalise item to display.
			SpellData spellData = new SpellData(opener, 0f, args);
			ItemStack itemData = null;
			if (option.itemSection != null) {
				MagicItem magicItem = MagicItems.getMagicItemFromSection(option.itemSection.get(spellData));
				if (magicItem != null) itemData = magicItem.getItemStack();
			}
			else if (option.itemString != null) {
				MagicItem magicItem = MagicItems.getMagicItemFromString(option.itemString.get(spellData));
				if (magicItem != null) itemData = magicItem.getItemStack();
			}

			option.item = itemData;

			if (option.item == null && option.items.isEmpty()) {
				MagicSpells.error("MenuSpell '" + internalName + "' has invalid items defined for: " + option.menuOptionName);
				continue;
			}

			ItemStack item = (option.item != null ? option.item : option.items.get(Util.getRandomInt(option.items.size()))).clone();
			DataUtil.setString(item, "menuOption", option.menuOptionName);
			item = translateItem(opener, args, item);

			int quantity;
			Variable variable = MagicSpells.getVariableManager().getVariable(option.quantity);
			if (variable == null) quantity = ACFUtil.parseInt(option.quantity, 1);
			else quantity = (int) Math.round(variable.getValue(opener));
			item.setAmount(quantity);

			// Set item for all defined slots.
			for (int slot : option.slots) {
				if (inv.getItem(slot) == null) inv.setItem(slot, item);
			}
		}
		// Fill inventory.
		if (filler == null) return;
		ItemStack item = translateItem(opener, args, filler);
		for (int i = 0; i < inv.getSize(); i++) {
			if (inv.getItem(i) != null) continue;
			inv.setItem(i, item);
		}
	}

	private Component translateRawComponent(Component component, Player player, String[] args) {
		String text = Util.getStringFromComponent(component);
		text = MagicSpells.doReplacements(text, player, args);
		return Util.getMiniMessage(text);
	}

	private ItemStack translateItem(Player opener, String[] args, ItemStack item) {
		ItemStack newItem = item.clone();
		ItemMeta meta = newItem.getItemMeta();
		if (meta == null) return newItem;
		meta.displayName(translateRawComponent(meta.displayName(), opener, args));
		List<Component> lore = meta.lore();
		if (lore != null) {
			for (int i = 0; i < lore.size(); i++) {
				lore.set(i, translateRawComponent(lore.get(i), opener, args));
			}
			meta.lore(lore);
		}
		newItem.setItemMeta(meta);
		return newItem;
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (!Util.getStringFromComponent(event.getView().title()).equals(internalName)) return;
		event.setCancelled(true);

		String closeState = castSpells(player, event.getCurrentItem(), event.getClick());

		UUID id = player.getUniqueId();

		if (closeState.equals("ignore")) return;
		if (closeState.equals("close")) {
			menuData.remove(id);
			MagicSpells.scheduleDelayedTask(player::closeInventory, 0);
			return;
		}
		// Reopen.
		Inventory newInv = Bukkit.createInventory(player, event.getView().getTopInventory().getSize(), Component.text(internalName));
		applyOptionsToInventory(player, newInv, MagicSpells.NULL_ARGS);
		player.openInventory(newInv);
		Util.setInventoryTitle(player, title);
	}

	private String castSpells(Player player, ItemStack item, ClickType click) {
		// Outside inventory.
		if (item == null) return stayOpenNonOption ? "ignore" : "close";
		String key = DataUtil.getString(item, "menuOption");
		// Probably a filler or air.
		if (key == null || key.isEmpty() || !options.containsKey(key)) return stayOpenNonOption ? "ignore" : "close";
		MenuOption option = options.get(key);
		if (option == null) return "close";
		return switch (click) {
			case LEFT -> processClickSpell(player, option.spell, option);
			case RIGHT -> processClickSpell(player, option.spellRight, option);
			case MIDDLE -> processClickSpell(player, option.spellMiddle, option);
			case SHIFT_LEFT -> processClickSpell(player, option.spellSneakLeft, option);
			case SHIFT_RIGHT -> processClickSpell(player, option.spellSneakRight, option);
			default -> option.stayOpen ? "ignore" : "close";
		};
	}

	private String processClickSpell(Player player, Subspell spell, MenuOption option) {
		LivingEntity entityTarget = null;
		Location locationTarget = null;
		float power = option.power;
		String[] args = null;

		UUID id = player.getUniqueId();
		MenuData data = menuData.get(id);
		if (data != null) {
			locationTarget = data.targetLocation;
			entityTarget = data.targetEntity;
			power *= data.power;
			args = data.args;
		}

		processVariables(option.variableModsClick, player, data);

		if (spell == null) return option.stayOpen ? "ignore" : "close";

		boolean success;

		if (entityTarget != null) success = spell.subcast(player, entityTarget, power, args);
		else if (locationTarget != null) success = spell.subcast(player, locationTarget, power, args);
		else if (bypassNormalCast) success = spell.subcast(player, power, args);
		else {
			SpellCastResult result = spell.getSpell().cast(player, power, MagicSpells.NULL_ARGS);
			success = result.state.equals(SpellCastState.NORMAL) && !result.action.equals(PostCastAction.ALREADY_HANDLED);
		}

		if (success) processVariables(option.variableModsClicked, player, data);

		return option.stayOpen ? "reopen" : "close";
	}

	private void processVariables(Multimap<String, VariableMod> varMods, Player player, MenuData data) {
		if (varMods == null || varMods.isEmpty()) return;

		for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
			VariableMod mod = entry.getValue();
			if (mod == null) continue;

			Variable variable = MagicSpells.getVariableManager().getVariable(entry.getKey());

			String amount = MagicSpells.getVariableManager().processVariableMods(variable, mod, player, player, null, data.power(), data.args());
			MagicSpells.debug(3, "Variable '" + entry.getKey() + "' for player '" + player.getName() + "' modified by " + amount + " as a result of spell cast '" + internalName + "'");
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID id = event.getPlayer().getUniqueId();
		menuData.remove(id);
	}

	private record MenuData(LivingEntity targetEntity, Location targetLocation, float power, String[] args) {
	}

	private static class MenuOption {
		private String menuOptionName;
		private List<Integer> slots;
		private ConfigData<ConfigurationSection> itemSection;
		private ConfigData<String> itemString;
		private ItemStack item;
		private List<ItemStack> items;
		private String quantity;
		private String spellName;
		private String spellRightName;
		private String spellMiddleName;
		private String spellSneakLeftName;
		private String spellSneakRightName;
		private Subspell spell;
		private Subspell spellRight;
		private Subspell spellMiddle;
		private Subspell spellSneakLeft;
		private Subspell spellSneakRight;
		private float power;
		private List<String> modifierList;
		private ModifierSet menuOptionModifiers;
		private boolean stayOpen;
		private List<String> varModsClick;
		private List<String> varModsClicked;
		protected Multimap<String, VariableMod> variableModsClick;
		protected Multimap<String, VariableMod> variableModsClicked;
	}

}
