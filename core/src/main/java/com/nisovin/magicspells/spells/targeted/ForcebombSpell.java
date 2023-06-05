package com.nisovin.magicspells.spells.targeted;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class ForcebombSpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Double> force;
	private ConfigData<Double> radius;
	private ConfigData<Double> yForce;
	private ConfigData<Double> yOffset;
	private ConfigData<Double> maxYForce;

	private boolean addYForceInstead;
	private boolean callTargetEvents;
	private boolean powerAffectsForce;
	private boolean addVelocityInstead;

	public ForcebombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		force = getConfigDataDouble("pushback-force", 30);
		yForce = getConfigDataDouble("additional-vertical-force", 15);
		radius = getConfigDataDouble("radius", 3);
		yOffset = getConfigDataDouble("y-offset", 0F);
		maxYForce = getConfigDataDouble("max-vertical-force", 20);

		addYForceInstead = getConfigBoolean("add-y-force-instead", false);
		callTargetEvents = getConfigBoolean("call-target-events", true);
		powerAffectsForce = getConfigBoolean("power-affects-force", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block = getTargetedBlock(caster, power, args);
			if (block != null && !BlockUtils.isAir(block.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, block.getLocation(), power, args);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block == null || BlockUtils.isAir(block.getType())) return noTarget(caster, args);
			knockback(caster, block.getLocation().add(0.5, 0, 0.5), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		knockback(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		knockback(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		knockback(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		knockback(null, target, power, null);
		return true;
	}

	private void knockback(LivingEntity caster, Location location, float basePower, String[] args) {
		if (location == null) return;
		if (location.getWorld() == null) return;

		location = location.clone().add(0D, yOffset.get(caster, null, basePower, args), 0D);

		double radiusSquared = this.radius.get(caster, null, basePower, args);
		radiusSquared *= radiusSquared;

		SpellData data = new SpellData(caster, basePower, args);
		if (validTargetList.canTargetOnlyCaster()) {
			if (caster == null) return;
			if (!caster.getWorld().equals(location.getWorld())) return;
			if (caster.getLocation().distanceSquared(location) > radiusSquared) return;

			bomb(caster, caster, location, basePower, args);

			playSpellEffects(EffectPosition.CASTER, caster, data);
			playSpellEffects(EffectPosition.SPECIAL, location, data);
			return;
		}

		Collection<LivingEntity> entities = location.getWorld().getLivingEntities();
		for (LivingEntity entity : entities) {
			if (!validTargetList.canTarget(caster, entity)) continue;
			if (!entity.getWorld().equals(location.getWorld())) continue;
			if (entity.getLocation().distanceSquared(location) > radiusSquared) continue;

			bomb(caster, entity, location, basePower, args);
		}

		playSpellEffects(EffectPosition.SPECIAL, location, data);
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster, data);
	}

	private void bomb(LivingEntity caster, LivingEntity target, Location location, float basePower, String[] args) {
		float power = basePower;
		if (callTargetEvents && caster != null) {
			SpellTargetEvent event = new SpellTargetEvent(this, caster, target, power, args);
			EventUtil.call(event);
			if (event.isCancelled()) return;

			target = event.getTarget();
			power = event.getPower();
		}

		double force = this.force.get(caster, target, power, args) / 10;
		if (powerAffectsForce) force *= power;

		Vector v = target.getLocation().toVector().subtract(location.toVector()).normalize().multiply(force);

		double yForce = this.yForce.get(caster, target, power, args) / 10;
		if (powerAffectsForce) yForce *= power;

		double maxYForce = this.maxYForce.get(caster, target, power, args) / 10;
		if (addYForceInstead) v.setY(Math.min(v.getY() + yForce, maxYForce));
		else v.setY(Math.min(force == 0 ? yForce : v.getY() * yForce, maxYForce));

		v = Util.makeFinite(v);

		if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);

		SpellData data = new SpellData(caster, target, power, args);
		if (caster != null) playSpellEffects(caster, location, target, data);
		else playSpellEffects(EffectPosition.TARGET, target, data);
	}

}
