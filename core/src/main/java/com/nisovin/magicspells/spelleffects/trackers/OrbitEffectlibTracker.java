package com.nisovin.magicspells.spelleffects.trackers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.util.VectorUtils;

public class OrbitEffectlibTracker extends AsyncEffectTracker implements Runnable {

	private Vector currentPosition;

	private BukkitTask repeatingHorizTask;
	private BukkitTask repeatingVertTask;

	private float orbRadius;
	private float orbHeight;

	private float xAxis;
	private float yAxis;
	private float zAxis;

	private final Effect effectlibEffect;

	public OrbitEffectlibTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect) {
		super(entity, checker, effect);
		currentPosition = entity.getLocation().getDirection().setY(0);
		Util.rotateVector(currentPosition, effect.getHorizOffset());
		orbRadius = effect.getOrbitRadius();
		orbHeight = effect.getOrbitYOffset();

		if (effect.getHorizExpandDelay() > 0 && effect.getHorizExpandRadius() != 0) {
			repeatingHorizTask = Bukkit.getScheduler().runTaskTimerAsynchronously(MagicSpells.getInstance(), () -> {
				orbRadius += effect.getHorizExpandRadius();
			}, effect.getHorizExpandDelay(), effect.getHorizExpandDelay());
		}

		if (effect.getVertExpandDelay() > 0 && effect.getVertExpandRadius() != 0) {
			repeatingVertTask = Bukkit.getScheduler().runTaskTimerAsynchronously(MagicSpells.getInstance(), () -> {
				orbHeight += effect.getVertExpandRadius();
			}, effect.getVertExpandDelay(), effect.getVertExpandDelay());
		}

		effectlibEffect = effect.playEffectLib(entity.getLocation());
		effectlibEffect.infinite();
	}

	@Override
	public void run() {
		if (!entity.isValid() || !checker.isActive(entity) || effect == null) {
			stop();
			return;
		}

		xAxis += effect.getOrbitXAxis();
		yAxis += effect.getOrbitYAxis();
		zAxis += effect.getOrbitZAxis();

		Location loc = getLocation();

		effectlibEffect.setLocation(loc);
	}

	private Location getLocation() {
		Vector perp;
		if (effect.isCounterClockwise()) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
		else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
		currentPosition.add(perp.multiply(effect.getDistancePerTick())).normalize();
		Vector pos = VectorUtils.rotateVector(currentPosition.clone(), xAxis, yAxis, zAxis);
		return entity.getLocation().clone().add(0, orbHeight, 0).add(pos.multiply(orbRadius)).setDirection(perp);
	}

	@Override
	public void stop() {
		super.stop();
		if (effectlibEffect != null) effectlibEffect.cancel();
		if (repeatingHorizTask != null) repeatingHorizTask.cancel();
		if (repeatingVertTask != null) repeatingVertTask.cancel();
		currentPosition = null;
	}

	public Effect getEffectlibEffect() {
		return effectlibEffect;
	}

}
