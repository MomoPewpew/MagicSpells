package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.targeted.PulserSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class RemovePulsersSpell extends TargetedSpell implements TargetedLocationSpell {

	float radius;
	boolean pointBlank;
	String markSpellName;
	PulserSpell pulserSpell;
	boolean mustBeOwner;

	public RemovePulsersSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		radius = getConfigFloat("radius", 10F);
		pointBlank = getConfigBoolean("point-blank", false);
		markSpellName = getConfigString("pulser-spell", "mark");
		mustBeOwner = getConfigBoolean("must-be-owner", false);
	}

	@Override
	public void initialize() {
		super.initialize();
		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell instanceof PulserSpell) {
			pulserSpell = (PulserSpell)spell;
		} else {
			MagicSpells.error("Failed to get mark spell for '" + internalName + "' spell");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) {
				loc = player.getLocation();
			} else {
				Block b = getTargetedBlock(player, power);
				if (b != null && b.getType() != Material.AIR) loc = b.getLocation();
			}
			if (loc == null) return noTarget(player);

			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, loc, power);
			EventUtil.call(event);
			if (event.isCancelled()) return noTarget(player);
			loc = event.getTargetLocation();
			power = event.getPower();

			removePulsers(player, loc, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	void removePulsers(Player caster, Location loc, float power) {
		float rad = radius * power;
		float radSq = rad * rad;
		HashMap<Block, PulserSpell.Pulser> pulsers = pulserSpell.getPulsers();
		Iterator<Block> iter = pulsers.keySet().iterator();
		World locWorld = loc.getWorld();
		String locWorldName = locWorld.getName();
		while (iter.hasNext()) {
			Block block = iter.next();
			PulserSpell.Pulser pulser = pulsers.get(block);
			if (mustBeOwner && !pulser.caster.equals(caster)) continue;
			Location l = pulser.location;
			if (!l.getWorld().getName().equals(locWorldName)) continue;
			if (l.distanceSquared(loc) < radSq) {
				pulser.stop();
				block.setType(Material.AIR);
				iter.remove();
			}
		}
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
		playSpellEffects(EffectPosition.TARGET, loc);
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		removePulsers(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		removePulsers(null, target, power);
		return true;
	}
}
