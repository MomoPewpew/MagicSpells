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

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.SpellData;

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
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Location loc = pointBlank ? data.caster().getLocation() : getTargetedBlock(data.caster(), data.power(), data.args()).getLocation();
			if (loc == null) {
				return noTarget(data);
			}
			undoReplaces(data.builder().location(loc).build());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		undoReplaces(data);
		return true;
	}

	private void undoReplaces(SpellData data) {
		Location loc = data.location();
		float radSq = radius.get(data);
		if (powerAffectsRadius)
			radSq *= data.power();
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
				playSpellEffects(EffectPosition.TARGET, block.getLocation(), data);
			}
		}

		if (data.caster() != null)
			playSpellEffects(EffectPosition.CASTER, data.caster().getLocation(), data);

		playSpellEffects(EffectPosition.SPECIAL, loc, data);
	}
}