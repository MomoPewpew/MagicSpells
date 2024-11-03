package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CustomNameVisibilitySpell extends TargetedSpell implements TargetedEntitySpell {

	private TargetBooleanState targetBooleanState;

	public CustomNameVisibilitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);
			LivingEntity target = targetInfo.target();

			target.setCustomNameVisible(targetBooleanState.getBooleanState(target.isCustomNameVisible()));
			playSpellEffects(data.builder().target(target).power(targetInfo.getPower()).build());
			sendMessages(data.caster(), target, data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		LivingEntity target = data.target();
		if (!validTargetList.canTarget(data.caster(), target)) return false;
		target.setCustomNameVisible(targetBooleanState.getBooleanState(target.isCustomNameVisible()));
		playSpellEffects(data);
		return true;
	}

}
