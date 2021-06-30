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
import com.nisovin.magicspells.spells.instant.MarkSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class RemoveMarksSpell extends TargetedSpell implements TargetedLocationSpell {

	float radius;
	boolean pointBlank;
	String markSpellName;
	MarkSpell markSpell;
	boolean mustBeOwner;

	public RemoveMarksSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		radius = getConfigFloat("radius", 10F);
		pointBlank = getConfigBoolean("point-blank", false);
		markSpellName = getConfigString("mark-spell", "mark");
		mustBeOwner = getConfigBoolean("must-be-owner", false);
	}

	@Override
	public void initialize() {
		super.initialize();
		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell instanceof MarkSpell) {
			markSpell = (MarkSpell)spell;
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

			removeMarks(player, loc, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	void removeMarks(Player caster, Location loc, float power) {
		float rad = radius * power;
		float radSq = rad * rad;
		HashMap<String, MagicLocation> marks = markSpell.getMarks();
		Iterator<String> iter = marks.keySet().iterator();
		World locWorld = loc.getWorld();
		String locWorldName = locWorld.getName();
		boolean isMod = false;
		while (iter.hasNext()) {
			String playerKey = iter.next();
			if (mustBeOwner && !MarkSpell.getPlayerKey(caster).equals(playerKey)) continue;
			MagicLocation l = marks.get(playerKey);
			if (!l.getWorld().equals(locWorldName)) continue;
			if (l.getLocation().distanceSquared(loc) < radSq) {
				isMod = true;
				iter.remove();
			}
		}
		if (isMod) markSpell.saveMarks();
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
		playSpellEffects(EffectPosition.TARGET, loc);
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		removeMarks(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		removeMarks(null, target, power);
		return true;
	}
}
