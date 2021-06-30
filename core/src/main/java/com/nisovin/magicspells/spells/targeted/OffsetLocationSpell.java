package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class OffsetLocationSpell extends TargetedSpell implements TargetedLocationSpell {

	private Vector relativeOffset;
	private Vector absoluteOffset;

	Subspell spell;

	public OffsetLocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		absoluteOffset = getConfigVector("absolute-offset", "0,0,0");

		spell = new Subspell(getConfigString("spell", ""));
	}

	@Override
	public void initialize() {
		super.initialize();
		if (spell != null && (!spell.process() || !spell.isTargetedLocationSpell())) {
			spell = null;
			MagicSpells.error("Invalid spell on OffsetLocationSpell '" + name + '\'');
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location baseTargetLocation;
			TargetInfo<LivingEntity> entityTargetInfo = getTargetedEntity(player, power);
			if (entityTargetInfo != null && entityTargetInfo.getTarget() != null) {
				baseTargetLocation = entityTargetInfo.getTarget().getLocation();
			} else {
				baseTargetLocation = getTargetedBlock(player, power).getLocation();
			}
			if (baseTargetLocation == null) return noTarget(player);

			Location loc = Util.applyOffsets(baseTargetLocation, relativeOffset, absoluteOffset);
			if (loc != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, loc, power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(player);
				loc = event.getTargetLocation();
				power = event.getPower();

				if (spell != null) spell.castAtLocation(player, loc, power);
				playSpellEffects(player, loc);
			} else {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Location location = target.clone();
		if (spell != null){
			return spell.castAtLocation(caster, Util.applyOffsets(location, relativeOffset, absoluteOffset), power);
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}

}
