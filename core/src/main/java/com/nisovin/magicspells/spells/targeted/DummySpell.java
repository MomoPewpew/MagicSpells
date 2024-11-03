package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class DummySpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell, TargetedEntityFromLocationSpell {

	public DummySpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			data = data.builder().target(target.target()).power(target.getPower()).build();
			if (target.noTarget()) return noTarget(data);

			playSpellEffects(data);
			sendMessages(data.caster(), target.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		playSpellEffects(data);
		return true;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		playSpellEffects(data);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		playSpellEffects(data.caster(), data.location(), data.target(), data);
		return true;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return true;
	}
	
}
