package com.nisovin.magicspells.spells.targeted;

import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.targeted.PulserSpell.Pulser;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class RemovePulsersSpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Float> radius;

	private boolean pointBlank;
	private boolean powerAffectsRadius;
	private boolean ownedPulsersOnly;

	private List<String> pulserSpellNames;
	private List<PulserSpell> pulserSpells;

	public RemovePulsersSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataFloat("radius", 10F);

		pointBlank = getConfigBoolean("point-blank", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		ownedPulsersOnly = getConfigBoolean("owned-pulsers-only", false);

		pulserSpellNames = getConfigStringList("pulser-spells", null);

		pulserSpells = new ArrayList<PulserSpell>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (pulserSpellNames != null) {
			for (String pulserSpellName : pulserSpellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(pulserSpellName);
				if (spell instanceof PulserSpell) {
					pulserSpells.add((PulserSpell) spell);
				} else {
					MagicSpells.error(
							"RemovePulsersSpell '" + internalName + "' has an invalid spell defined in pulser-spells!");
					return;
				}
			}
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank)
				loc = caster.getLocation();
			else {
				Block b = getTargetedBlock(caster, power, args);
				if (b != null && !BlockUtils.isAir(b.getType()))
					loc = b.getLocation();
			}
			if (loc == null)
				return noTarget(caster, args);
			removePulsers(caster, loc, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		removePulsers(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		removePulsers(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		removePulsers(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		removePulsers(null, target, power, null);
		return true;
	}

	private void removePulsers(LivingEntity caster, Location loc, float power, String[] args) {
		float radSq = radius.get(caster, null, power, args);
		if (powerAffectsRadius)
			radSq *= power;

		radSq *= radSq;

		World locWorld = loc.getWorld();

		List<PulserSpell> pulserSpellsTemp = new ArrayList<>();

		if (pulserSpells.isEmpty()) {
			for (Spell spell : MagicSpells.spells()) {
				if (spell instanceof PulserSpell) {
					pulserSpellsTemp.add((PulserSpell) spell);
				}
			}
		} else {
			pulserSpellsTemp = pulserSpells;
		}

		for (PulserSpell pulserSpell : pulserSpellsTemp) {
			Iterator<Entry<Block, Pulser>> iterator = pulserSpell.pulsers.entrySet().iterator();

			while (iterator.hasNext()) {
				Entry<Block, Pulser> entry = iterator.next();
				Block block = entry.getKey();
				Pulser pulser = entry.getValue();

				if (!block.getWorld().equals(locWorld))
					continue;
				if (block.getLocation().distanceSquared(loc) > radSq)
					continue;
				if (ownedPulsersOnly && !pulser.caster.equals(caster))
					continue;

				pulser.stop();
				iterator.remove();
				playSpellEffects(EffectPosition.TARGET, block.getLocation(), power, args);
			}
		}

		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster.getLocation(), power, args);
		playSpellEffects(EffectPosition.SPECIAL, loc, power, args);
	}

}
