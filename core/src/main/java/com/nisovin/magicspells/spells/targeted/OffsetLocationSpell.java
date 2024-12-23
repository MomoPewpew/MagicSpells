package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.SpellData;

public class OffsetLocationSpell extends TargetedSpell implements TargetedLocationSpell {

	private Vector relativeOffset;
	private Vector absoluteOffset;

	private float forcedPitch;
	private boolean forcePitch = false;

	private Subspell spellToCast;
	private String spellToCastName;

	public OffsetLocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		absoluteOffset = getConfigVector("absolute-offset", "0,0,0");

		if (configKeyExists("forced-pitch")) {
			forcedPitch = getConfigFloat("forced-pitch", 0);
			forcePitch = true;
		}

		spellToCastName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			MagicSpells.error("OffsetLocationSpell '" + internalName + "' has an invalid spell defined!");
			spellToCast = null;
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Location baseTargetLocation;
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.cancelled()) return PostCastAction.ALREADY_HANDLED;

			if (!info.empty()) baseTargetLocation = info.target().getLocation();
			else baseTargetLocation = getTargetedBlock(data.caster(), data.power()).getLocation();

			Location loc;
			if (forcePitch) {
				loc = Util.applyOffsets(baseTargetLocation.clone(), relativeOffset, absoluteOffset, forcedPitch);
			} else {
				loc = Util.applyOffsets(baseTargetLocation.clone(), relativeOffset, absoluteOffset);
			}

			if (spellToCast != null) spellToCast.subcast(data.builder().location(loc).build());
			playSpellEffects(data.caster(), loc, data);

			if (!info.empty()) {
				sendMessages(data.caster(), info.target(), data.args());
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		if (spellToCast != null) {
			if (forcePitch) {
				spellToCast.subcast(data.builder().location(Util.applyOffsets(data.location().clone(), relativeOffset, absoluteOffset, forcedPitch)).build());
			} else {
				spellToCast.subcast(data.builder().location(Util.applyOffsets(data.location().clone(), relativeOffset, absoluteOffset)).build());
			}
		}
		playSpellEffects(data.caster(), data.location(), data);
		return true;
	}

}
