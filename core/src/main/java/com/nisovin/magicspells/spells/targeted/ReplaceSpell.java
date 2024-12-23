package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.nisovin.magicspells.util.managers.AlteredBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class ReplaceSpell extends TargetedSpell implements TargetedLocationSpell {

	private boolean replaceAll;
	private List<List<BlockData>> replace;
	private List<List<BlockData>> replaceWith;
	private List<BlockData> replaceBlacklist;

	private ConfigData<Integer> yOffset;
	private ConfigData<Integer> radiusUp;
	private ConfigData<Integer> radiusDown;
	private ConfigData<Integer> radiusHoriz;
	private ConfigData<Integer> replaceDuration;

	private boolean pointBlank;
	private boolean replaceRandom;
	private boolean powerAffectsRadius;
	private final boolean checkPlugins;
	private final boolean applyPhysics;
	private boolean resolveDurationPerBlock;
	private boolean circleShape;
	private boolean mergeBlockData;
	private boolean affectsContainers;

	public ReplaceSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		replace = new ArrayList<>();
		replaceWith = new ArrayList<>();
		replaceBlacklist = new ArrayList<>();

		yOffset = getConfigDataInt("y-offset", 0);
		radiusUp = getConfigDataInt("radius-up", 1);
		radiusDown = getConfigDataInt("radius-down", 1);
		radiusHoriz = getConfigDataInt("radius-horiz", 1);
		replaceDuration = getConfigDataInt("duration", 0);

		pointBlank = getConfigBoolean("point-blank", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		applyPhysics = getConfigBoolean("apply-physics", false);
		replaceRandom = getConfigBoolean("replace-random", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);
		resolveDurationPerBlock = getConfigBoolean("resolve-duration-per-block", false);
		circleShape = getConfigBoolean("circle-shape", false);
		mergeBlockData = getConfigBoolean("merge-block-data", true);
		affectsContainers = getConfigBoolean("affects-containers", false);

		List<String> list = getConfigStringList("replace-blocks", null);
		if (list != null) {
			replaceAll = false;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).equals("all")) {
					replaceAll = true;
					// Just a filler.
					replace.add(null);
					break;
				}

				List<BlockData> blockList = new ArrayList<BlockData>();
				String[] split = list.get(i).split("\\|");
				for (String block : split) {
					try {
						BlockData data = Bukkit.createBlockData(block.trim().toLowerCase());
						blockList.add(data);
					} catch (IllegalArgumentException e) {
						MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-blocks item: " + block);
					}
				}
				replace.add(blockList);
			}
		}

		list = getConfigStringList("replace-with", null);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				List<BlockData> blockList = new ArrayList<BlockData>();
				String[] split = list.get(i).split("\\|");

				for (String block : split) {
					try {
						int n = 1;

						String[] blockSplit = block.split("%");
						String blockName = null;

						if (blockSplit.length == 2) {
							n = Integer.valueOf(blockSplit[0]);
							blockName = blockSplit[1];
						} else {
							blockName = blockSplit[0];
						}

						BlockData data = null;
						
						if (!blockName.equals("same")) data = Bukkit.createBlockData(blockName.trim().toLowerCase());

						for (int j = 0; j < n; j++) {
							blockList.add(data);
						}
					} catch (IllegalArgumentException e) {
						MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-with item: " + block);
					}
				}
				replaceWith.add(blockList);
			}
		}

		list = getConfigStringList("replace-blacklist", null);
		if (list != null) {
			for (String s : list) {
				try {
					BlockData data = Bukkit.createBlockData(s.trim().toLowerCase());
					replaceBlacklist.add(data);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-blacklist item: " + s);
				}
			}
		}

		if (!replaceRandom && replace.size() != replaceWith.size()) {
			replaceRandom = true;
			MagicSpells.error("ReplaceSpell " + internalName + " replace-random false, but replace-blocks and replace-with have different sizes!");
		}

		if (replace.isEmpty()) MagicSpells.error("ReplaceSpell " + internalName + " has empty replace-blocks list!");
		if (replaceWith.isEmpty()) MagicSpells.error("ReplaceSpell " + internalName + " has empty replace-with list!");
	}

	@Override
	public void turnOff() {
		for (AlteredBlockManager.Change change : MagicSpells.getAlteredBlockManager().getByInternalName(internalName)) {
			change.undo(applyPhysics);
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Block target = pointBlank ? data.caster().getLocation().getBlock() : getTargetedBlock(data.caster(), data.power());
			if (target == null) return noTarget(data);
			replace(data.builder().location(target.getLocation()).build());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		return replace(data);
	}

	private boolean replace(SpellData data) {
		boolean replaced = false;
		Block block;

		int d = radiusDown.get(data);
		int u = radiusUp.get(data);
		int h = radiusHoriz.get(data);
		if (powerAffectsRadius) {
			d = Math.round(d * data.power());
			u = Math.round(u * data.power());
			h = Math.round(h * data.power());
		}

		int yOffset = this.yOffset.get(data);
		int replaceDuration = resolveDurationPerBlock ? 0 : this.replaceDuration.get(data);

		List<BlockData> allReplaceWithBlocks = new ArrayList<BlockData>();

		if (replaceRandom) {
			for (List<BlockData> blockList : replaceWith) {
				for (BlockData blockData : blockList) {
					allReplaceWithBlocks.add(blockData);
				}
			}
		}

		Location target = data.location();
		for (int y = target.getBlockY() - d + yOffset; y <= target.getBlockY() + u + yOffset; y++) {
			for (int x = target.getBlockX() - h; x <= target.getBlockX() + h; x++) {
				for (int z = target.getBlockZ() - h; z <= target.getBlockZ() + h; z++) {
					if (circleShape) {
						double hDistanceSq = NumberConversions.square(x - target.getBlockX()) + NumberConversions.square(z - target.getBlockZ());
						if (hDistanceSq > (h * h)) continue;
						double vDistance = NumberConversions.square(y - (target.getBlockY() + yOffset));
						if (y > target.getBlockY() + yOffset) {
							if (vDistance > (u * u)) continue;
						} else {
							if (vDistance > (d * d)) continue;
						}
					}

					block = target.getWorld().getBlockAt(x, y, z);
					for (int i = 0; i < replace.size(); i++) {
						BlockData bdata = block.getBlockData();

						// If specific blocks are being replaced, skip if the block isn't replaceable.
						if (!replaceAll) {
							Boolean cont = true;

							for (BlockData replaceData : replace.get(i)) {
								if (bdata.matches(replaceData)) {
									cont = false;
								}
							}
							if (cont) continue;
						}

						// If all blocks are being replaced, skip if the block is already replaced.
						if (replaceAll) {
							Boolean cont = false;

							for (BlockData replaceData : replaceWith.get(i)) {
								if (bdata.matches(replaceData)) {
									cont = true;
								}
							}
							if (cont || (!affectsContainers && BlockUtils.isContainer(block))) continue;
						}

						if (replaceBlacklisted(bdata)) continue;

						Block finalBlock = block;
						BlockState previousState = block.getState();

						// Place block.
						BlockData newBlockData = null;
						if (replaceRandom) newBlockData = allReplaceWithBlocks.get(Util.getRandomInt(allReplaceWithBlocks.size()));
						else newBlockData = replaceWith.get(i).get(Util.getRandomInt(replaceWith.get(i).size()));

						if (newBlockData == null) continue;

						if (!newBlockData.isSupported(block.getLocation())) continue;

						BlockUtils.setBlockData(block, bdata, newBlockData, mergeBlockData, applyPhysics);

						if (checkPlugins && data.caster() instanceof Player player) {
							Block against = data.location().clone().add(data.location().getDirection()).getBlock();
							if (block.equals(against)) against = block.getRelative(BlockFace.DOWN);
							MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, previousState, against, player.getInventory().getItemInMainHand(), player, true, bypassDippGen);
							EventUtil.call(event);
							if (event.isCancelled()) {
								previousState.update(true);
								return false;
							}
						}
						playSpellEffects(EffectPosition.SPECIAL, finalBlock.getLocation(), data);

						// Break block.
						if (resolveDurationPerBlock) replaceDuration = this.replaceDuration.get(data);
						if (replaceDuration > 0) {
							AlteredBlockManager.Change change = new AlteredBlockManager.Change(internalName, block, bdata);
							MagicSpells.getAlteredBlockManager().add(change);

							MagicSpells.scheduleDelayedTask(() -> {
								change.undo(applyPhysics);
								playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, finalBlock.getLocation(), data);
							}, replaceDuration);
						}

						replaced = true;
						break;
					}
				}
			}
		}

		if (data.caster() != null) playSpellEffects(data.caster(), target, data);
		else playSpellEffects(EffectPosition.TARGET, target, data);

		return replaced;
	}

	private boolean replaceBlacklisted(BlockData data) {
		for (BlockData blockData : replaceBlacklist)
			if (data.matches(blockData))
				return true;

		return false;
	}
}
