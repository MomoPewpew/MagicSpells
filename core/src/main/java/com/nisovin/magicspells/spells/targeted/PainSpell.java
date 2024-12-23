package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.spells.DamageSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell, DamageSpell {

	private String spellDamageType;
	private DamageCause damageType;

	private ConfigData<Double> damage;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean powerAffectsDamage;
	private boolean avoidDamageModification;
	private boolean tryAvoidingAntiCheatPlugins;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellDamageType = getConfigString("spell-damage-type", "");
		String damageTypeName = getConfigString("damage-type", "ENTITY_ATTACK");
		try {
			damageType = DamageCause.valueOf(damageTypeName.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			DebugHandler.debugBadEnumValue(DamageCause.class, damageTypeName);
			damageType = DamageCause.ENTITY_ATTACK;
		}

		damage = getConfigDataDouble("damage", 4);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsDamage = getConfigBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", true);
		tryAvoidingAntiCheatPlugins = getConfigBoolean("try-avoiding-anticheat-plugins", false);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			boolean done;
			if (data.caster() instanceof Player) {
				done = CompatBasics.exemptAction(() -> causePain(data.builder().target(target.target()).power(target.getPower()).build()), 
					(Player) data.caster(), CompatBasics.activeExemptionAssistant.getPainExemptions());
			} else done = causePain(data.builder().target(target.target()).power(target.getPower()).build());
			if (!done) return noTarget(data);
			
			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return causePain(data);
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}
	
	private boolean causePain(SpellData data) {
		LivingEntity target = data.target();
		if (target == null) return false;
		if (target.isDead()) return false;

		double localDamage = damage.get(data);
		if (powerAffectsDamage) localDamage *= data.power();

		if (checkPlugins) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(data.caster(), target, damageType, localDamage, this);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) localDamage = event.getDamage();
			target.setLastDamageCause(event);
		}

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, data, localDamage, damageType, spellDamageType);
		EventUtil.call(event);
		localDamage = event.getFinalDamage();

		if (ignoreArmor) {
			double health = target.getHealth();
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			health -= localDamage;
			if (health < 0) health = 0;
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			if (health == 0 && data.caster() instanceof Player) target.setKiller((Player) data.caster());

			target.setHealth(health);
			target.setLastDamage(localDamage);

			if (data.caster() != null) MagicSpells.getVolatileCodeHandler().playHurtAnimation(target, LocationUtil.getRotatedLocation(data.caster().getLocation(), target.getLocation()).getYaw());
			else MagicSpells.getVolatileCodeHandler().playHurtAnimation(target, target.getLocation().getYaw());

			if (data.caster() != null) playSpellEffects(data.caster(), target, data);
			else playSpellEffects(EffectPosition.TARGET, target, data);

			return true;
		}

		if (tryAvoidingAntiCheatPlugins) target.damage(localDamage);
		else target.damage(localDamage, data.caster());

		if (data.caster() != null) playSpellEffects(data.caster(), target, data);
		else playSpellEffects(EffectPosition.TARGET, target, data);

		return true;
	}

}
