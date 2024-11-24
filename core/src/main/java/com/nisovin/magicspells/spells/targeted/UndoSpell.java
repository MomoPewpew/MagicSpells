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
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = pointBlank ? caster.getLocation() : getTargetedBlock(caster, power, args).getLocation();
			if (loc == null) {
				return noTarget(caster, args);
			}
			undo(caster, loc, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		undo(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		undo(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		undo(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		undo(null, target, power, null);
		return true;
	}

	private void undo(LivingEntity caster, Location loc, float power, String[] args) {
		int rad = radius.get(caster, null, power, args);
		if (powerAffectsRadius)
			rad *= power;

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
							playSpellEffects(EffectPosition.TARGET, block.getLocation(), power, args);
						});
					}
				}
			}
		}

		if (caster != null)
			playSpellEffects(EffectPosition.CASTER, caster.getLocation(), power, args);

		playSpellEffects(EffectPosition.SPECIAL, loc, power, args);
	}
}