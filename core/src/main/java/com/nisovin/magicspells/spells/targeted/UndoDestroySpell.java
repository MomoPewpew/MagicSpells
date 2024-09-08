package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.targeted.DestroySpell.DestroyedBlock;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class UndoDestroySpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Float> radius;

	private boolean pointBlank;
	private boolean powerAffectsRadius;

	private String searchModeName;
	private SearchMode searchMode;

	private List<String> destroySpellNames;

	public UndoDestroySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataFloat("radius", 10F);
		pointBlank = getConfigBoolean("point-blank", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		searchModeName = getConfigString("search-mode", "both").toUpperCase();
		destroySpellNames = getConfigStringList("destroy-spells", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (destroySpellNames != null) {
			Iterator<String> iterator = destroySpellNames.iterator();
			while (iterator.hasNext()) {
				String destroySpellName = iterator.next();
				Spell spell = MagicSpells.getSpellByInternalName(destroySpellName);
				if (!(spell instanceof DestroySpell)) {
					MagicSpells.error(
							"UndoDestroySpell '" + internalName + "' has an invalid spell defined in Destroy-spells!");
					iterator.remove();
				}
			}
		}

		SearchMode sm;
		try {
			sm = SearchMode.valueOf(searchModeName);
		} catch (IllegalArgumentException e) {
			MagicSpells.error(
					"UndoDestroySpell '" + internalName + "' has an invalid search-mode. Defaulting to 'both'.");
			sm = SearchMode.BOTH;
		}
		searchMode = sm;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = pointBlank ? caster.getLocation() : getTargetedBlock(caster, power, args).getLocation();
			if (loc == null) {
				return noTarget(caster, args);
			}
			undoDestroys(caster, loc, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		undoDestroys(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		undoDestroys(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		undoDestroys(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		undoDestroys(null, target, power, null);
		return true;
	}

	private void undoDestroys(LivingEntity caster, Location loc, float power, String[] args) {
		float radSq = radius.get(caster, null, power, args);
		if (powerAffectsRadius)
			radSq *= power;
		radSq *= radSq;

		World locWorld = loc.getWorld();
		if (locWorld == null) {
			MagicSpells.error("Location world is null for UndoDestroySpell.");
			return;
		}

		Iterator<DestroyedBlock> iterator = DestroySpell.destroyedBlocks.iterator();
		while (iterator.hasNext()) {
			DestroyedBlock db = iterator.next();

			if (destroySpellNames != null && !destroySpellNames.isEmpty()
					&& !destroySpellNames.contains(db.spellInternalName))
				continue;

			Block source = db.sourceBlock;
			Block target = db.targetBlock;

			if ((source == null && target == null) ||
					((source != null && !source.getWorld().equals(locWorld))
							&& (target != null && !target.getWorld().equals(locWorld)))) {
				continue;
			}
			if ((searchMode == SearchMode.BOTH
					&& ((source != null && source.getLocation().distanceSquared(loc) < radSq)
							|| (target != null && target.getLocation().distanceSquared(loc) < radSq)))
					|| (searchMode == SearchMode.SOURCE
							&& (source != null && source.getLocation().distanceSquared(loc) < radSq))
					|| (searchMode == SearchMode.TARGET
							&& (target != null && target.getLocation().distanceSquared(loc) < radSq))) {

				if (db.undo(DestroySpell.destroyedBlocks) && db.targetBlock != null)
					playSpellEffects(EffectPosition.TARGET, db.targetBlock.getLocation(), power,
							args);

				iterator.remove();

				DestroySpell.destroyedBlocks.forEach(destroyedBlock -> {
					if (source != null && destroyedBlock.targetBlock != null
							&& destroyedBlock.targetBlock.getLocation().equals(source.getLocation()))
						destroyedBlock.targetBlock = null;
				});
			}
		}

		if (caster != null)
			playSpellEffects(EffectPosition.CASTER, caster.getLocation(), power, args);

		playSpellEffects(EffectPosition.SPECIAL, loc, power, args);
	}
}

enum SearchMode {
	BOTH,
	SOURCE,
	TARGET
}