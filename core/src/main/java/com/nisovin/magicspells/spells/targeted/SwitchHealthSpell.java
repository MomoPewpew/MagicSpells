package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.SpellData;

public class SwitchHealthSpell extends TargetedSpell implements TargetedEntitySpell {

	private boolean requireLesserHealthPercent;
	private boolean requireGreaterHealthPercent;

	public SwitchHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		requireLesserHealthPercent = getConfigBoolean("require-lesser-health-percent", false);
		requireGreaterHealthPercent = getConfigBoolean("require-greater-health-percent", false);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			data = data.builder().power(target.getPower()).build();
			boolean ok = switchHealth(data);
			if (!ok) return noTarget(data);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return switchHealth(data);
	}

	private boolean switchHealth(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		if (caster.isDead() || target.isDead()) return false;
		double casterPct = caster.getHealth() / Util.getMaxHealth(caster);
		double targetPct = target.getHealth() / Util.getMaxHealth(target);
		if (requireGreaterHealthPercent && casterPct < targetPct) return false;
		if (requireLesserHealthPercent && casterPct > targetPct) return false;
		caster.setHealth(targetPct * Util.getMaxHealth(caster));
		target.setHealth(casterPct * Util.getMaxHealth(target));
		playSpellEffects(caster, target, data);
		return true;
	}

}
