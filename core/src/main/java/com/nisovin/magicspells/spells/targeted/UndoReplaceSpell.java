package com.nisovin.magicspells.spells.targeted;

import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class UndoReplaceSpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Float> radius;

	private boolean pointBlank;
	private boolean powerAffectsRadius;
	private boolean applyPhysics;

	private List<String> replaceSpellNames;
	private List<ReplaceSpell> replaceSpells;

	public UndoReplaceSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataFloat("radius", 10F);
		pointBlank = getConfigBoolean("point-blank", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		applyPhysics = getConfigBoolean("apply-physics", true);
		replaceSpellNames = getConfigStringList("replace-spells", null);
		replaceSpells = new ArrayList<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (replaceSpellNames != null) {
			for (String replaceSpellName : replaceSpellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(replaceSpellName);
				if (spell instanceof ReplaceSpell) {
					replaceSpells.add((ReplaceSpell) spell);
				} else {
					MagicSpells.error(
							"UndoReplaceSpell '" + internalName + "' has an invalid spell defined in replace-spells!");
					return;
				}
			}
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = pointBlank ? caster.getLocation() : getTargetedBlock(caster, power, args).getLocation();
			if (loc == null) {
				return noTarget(caster, args);
			}
			undoReplaces(caster, loc, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		undoReplaces(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		undoReplaces(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		undoReplaces(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		undoReplaces(null, target, power, null);
		return true;
	}

	private void undoReplaces(LivingEntity caster, Location loc, float power, String[] args) {
		float radSq = radius.get(caster, null, power, args);
		if (powerAffectsRadius)
			radSq *= power;
		radSq *= radSq;

		World locWorld = loc.getWorld();
		if (locWorld == null) {
			MagicSpells.error("Location world is null for UndoReplaceSpell.");
			return;
		}

		List<ReplaceSpell> replaceSpellsTemp = replaceSpells.isEmpty()
				? MagicSpells.spells().stream()
						.filter(ReplaceSpell.class::isInstance)
						.map(ReplaceSpell.class::cast)
						.collect(Collectors.toList())
				: new ArrayList<>(replaceSpells);

		for (ReplaceSpell replaceSpell : replaceSpellsTemp) {
			Iterator<Entry<Block, BlockData>> iterator = replaceSpell.blocks.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Block, BlockData> entry = iterator.next();
				Block block = entry.getKey();
				BlockData blockData = entry.getValue();

				if (!block.getWorld().equals(locWorld))
					continue;
				if (block.getLocation().distanceSquared(loc) > radSq)
					continue;

				block.setBlockData(blockData, applyPhysics);
				iterator.remove();
				playSpellEffects(EffectPosition.TARGET, block.getLocation(), power, args);
			}
		}

		if (caster != null)
			playSpellEffects(EffectPosition.CASTER, caster.getLocation(), power, args);

		playSpellEffects(EffectPosition.SPECIAL, loc, power, args);
	}
}