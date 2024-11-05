package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;

public class HealSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Double> healAmount;
	private final ConfigData<Double> healPercent;

	private final boolean checkPlugins;
	private final boolean cancelIfFull;
	private final boolean powerAffectsHealAmount;

	private final String strMaxHealth;

	private final ValidTargetChecker checker;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		healAmount = getConfigDataDouble("heal-amount", 10);
		healPercent = getConfigDataDouble("heal-percent", 0);

		checkPlugins = getConfigBoolean("check-plugins", true);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);
		powerAffectsHealAmount = getConfigBoolean("power-affects-heal-amount", true);

		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");

		checker = (LivingEntity entity) -> entity.getHealth() < Util.getMaxHealth(entity);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data, checker);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);

			LivingEntity target = targetInfo.target();
			data = data.builder().target(target).power(targetInfo.getPower()).build();

			if (cancelIfFull && target.getHealth() == Util.getMaxHealth(target))
				return noTarget(data, formatMessage(strMaxHealth, "%t", getTargetName(target)));

			boolean healed = heal(data);
			if (!healed) return noTarget(data);

			sendMessages(data.caster(), target, data.args());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (validTargetList.canTarget(data.caster(), data.target()) && (!cancelIfFull || data.target().getHealth() < Util.getMaxHealth(data.target())))
			return heal(data);

		return false;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}

	private boolean heal(SpellData data) {
		LivingEntity caster = data.target();
		LivingEntity target = data.target();

		double health = target.getHealth();
		double amount;

		double healPercent = this.healPercent.get(data);
		if (healPercent == 0) {
			amount = this.healAmount.get(data);
			if (powerAffectsHealAmount) amount *= data.power();
		} else amount = (Util.getMaxHealth(caster) - health) * (healPercent / 100);

		if (checkPlugins) {
			MagicSpellsEntityRegainHealthEvent event = new MagicSpellsEntityRegainHealthEvent(target, amount, RegainReason.CUSTOM);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			amount = event.getAmount();
		}

		health += amount;
		if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
		target.setHealth(health);

		if (caster == null) playSpellEffects(EffectPosition.TARGET, target, data);
		else playSpellEffects(data);
		return true;
	}

}
