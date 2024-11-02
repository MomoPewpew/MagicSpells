package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class AgeSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker AGEABLE = entity -> entity instanceof Ageable;

	private ConfigData<Integer> rawAge;

	private boolean setMaturity;
	private boolean applyAgeLock;

	public AgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		rawAge = getConfigDataInt("age", 0);
		setMaturity = getConfigBoolean("set-maturity", true);
		applyAgeLock = getConfigBoolean("apply-age-lock", false);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data, AGEABLE);
			if (info.noTarget()) return noTarget(data, info);

			applyAgeChanges(data.builder().target(info.target()).build());
			sendMessages(data.caster(), info.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target()) || !(data.target() instanceof Ageable ageable)) return false;
		applyAgeChanges(data);
		return true;
	}

	private void applyAgeChanges(SpellData data) {
		Ageable target = (Ageable) data.target();

		if (setMaturity) target.setAge(rawAge.get(data));
		if (target instanceof Breedable breedable) breedable.setAgeLock(applyAgeLock);

		if (data.caster() != null) playSpellEffects(data);
		else playSpellEffects(EffectPosition.TARGET, data.caster(), data);
	}

}
