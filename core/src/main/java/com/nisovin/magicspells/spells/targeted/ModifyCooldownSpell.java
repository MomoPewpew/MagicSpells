package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ModifyCooldownSpell extends TargetedSpell implements TargetedEntitySpell {

	private final SpellFilter filter;

	private final ConfigData<Float> seconds;
	private final ConfigData<Float> multiplier;

	private final boolean powerAffectsSeconds;
	private final boolean powerAffectsMultiplier;

	public ModifyCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		seconds = getConfigDataFloat("seconds", 1F);
		multiplier = getConfigDataFloat("multiplier", 1F);

		powerAffectsSeconds = getConfigBoolean("power-affects-seconds", true);
		powerAffectsMultiplier = getConfigBoolean("power-affects-multiplier", true);

		filter = getConfigSpellFilter();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			modifyCooldowns(data.builder()
				.target(target.target())
				.power(target.getPower())
				.build());
			sendMessages(data.caster(), target.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		modifyCooldowns(data);
		return true;
	}

	private void modifyCooldowns(SpellData data) {
		float sec = seconds.get(data);
		if (powerAffectsSeconds) sec *= data.power();

		float mult = multiplier.get(data);
		if (powerAffectsMultiplier) mult /= data.power();

		for (Spell spell : MagicSpells.spells()) {
			if (!spell.onCooldown(data.target())) continue;
			if (!filter.check(spell)) continue;

			float cd = spell.getCooldown(data.target()) - sec;
			cd *= mult;
			if (cd < 0) cd = 0;
			spell.setCooldown(data.target(), cd, false);
		}

		if (data.caster() != null) playSpellEffects(data.caster(), data.target(), data);
		else playSpellEffects(EffectPosition.TARGET, data.target(), data);
	}

}
