package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class CombustSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, SpellData> combusting;

	private ConfigData<Integer> fireTicks;
	private ConfigData<Double> fireTickDamage;

	private boolean checkPlugins;
	private boolean preventImmunity;
	private boolean powerAffectsFireTicks;
	private boolean powerAffectsFireTickDamage;

	public CombustSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fireTicks = getConfigDataInt("fire-ticks", 100);
		fireTickDamage = getConfigDataDouble("fire-tick-damage", 1);

		checkPlugins = getConfigBoolean("check-plugins", true);
		preventImmunity = getConfigBoolean("prevent-immunity", true);
		powerAffectsFireTicks = getConfigBoolean("power-affects-fire-ticks", true);
		powerAffectsFireTickDamage = getConfigBoolean("power-affects-fire-tick-damage", true);

		combusting = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data);

			boolean combusted = combust(data.builder().target(target.target()).power(target.getPower()).build());
			if (!combusted) return noTarget(data);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return combust(data);
	}

	private boolean combust(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		if (checkPlugins && caster != null) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, 1, this);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
		}

		int duration = fireTicks.get(data);
		if (powerAffectsFireTicks) duration = Math.round(duration * data.power());
		target.setFireTicks(duration);

		combusting.put(target.getUniqueId(), data);

		if (caster != null) playSpellEffects(data);
		else playSpellEffects(EffectPosition.TARGET, target, data);

		MagicSpells.scheduleDelayedTask(() -> combusting.remove(target.getUniqueId()), duration + 2);

		return true;
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FIRE_TICK) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity target)) return;

		SpellData data = combusting.get(target.getUniqueId()).builder().target(target).build();
		if (data == null) return;

		double fireTickDamage = this.fireTickDamage.get(data);
		if (powerAffectsFireTickDamage) fireTickDamage = fireTickDamage * data.power();

		EventUtil.call(new SpellApplyDamageEvent(this, data, fireTickDamage, DamageCause.FIRE_TICK, ""));
		event.setDamage(fireTickDamage);

		if (preventImmunity) MagicSpells.scheduleDelayedTask(() -> target.setNoDamageTicks(0), 0);
	}

}
