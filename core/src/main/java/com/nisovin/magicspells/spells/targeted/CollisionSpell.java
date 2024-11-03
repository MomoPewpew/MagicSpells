package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CollisionSpell extends TargetedSpell implements TargetedEntitySpell {

	private TargetBooleanState targetBooleanState;

	public CollisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data);
			LivingEntity target = info.target();

			target.setCollidable(targetBooleanState.getBooleanState(target.isCollidable()));
			playSpellEffects(data.builder().power(info.getPower()).build());
			sendMessages(data.caster(), target, data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		LivingEntity target = data.target();
		if (!validTargetList.canTarget(data.caster(), target)) return false;
		target.setCollidable(targetBooleanState.getBooleanState(target.isCollidable()));
		playSpellEffects(data);
		return true;
	}

}
