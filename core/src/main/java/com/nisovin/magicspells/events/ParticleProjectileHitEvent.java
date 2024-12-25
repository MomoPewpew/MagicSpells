package com.nisovin.magicspells.events;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

public class ParticleProjectileHitEvent extends SpellEvent implements Cancellable {

	private LivingEntity target;

	private ParticleProjectileTracker tracker;

	private float power;

	private boolean cancelled = false;

	public ParticleProjectileHitEvent(ParticleProjectileTracker tracker, Spell spell, SpellData data) {
		super(spell, data);

		this.tracker = tracker;
	}

	public ParticleProjectileTracker getTracker() {
		return tracker;
	}

	public void setTracker(ParticleProjectileTracker tracker) {
		this.tracker = tracker;
	}

	public LivingEntity getTarget() {
		return target;
	}

	public void setTarget(LivingEntity target) {
		this.target = target;
	}

	public float getPower() {
		return power;
	}

	public void setPower(float power) {
		this.power = power;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

}
