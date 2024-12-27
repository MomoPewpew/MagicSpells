package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class ChainSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private ConfigData<Integer> bounces;
	private ConfigData<Integer> interval;

	private ConfigData<Double> bounceRange;

	private String spellToCastName;
	private Subspell spellToCast;

	public ChainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		bounces = getConfigDataInt("bounces", 3);
		interval = getConfigDataInt("interval", 10);

		bounceRange = getConfigDataDouble("bounce-range", 8);

		spellToCastName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			spellToCast = null;
			MagicSpells.error("ChainSpell '" + internalName + "' has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data, info);
			LivingEntity target = info.target();

			chain(data.builder().location(data.caster().getLocation()).power(info.getPower()).build());
			sendMessages(data.caster(), target, data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		chain(data.builder().location(data.caster().getLocation()).build());
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		chain(data);
		return true;
	}

	private void chain(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location start = data.location();
		float power = data.power();
		String[] args = data.args();
		List<LivingEntity> targets = new ArrayList<>();
		List<Float> targetPowers = new ArrayList<>();
		targets.add(target);
		targetPowers.add(power);

		int bounces = this.bounces.get(data);
		int interval = this.interval.get(data);
		double bounceRange = Math.min(this.bounceRange.get(data), MagicSpells.getGlobalRadius());

		// Get targets
		LivingEntity current = target;
		int attempts = 0;
		while (targets.size() < bounces && attempts++ < bounces << 1) {
			List<Entity> entities = current.getNearbyEntities(bounceRange, bounceRange, bounceRange);
			for (Entity entity : entities) {
				if (!(entity instanceof LivingEntity livingEntity)) continue;
				if (targets.contains(livingEntity)) continue;

				if (!validTargetList.canTarget(caster, livingEntity)) continue;

				float subPower = power;
				if (caster != null) {
					SpellTargetEvent event = new SpellTargetEvent(this, data);
					if (!event.callEvent()) continue;

					livingEntity = event.getTarget();
					subPower = event.getPower();
				}

				targets.add(livingEntity);
				targetPowers.add(subPower);
				current = livingEntity;

				break;
			}
		}

		// Cast spell at targets
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster, data);
		else if (start != null) playSpellEffects(EffectPosition.CASTER, start, data);

		if (interval <= 0) {
			for (int i = 0; i < targets.size(); i++) {
				Location from;
				if (i == 0) from = start;
				else from = targets.get(i - 1).getLocation();

				spellToCast.subcast(data.builder().target(targets.get(i)).location(from).power(targetPowers.get(i)).build());

				SpellData data_ = data.builder().target(targets.get(i)).location(from).power(targetPowers.get(i)).args(args).build();
				if (i > 0) playSpellEffectsTrail(targets.get(i - 1).getLocation(), targets.get(i).getLocation(), data_);
				else if (caster != null) playSpellEffectsTrail(caster.getLocation(), targets.get(i).getLocation(), data_);
				playSpellEffects(EffectPosition.TARGET, targets.get(i), data);
			}
		} else new ChainBouncer(data, targets, targetPowers, interval);
	}

	private void castSpellAt(SpellData data) {
		spellToCast.subcast(data);
	}

	private class ChainBouncer implements Runnable {

		private SpellData data;
		private final int taskId;

		private final List<LivingEntity> targets;
		private final List<Float> targetPowers;

		private int current = 0;

		private ChainBouncer(SpellData data, List<LivingEntity> targets, List<Float> targetPowers, int interval) {
			this.data = data;

			this.targetPowers = targetPowers;
			this.targets = targets;

			taskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			Location from;
			if (current == 0) from = data.location();
			else from = targets.get(current - 1).getLocation();

			SpellData data = this.data.builder().target(targets.get(current)).power(targetPowers.get(current)).location(from).build();

			spellToCast.subcast(data);
			if (current > 0) {
				playSpellEffectsTrail(targets.get(current - 1).getLocation().add(0, 0.5, 0), targets.get(current).getLocation().add(0, 0.5, 0), data);
			} else if (current == 0 && data.caster() != null) {
				playSpellEffectsTrail(data.caster().getLocation().add(0, 0.5, 0), targets.get(current).getLocation().add(0, 0.5, 0), data);
			}

			playSpellEffects(EffectPosition.TARGET, targets.get(current), data);
			current++;
			if (current >= targets.size()) MagicSpells.cancelTask(taskId);
		}

	}

}
