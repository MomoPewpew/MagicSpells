package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ReplaceBlockDataSpell extends TargetedSpell implements TargetedLocationSpell {

	private boolean pointBlank;
	private boolean powerAffectsRadius;
	private boolean circleShape;

	private ConfigData<Integer> yOffset;
	private ConfigData<Integer> radiusUp;
	private ConfigData<Integer> radiusDown;
	private ConfigData<Integer> radiusHoriz;

	private List<String> replace;
	private List<String> replaceWith;

	public ReplaceBlockDataSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		replace = new ArrayList<>();
		replaceWith = new ArrayList<>();

		yOffset = getConfigDataInt("y-offset", 0);
		radiusUp = getConfigDataInt("radius-up", 0);
		radiusDown = getConfigDataInt("radius-down", 0);
		radiusHoriz = getConfigDataInt("radius-horiz", 0);

		pointBlank = getConfigBoolean("point-blank", false);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);
		circleShape = getConfigBoolean("circle-shape", false);

		replace = getConfigStringList("replace", null);
		replaceWith = getConfigStringList("replace-with", null);

		if (replace == null) MagicSpells.error("ReplaceBlockDataSpell " + internalName + " has an empty replace list");
		if (replace != null && (replaceWith == null || replace.size() != replaceWith.size())) MagicSpells.error("ReplaceBlockDataSpell " + internalName + " replace-with list is not the same size as the replace list");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pointBlank ? caster.getLocation().getBlock() : getTargetedBlock(caster, power, args);
			if (target == null) return noTarget(caster, args);
			replaceBlockData(caster, target.getLocation(), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return replaceBlockData(caster, target, power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return replaceBlockData(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return replaceBlockData(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return replaceBlockData(null, target, power, null);
	}

	private boolean replaceBlockData(LivingEntity caster, Location target, float power, String[] args) {
		if (replace == null || replaceWith == null) return false;

		boolean replaced = false;
		Block block;

		int d = radiusDown.get(caster, null, power, args);
		int u = radiusUp.get(caster, null, power, args);
		int h = radiusHoriz.get(caster, null, power, args);
		if (powerAffectsRadius) {
			d = Math.round(d * power);
			u = Math.round(u * power);
			h = Math.round(h * power);
		}

		SpellData spellData = new SpellData(caster, power, args);
		int yOffset = this.yOffset.get(caster, null, power, args);

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

					String blockDataString = block.getBlockData().getAsString();
					boolean contains = false;

					for (int i = 0; i < replace.size(); i++) {
						if (blockDataString.contains(replace.get(i)) && replaceWith.size() > i) {
							blockDataString = blockDataString.replace(replace.get(i), replaceWith.get(i));
							contains = true;
						}
					}

					if (contains) {
						BlockData blockData = Bukkit.createBlockData(blockDataString);

						block.setBlockData(blockData);

						playSpellEffects(EffectPosition.SPECIAL, block.getLocation(), spellData);

						replaced = true;
					}
				}
			}
		}

		return replaced;
	}
	
}
