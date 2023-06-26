package com.nisovin.magicspells.spelleffects.trackers;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

import de.slikey.effectlib.util.VectorUtils;

public class OrbitTracker extends EffectTracker implements Runnable {

	private Vector currentPosition;

	private int horizontalTaskId;
	private int verticalTaskId;

	private float orbRadius;
	private float orbHeight;

	private float xAxis;
	private float yAxis;
	private float zAxis;

	private final float orbitXAxis;
	private final float orbitYAxis;
	private final float orbitZAxis;
	private final float distancePerTick;

	private Location location;

	private ModifierResult result;

	public OrbitTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		super(entity, checker, effect, data);

		currentPosition = entity.getLocation().getDirection().setY(0);
		Util.rotateVector(currentPosition, effect.getHorizOffset().get(data));

		orbRadius = effect.getOrbitRadius().get(data);
		orbHeight = effect.getOrbitYOffset().get(data);

		orbitXAxis = effect.getOrbitXAxis().get(data);
		orbitYAxis = effect.getOrbitYAxis().get(data);
		orbitZAxis = effect.getOrbitZAxis().get(data);
		distancePerTick = 6.28f * effect.getEffectInterval().get(data) / effect.getSecondsPerRevolution().get(data) / 20f;

		float horizRadius = effect.getHorizExpandRadius().get(data);
		int horizDelay = effect.getHorizExpandDelay().get(data);
		if (horizDelay > 0 && horizRadius != 0)
			horizontalTaskId = MagicSpells.scheduleRepeatingTask(() -> orbRadius += horizRadius, horizDelay, horizDelay);

		float vertRadius = effect.getVertExpandRadius().get(data);
		int vertDelay = effect.getVertExpandDelay().get(data);
		if (vertDelay > 0 && vertRadius != 0)
			verticalTaskId = MagicSpells.scheduleRepeatingTask(() -> orbHeight += vertRadius, vertDelay, vertDelay);
	}

	@Override
	public void run() {
		if (!entity.isValid() || !checker.isActive(entity) || effect == null) {
			stop();
			return;
		}

		xAxis += orbitXAxis;
		yAxis += orbitYAxis;
		zAxis += orbitZAxis;

		location = getLocation();

		if (entity instanceof LivingEntity livingEntity && effect.getModifiers() != null) {
			result = effect.getModifiers().apply(livingEntity, data);
			data = result.data();

			if (!result.check()) return;
		}

		effect.playEffect(location, data);
		playEffects(location, data);
	}

	private void playEffects(Location location, SpellData data) {
		if (!isEntityEffect) {
			effect.playEffect(location, data);
			return;
		}

		if (!effect.isDraggingEntity().get(data)) {
			effect.playEffect(location, data);
			return;
		}

		if (effectEntity == null) {
			effectEntity = effect.playEntityEffect(location, data);
			return;
		}

		effectEntity.teleport(location);
	}

	private Location getLocation() {
		Vector perp;
		if (effect.isCounterClockwise()) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
		else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
		currentPosition.add(perp.multiply(distancePerTick)).normalize();
		Vector pos = VectorUtils.rotateVector(currentPosition.clone(), xAxis, yAxis, zAxis);
		return entity.getLocation().clone().add(0, orbHeight, 0).add(pos.multiply(orbRadius)).setDirection(perp);
	}

	@Override
	public void stop() {
		super.stop();
		MagicSpells.cancelTask(horizontalTaskId);
		MagicSpells.cancelTask(verticalTaskId);
		currentPosition = null;
	}

}
