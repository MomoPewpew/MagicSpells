package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.DamageSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class DotSpell extends TargetedSpell implements TargetedEntitySpell, DamageSpell {

	private Map<UUID, Dot> activeDots;

	private ConfigData<Integer> delay;
	private ConfigData<Integer> interval;
	private ConfigData<Integer> duration;

	private ConfigData<Double> damage;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean powerAffectsDamage;
	private boolean avoidDamageModification;
	private boolean tryAvoidingAntiCheatPlugins;

	private String spellDamageType;
	private DamageCause damageType;

	public DotSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		delay = getConfigDataInt("delay", 1);
		interval = getConfigDataInt("interval", 20);
		duration = getConfigDataInt("duration", 200);

		damage = getConfigDataDouble("damage", 2);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsDamage = getConfigBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", true);
		tryAvoidingAntiCheatPlugins = getConfigBoolean("try-avoiding-anticheat-plugins", false);

		spellDamageType = getConfigString("spell-damage-type", "");
		String damageTypeName = getConfigString("damage-type", "ENTITY_ATTACK");
		try {
			damageType = DamageCause.valueOf(damageTypeName.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			DebugHandler.debugBadEnumValue(DamageCause.class, damageTypeName);
			damageType = DamageCause.ENTITY_ATTACK;
		}

		activeDots = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);

			applyDot(data.builder().target(targetInfo.getTarget()).power(targetInfo.getPower()).build());
			sendMessages(data.caster(), targetInfo.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		applyDot(data);
		return true;
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}

	public boolean isActive(LivingEntity entity) {
		return activeDots.containsKey(entity.getUniqueId());
	}

	public void cancelDot(LivingEntity entity) {
		if (!isActive(entity)) return;
		Dot dot = activeDots.get(entity.getUniqueId());
		dot.cancel();
	}

	private void applyDot(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		float power = data.power();
		String[] args = data.args();
		
		Dot dot = activeDots.get(target.getUniqueId());
		if (dot != null) {
			dot.data.caster(caster);
			dot.data.power(power);
			dot.data.args(args);

			dot.init();
		} else {
			dot = new Dot(data);
			activeDots.put(target.getUniqueId(), dot);
		}

		if (caster != null) playSpellEffects(data);
		else playSpellEffects(EffectPosition.TARGET, target, data);
	}

	@EventHandler
	private void onDeath(PlayerDeathEvent event) {
		Dot dot = activeDots.get(event.getEntity().getUniqueId());
		if (dot != null) dot.cancel();
	}

	private class Dot implements Runnable {

		private SpellData data;

		private int taskId = -1;
		private int duration;
		private int interval;
		private int dur = 0;

		private Dot(SpellData data) {
			this.data = data;

			init();
		}

		private void init() {
			if (taskId != -1) MagicSpells.cancelTask(taskId);

			interval = DotSpell.this.interval.get(data);
			duration = DotSpell.this.duration.get(data);
			dur = 0;

			taskId = MagicSpells.scheduleRepeatingTask(this, delay.get(data), interval);
		}

		@Override
		public void run() {
			LivingEntity caster = data.caster();
			LivingEntity target = data.target();
			float power = data.power();

			dur += interval;
			if (dur > duration) {
				cancel();
				return;
			}

			if (target.isDead() || !target.isValid()) {
				cancel();
				return;
			}

			double localDamage = damage.get(data);
			if (powerAffectsDamage) localDamage *= power;

			if (checkPlugins) {
				MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, damageType, localDamage, DotSpell.this);
				EventUtil.call(event);
				if (event.isCancelled()) return;
				if (!avoidDamageModification) localDamage = event.getDamage();
				target.setLastDamageCause(event);
			}

			SpellApplyDamageEvent event = new SpellApplyDamageEvent(DotSpell.this, data, localDamage, damageType, spellDamageType);
			EventUtil.call(event);
			localDamage = event.getFinalDamage();

			if (ignoreArmor) {
				double maxHealth = Util.getMaxHealth(target);
				double health = target.getHealth();

				if (health > maxHealth) health = maxHealth;
				health -= localDamage;

				if (health < 0) {
					health = 0;
					if (caster instanceof Player) target.setKiller((Player) caster);
				}

				if (health > maxHealth) health = maxHealth;

				target.setHealth(health);
				target.setLastDamage(localDamage);

				if (caster != null) MagicSpells.getVolatileCodeHandler().playHurtAnimation(target, LocationUtil.getRotatedLocation(caster.getLocation(), target.getLocation()).getYaw());
				else MagicSpells.getVolatileCodeHandler().playHurtAnimation(target, target.getLocation().getYaw());
			} else {
				if (tryAvoidingAntiCheatPlugins) target.damage(localDamage);
				else target.damage(localDamage, caster);
			}

			playSpellEffects(EffectPosition.DELAYED, target, data);
			target.setNoDamageTicks(0);
		}

		private void cancel() {
			MagicSpells.cancelTask(taskId);
			activeDots.remove(data.target().getUniqueId());
		}

	}

}
