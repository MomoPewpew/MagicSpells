package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class GlideSpell extends TargetedSpell implements TargetedEntitySpell{
	
	private TargetBooleanState targetState;
	
	public GlideSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}
	
	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);
			data = data.builder().target(targetInfo.target()).power(targetInfo.getPower()).build();

			data.target().setGliding(targetState.getBooleanState(data.target().isGliding()));
			playSpellEffects(data);
			sendMessages(data.caster(), data.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(SpellData data) {
		LivingEntity target = data.target();
		if (!validTargetList.canTarget(data.caster(), target)) return false;
		target.setGliding(targetState.getBooleanState(target.isGliding()));
		return true;
	}
	
}
