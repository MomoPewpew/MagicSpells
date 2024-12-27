package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class BuildSpell extends TargetedSpell implements TargetedLocationSpell {

	private Set<Material> allowedTypes;

	private String strCantBuild;
	private String strInvalidBlock;

	private ConfigData<Integer> slot;

	private boolean consumeBlock;
	private boolean checkPlugins;
	private boolean playBreakEffect;

	public BuildSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strCantBuild = getConfigString("str-cant-build", "You can't build there.");
		strInvalidBlock = getConfigString("str-invalid-block", "You can't build that block.");

		slot = getConfigDataInt("slot", 0);

		consumeBlock = getConfigBoolean("consume-block", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("show-effect", true);

		List<String> materials = getConfigStringList("allowed-types", null);
		if (materials == null) {
			materials = new ArrayList<>();
			materials.add("GRASS_BLOCK");
			materials.add("STONE");
			materials.add("DIRT");
		}

		allowedTypes = new HashSet<>();
		for (String str : materials) {
			Material material = Util.getMaterial(str);
			if (material == null) {
				MagicSpells.error("BuildSpell '" + internalName + "' has an invalid material '" + str + "' defined!");
				continue;
			}
			if (!material.isBlock()) {
				MagicSpells.error("BuildSpell '" + internalName + "' has a non block material '" + str + "' defined!");
				continue;
			}

			allowedTypes.add(material);
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			int slot = this.slot.get(data);
			ItemStack item = player.getInventory().getItem(slot);
			if (item == null || !isAllowed(item.getType())) return noTarget(data, strInvalidBlock);

			List<Block> lastBlocks;
			try {
				lastBlocks = getLastTwoTargetedBlocks(data);
			} catch (IllegalStateException e) {
				DebugHandler.debugIllegalState(e);
				lastBlocks = null;
			}

			if (lastBlocks == null || lastBlocks.size() < 2 || BlockUtils.isAir(lastBlocks.get(1).getType()))
				return noTarget(data, strCantBuild);

			boolean built = build(data, lastBlocks.get(0), lastBlocks.get(1), item, slot);
			if (!built) return noTarget(data, strCantBuild);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		if (!(data.caster() instanceof Player player)) return false;

		int slot = this.slot.get(data);
		ItemStack item = player.getInventory().getItem(slot);
		if (item == null || !isAllowed(item.getType())) return false;

		Block block = data.location().getBlock();

		return build(data, block, block, item, slot);
	}

	private boolean isAllowed(Material mat) {
		return mat.isBlock() && allowedTypes != null && allowedTypes.contains(mat);
	}

	private boolean build(SpellData data, Block block, Block against, ItemStack item, int slot) {
		Player player = (Player) data.caster();
		BlockState previousState = block.getState();
		block.setType(item.getType());

		if (checkPlugins) {
			MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, previousState, against, player.getEquipment().getItemInMainHand(), player, true, bypassDippGen);
			EventUtil.call(event);
			if (event.isCancelled() && block.getType() == item.getType()) {
				previousState.update(true);
				return false;
			}
		}

		if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());

		playSpellEffects(data.builder().location(block.getLocation()).build());

		if (consumeBlock) {
			int amt = item.getAmount() - 1;
			if (amt > 0) {
				item.setAmount(amt);
				player.getInventory().setItem(slot, item);
			} else player.getInventory().setItem(slot, null);
		}

		return true;
	}

}
