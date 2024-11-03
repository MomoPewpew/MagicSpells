package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CrippleSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Integer> strength;
	private ConfigData<Integer> duration;
	private ConfigData<Integer> portalCooldown;

	private boolean useSlownessEffect;
	private boolean applyPortalCooldown;
	private boolean powerAffectsDuration;
	private boolean powerAffectsPortalCooldown;

	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigDataInt("effect-strength", 5);
		duration = getConfigDataInt("effect-duration", 100);
		portalCooldown = getConfigDataInt("portal-cooldown-ticks", 100);

		useSlownessEffect = getConfigBoolean("use-slowness-effect", true);
		applyPortalCooldown = getConfigBoolean("apply-portal-cooldown", false);
		powerAffectsDuration = getConfigBoolean("power-affects-duration", true);
		powerAffectsPortalCooldown = getConfigBoolean("power-affects-portal-cooldown", true);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data, info);

			cripple(data.builder().target(info.target()).power(info.getPower()).build());
			sendMessages(data.caster(), info.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		cripple(data);
		return true;
	}

	private void cripple(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		if (target == null) return;

		if (caster != null) playSpellEffects(data);
		else playSpellEffects(EffectPosition.TARGET, target, data);

		if (useSlownessEffect) {
			int strength = this.strength.get(data);
			int duration = this.duration.get(data);
			if (powerAffectsDuration) duration = Math.round(duration * data.power());

			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, strength));
		}

		if (applyPortalCooldown) {
			int portalCooldown = this.portalCooldown.get(data);
			if (powerAffectsPortalCooldown) portalCooldown = Math.round(portalCooldown * data.power());

			if (target.getPortalCooldown() < portalCooldown) target.setPortalCooldown(portalCooldown);
		}
	}

}
