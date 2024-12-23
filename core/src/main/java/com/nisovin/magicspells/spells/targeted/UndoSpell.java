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

public class UndoSpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Integer> radius;

	private boolean pointBlank;
	private boolean powerAffectsRadius;
	private boolean applyPhysics;

	private List<String> spellNames;
	private List<Spell> spells;

	public UndoSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataInt("radius", 10);
		pointBlank = getConfigBoolean("point-blank", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		applyPhysics = getConfigBoolean("apply-physics", true);
		spellNames = getConfigStringList("spells", null);
		spells = new ArrayList<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (spellNames != null) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell instanceof ReplaceSpell || spell instanceof DestroySpell) {
					spells.add(spell);
				} else {
					MagicSpells.error(
							"UndoSpell '" + internalName + "' has an invalid spell defined in spells!");
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
			undo(data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		undo(data);
		return true;
	}

	private void undo(SpellData data) {
		int rad = radius.get(data);
		if (powerAffectsRadius)
			rad *= data.power();

		Location loc = data.location();
		World locWorld = loc.getWorld();
		if (locWorld == null) {
			MagicSpells.error("Location world is null for UndoSpell.");
			return;
		}

		List<Spell> spellsTemp = spells.isEmpty()
				? MagicSpells.spells().stream()
						.filter(spell -> spell instanceof ReplaceSpell || spell instanceof DestroySpell)
						.toList()
				: new ArrayList<>(spells);

		for (Spell spell : spellsTemp) {
			for (int y = loc.getBlockY() - rad; y <= loc.getBlockY() + rad; y++) {
				for (int x = loc.getBlockX() - rad; x <= loc.getBlockX() + rad; x++) {
					for (int z = loc.getBlockZ() - rad; z <= loc.getBlockZ() + rad; z++) {
						Block block = loc.getWorld().getBlockAt(x, y, z);
						MagicSpells.getAlteredBlockManager().getByBlockAndInternalName(block, spell.getInternalName()).forEach(it -> {
							it.undo(applyPhysics);
							playSpellEffects(EffectPosition.TARGET, block.getLocation(), data);
						});
					}
				}
			}
		}

		if (data.caster() != null)
			playSpellEffects(EffectPosition.CASTER, data.caster().getLocation(), data);

		playSpellEffects(EffectPosition.SPECIAL, loc, data);
	}
}