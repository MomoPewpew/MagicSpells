package com.nisovin.magicspells.util.trackers;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.block.BlockFace;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.ParticleProjectileHitEvent;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spells.instant.ParticleProjectileSpell;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

public class ParticleProjectileTracker implements Runnable, Tracker {

	private final Random rand = ThreadLocalRandom.current();

	private NoMagicZoneManager zoneManager;

	private Set<EffectlibSpellEffect> effectSet;
	private Map<SpellEffect, Entity> entityMap;
	private Set<ArmorStand> armorStandSet;

	private SpellData data;
	private long startTime;
	private Location startLocation;
	private Location previousLocation;
	private Location currentLocation;
	private Vector currentVelocity;
	private Vector startDirection;
	private Vector effectOffset;
	private int currentX;
	private int currentZ;
	private int counter;
	private int taskId;
	private BoundingBox hitBox;
	private List<Block> nearBlocks;
	private Set<LivingEntity> immune;
	private int maxHitLimit;
	private ValidTargetChecker entitySpellChecker;
	private ParticleProjectileTracker tracker;
	private ParticleProjectileSpell spell;
	private Set<Material> groundMaterials;
	private Set<Material> disallowedGroundMaterials;
	private ValidTargetList targetList;
	private ModifierSet projectileModifiers;
	private Map<String, Subspell> interactionSpells;

	private boolean stopped = false;

	// projectile options
	private double maxDuration;
	private double maxDistanceSquared;

	private boolean hugSurface;
	private boolean callEvents;
	private boolean changePitch;
	private boolean controllable;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;
	private boolean allowCasterInteract;
	private boolean powerAffectsVelocity;

	private boolean hitGround;
	private boolean hitAirAtEnd;
	private boolean hitAirDuring;
	private boolean hitAirAfterDuration;

	private float acceleration;
	private float startXOffset;
	private float startYOffset;
	private float startZOffset;
	private float targetYOffset;
	private float ticksPerSecond;
	private float heightFromSurface;
	private float verticalHitRadius;
	private float horizontalHitRadius;

	private float projectileTurn;
	private float projectileVelocity;
	private double verticalRotation;
	private double horizontalRotation;
	private double xRotation;
	private float projectileVertOffset;
	private float projectileVertSpread;
	private float projectileHorizOffset;
	private float projectileHorizSpread;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private int spellInterval;
	private int tickSpellLimit;
	private int maxEntitiesHit;
	private int accelerationDelay;
	private int intermediateEffects;
	private int intermediateHitboxes;
	private int specialEffectInterval;
	private int groundVerticalHitRadius;
	private int groundHorizontalHitRadius;

	private Subspell airSpell;
	private Subspell tickSpell;
	private Subspell entitySpell;
	private Subspell casterSpell;
	private Subspell groundSpell;
	private Subspell durationSpell;
	private Subspell modifierSpell;
	private Subspell entityLocationSpell;

	private int ticks = 0;

	private static final double ANGLE_Y = AccurateMath.toRadians(-90);

	public ParticleProjectileTracker(SpellData data) {
		this.data = data.builder().build();
	}

	public void start(Location from) {
		startTime = System.currentTimeMillis();
		if (!changePitch) from.setPitch(0F);
		startLocation = from.clone();

		// Changing the start location
		Util.applyRelativeOffset(startLocation, from.getDirection().normalize(), startXOffset, startYOffset, startZOffset);

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();
		currentVelocity = from.getDirection();

		initialize();
	}

	public void startTarget(Location from, LivingEntity target) {
		startTarget(from, target.getLocation());
	}

	public void startTarget(Location from, Location target) {
		startTime = System.currentTimeMillis();
		if (!changePitch) from.setPitch(0F);
		startLocation = from.clone();

		// Changing the target location
		Location targetLoc = target.clone();
		targetLoc.add(0, targetYOffset,0);
		Vector dir = targetLoc.clone().subtract(from.clone()).toVector();

		// Changing the start location
		startDirection = dir.clone().normalize();
		Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
		startLocation.add(horizOffset.multiply(startZOffset));
		startLocation.add(startLocation.getDirection().multiply(startXOffset));
		startLocation.setY(startLocation.getY() + startYOffset);

		dir = targetLoc.clone().subtract(startLocation.clone()).toVector();

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();
		currentVelocity = LocationUtil.setDirection(from, dir).getDirection();

		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();
		counter = 0;

		Vector dir = currentVelocity.clone().normalize();
		Vector dirNormalized = dir.clone().normalize();

		Vector angleZ = Util.makeFinite(new Vector(-dirNormalized.getZ(), 0D, dirNormalized.getX()).normalize());
		Vector angleY = Util.makeFinite(dir.clone().rotateAroundAxis(angleZ, ANGLE_Y).normalize());
		Vector angleX = Util.makeFinite(dir.clone());

		if (verticalRotation != 0) currentVelocity.rotateAroundAxis(angleZ, verticalRotation);
		if (horizontalRotation != 0) currentVelocity.rotateAroundAxis(angleY, horizontalRotation);
		if (xRotation != 0) currentVelocity.rotateAroundAxis(angleX, xRotation);

		if (projectileHorizOffset != 0) Util.rotateVector(currentVelocity, projectileHorizOffset);
		if (projectileVertOffset != 0) currentVelocity.add(new Vector(0, projectileVertOffset, 0)).normalize();
		if (projectileVertSpread > 0 || projectileHorizSpread > 0) {
			float rx = -1 + rand.nextFloat() * 2;
			float ry = -1 + rand.nextFloat() * 2;
			float rz = -1 + rand.nextFloat() * 2;
			currentVelocity.add(new Vector(rx * projectileHorizSpread, ry * projectileVertSpread, rz * projectileHorizSpread));
		}

		if (hugSurface) {
			currentLocation.setY(currentLocation.getY() + heightFromSurface);
			currentVelocity.setY(0).normalize();
			currentLocation.setPitch(0);
		}

		if (powerAffectsVelocity) currentVelocity.multiply(data.power());
		currentVelocity.multiply(projectileVelocity / ticksPerSecond);

		nearBlocks = new ArrayList<>();
		immune = new HashSet<>();

		maxHitLimit = 0;
		hitBox = new BoundingBox(currentLocation, horizontalHitRadius, verticalHitRadius);
		LocationUtil.setDirection(currentLocation, currentVelocity);
		tracker = this;

		if (spell != null) {
			effectSet = spell.playEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			entityMap = spell.playEntityEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			armorStandSet = spell.playArmorStandEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			ParticleProjectileSpell.getProjectileTrackers().add(tracker);
		}

		taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
	}

	@Override
	public void run() {
		currentVelocity = Util.makeFinite(currentVelocity);
		currentLocation = Util.makeFinite(currentLocation);
		previousLocation = Util.makeFinite(previousLocation);

		if (data.caster() != null && !data.caster().isValid()) {
			stop();
			return;
		}

		if (zoneManager.willFizzle(currentLocation, spell)) {
			stop();
			return;
		}

		if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
			if (hitAirAfterDuration && durationSpell != null) {
				durationSpell.subcast(data.builder().build());
				if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLocation, data);
			}
			stop();
			return;
		}

		if (projectileModifiers != null) {
			ModifierResult result = projectileModifiers.apply(data.caster(), data);
			data = result.data();

			if (!result.check()) {
				if (modifierSpell != null) modifierSpell.subcast(data.builder().build());
				if (stopOnModifierFail) stop();
				return;
			}
		}

		if (controllable) {
            assert data.caster() != null;
            currentVelocity = data.caster().getLocation().getDirection();
			if (hugSurface) currentVelocity.setY(0).normalize();
			currentVelocity.multiply(projectileVelocity / ticksPerSecond);
			LocationUtil.setDirection(currentLocation, currentVelocity);
		}

		currentVelocity = Util.makeFinite(currentVelocity);

		// Move projectile and apply gravity
		previousLocation = currentLocation.clone();
		currentLocation.add(currentVelocity);

		currentLocation = Util.makeFinite(currentLocation);
		previousLocation = Util.makeFinite(previousLocation);

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) {
				return;
			}
		}

		if (hugSurface && (currentLocation.getBlockX() != currentX || currentLocation.getBlockZ() != currentZ)) {
			Block b = currentLocation.subtract(0, heightFromSurface, 0).getBlock();

			int attempts = 0;
			boolean ok = false;
			while (attempts++ < 10) {
				if (BlockUtils.isPathable(b)) {
					b = b.getRelative(BlockFace.DOWN);
					if (BlockUtils.isPathable(b)) currentLocation.add(0, -1, 0);
					else {
						ok = true;
						break;
					}
				} else {
					b = b.getRelative(BlockFace.UP);
					currentLocation.add(0, 1, 0);
					if (BlockUtils.isPathable(b)) {
						ok = true;
						break;
					}
				}
			}
			if (!ok) {
				stop();
				return;
			}

			currentLocation.setY((int) currentLocation.getY() + heightFromSurface);
			currentX = currentLocation.getBlockX();
			currentZ = currentLocation.getBlockZ();

			// Apply vertical gravity
		} else if (projectileVertGravity != 0) currentVelocity.setY(currentVelocity.getY() - (projectileVertGravity / ticksPerSecond));

		// Apply turn
		if (projectileTurn != 0) Util.rotateVector(currentVelocity, projectileTurn);

		// Apply horizontal gravity
		if (projectileHorizGravity != 0) Util.rotateVector(currentVelocity, (projectileHorizGravity / ticksPerSecond) * counter);

		// Rotate effects properly
		LocationUtil.setDirection(currentLocation, currentVelocity);

		if (effectSet != null) {
			Effect effect;
			Location effectLoc;
			for (EffectlibSpellEffect spellEffect : effectSet) {
				if (spellEffect == null) continue;
				effect = spellEffect.getEffect();
				if (effect == null) continue;

				effectLoc = spellEffect.getSpellEffect().applyOffsets(currentLocation.clone(), data);
				effect.setLocation(effectLoc);

				if (effect instanceof ModifiedEffect mod) {
					Effect modifiedEffect = mod.getInnerEffect();
					if (modifiedEffect != null) modifiedEffect.setLocation(effectLoc);
				}
			}
		}

		if (armorStandSet != null || entityMap != null) {
			// Changing the effect location
			EulerAngle angle;

			Vector dir = currentLocation.getDirection().normalize();
			Vector horizOffset = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
			Location effectLoc = currentLocation.clone();
			effectLoc.add(horizOffset.multiply(effectOffset.getZ()));
			effectLoc.add(effectLoc.getDirection().multiply(effectOffset.getX()));
			effectLoc.setY(effectLoc.getY() + effectOffset.getY());

			effectLoc = Util.makeFinite(effectLoc);

			angle = EulerAngle.ZERO.setX(AccurateMath.toRadians(effectLoc.getPitch()));

			if (armorStandSet != null) {
				for (ArmorStand armorStand : armorStandSet) {
					armorStand.teleportAsync(effectLoc);
					armorStand.setHeadPose(angle);
					armorStand.setLeftArmPose(angle);
					armorStand.setRightArmPose(angle);
				}
			}

			if (entityMap != null) {
				for (var entry : entityMap.entrySet()) {
					entry.getValue().teleportAsync(entry.getKey().applyOffsets(effectLoc.clone()));
				}
			}
		}

		// Play effects
		if (spell != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, currentLocation, data);

		// Acceleration
		if (acceleration != 0 && accelerationDelay > 0 && counter % accelerationDelay == 0) currentVelocity.multiply(acceleration);

		// Intermediate effects
		if (intermediateEffects > 0) playIntermediateEffects(previousLocation, currentVelocity);

		// Intermediate hitboxes
		if (intermediateHitboxes > 0) checkIntermediateHitboxes(previousLocation, currentVelocity);

		if (stopped) return;

		counter++;

		// Cast spell mid air
		if (hitAirDuring && counter % spellInterval == 0 && tickSpell != null) {
			if (tickSpellLimit <= 0 || ticks < tickSpellLimit) {
				tickSpell.subcast(new SpellData(caster, currentLocation.clone(), power, args));
				ticks++;
			}
		}

		if (groundHorizontalHitRadius == 0 || groundVerticalHitRadius == 0) {
			nearBlocks = new ArrayList<>();
			nearBlocks.add(currentLocation.getBlock());
		} else nearBlocks = BlockUtils.getNearbyBlocks(currentLocation, groundHorizontalHitRadius, groundVerticalHitRadius);

		for (Block b : nearBlocks) {
			if (!groundMaterials.contains(b.getType()) || disallowedGroundMaterials.contains(b.getType())) continue;
			if (hitGround && groundSpell != null) {
				Util.setLocationFacingFromVector(previousLocation, currentVelocity);
				groundSpell.subcast(caster, previousLocation, power, args);
				if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLocation, data);
			}
			if (stopOnHitGround) {
				stop();
				return;
			}
		}

		if (currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
			if (hitAirAtEnd && airSpell != null) {
				airSpell.subcast(caster, currentLocation.clone(), power, args);
				if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLocation, data);
			}
			stop();
			return;
		}

		checkHitbox(currentLocation);
		if (stopped) return;

		if (spell == null || interactionSpells == null || interactionSpells.isEmpty()) return;
		Set<ParticleProjectileTracker> toRemove = new HashSet<>();
		Set<ParticleProjectileTracker> trackers = new HashSet<>(ParticleProjectileSpell.getProjectileTrackers());
		for (ParticleProjectileTracker collisionTracker : trackers) {
			if (!canInteractWith(collisionTracker)) continue;

			Subspell collisionSpell = interactionSpells.get(collisionTracker.spell.getInternalName());
			if (collisionSpell == null) {
				toRemove.add(collisionTracker);
				toRemove.add(tracker);
				collisionTracker.stop(false);
				tracker.stop(false);
				continue;
			}

			double x = (tracker.currentLocation.getX() + collisionTracker.currentLocation.getX()) / 2D;
			double y = (tracker.currentLocation.getY() + collisionTracker.currentLocation.getY()) / 2D;
			double z = (tracker.currentLocation.getZ() + collisionTracker.currentLocation.getZ()) / 2D;

			Location middleLoc = new Location(tracker.currentLocation.getWorld(), x, y, z);
			collisionSpell.subcast(tracker.caster, middleLoc, tracker.power, tracker.args);
			toRemove.add(collisionTracker);
			toRemove.add(tracker);
			collisionTracker.stop(false);
			tracker.stop(false);
		}

		ParticleProjectileSpell.getProjectileTrackers().removeAll(toRemove);
		toRemove.clear();
		trackers.clear();
	}

	private boolean canInteractWith(ParticleProjectileTracker collisionTracker) {
		if (collisionTracker == null) return false;
		if (tracker == null) return false;
		if (tracker.data.caster() == null) return false;
		if (collisionTracker.data.caster() == null) return false;
		if (collisionTracker.equals(tracker)) return false;
		if (!interactionSpells.containsKey(collisionTracker.spell.getInternalName())) return false;
		if (!collisionTracker.currentLocation.getWorld().equals(tracker.currentLocation.getWorld())) return false;
		if (!collisionTracker.hitBox.contains(tracker.currentLocation) && !tracker.hitBox.contains(collisionTracker.currentLocation)) return false;
		if (!allowCasterInteract) {
            assert collisionTracker.data.caster() != null;
            return !collisionTracker.data.caster().equals(tracker.data.caster());
        }
		return true;
	}

	private void playIntermediateEffects(Location old, Vector movement) {
		int divideFactor = intermediateEffects + 1;
		Vector v = movement.clone();

		v.setX(v.getX() / divideFactor);
		v.setY(v.getY() / divideFactor);
		v.setZ(v.getZ() / divideFactor);

		for (int i = 0; i < intermediateEffects; i++) {
			old = LocationUtil.setDirection(old.add(v), v);
			if (spell != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, old, data);
		}
	}

	private void checkIntermediateHitboxes(Location old, Vector movement) {
		int divideFactor = intermediateHitboxes + 1;
		Vector v = movement.clone();

		v.setX(v.getX() / divideFactor);
		v.setY(v.getY() / divideFactor);
		v.setZ(v.getZ() / divideFactor);

		for (int i = 0; i < intermediateHitboxes; i++) {
			old = LocationUtil.setDirection(old.add(v), v);
			checkHitbox(old);
			if (stopped) return;
		}
	}

	private void checkHitbox(Location currentLoc) {
		if (currentLoc == null || currentLoc.getWorld() == null) return;

		hitBox.setCenter(currentLoc);

		for (LivingEntity target : currentLoc.getNearbyLivingEntities(horizontalHitRadius, verticalHitRadius)) {
			if (!target.isValid() || immune.contains(target) || !targetList.canTarget(data.caster(), target)) continue;

			ParticleProjectileHitEvent hitEvent = new ParticleProjectileHitEvent(data.caster(), data.target(), tracker, spell, data.power());
			hitEvent.callEvent();

			if (stopped) return;
			if (hitEvent.isCancelled()) continue;

			target = hitEvent.getTarget();
			float subPower = hitEvent.getPower();

			SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data.builder().target(target).power(subPower).build());
			if (!targetEvent.callEvent()) continue;

			target = targetEvent.getTarget();
			subPower = targetEvent.getPower();

			if (casterSpell != null && target.equals(data.caster())) casterSpell.subcast(data.builder().target(target).power(subPower).build());
			if (entitySpell != null && !target.equals(data.caster())) entitySpell.subcast(data.builder().target(target).power(subPower).build());
			if (entityLocationSpell != null) entityLocationSpell.subcast(data.builder().target(target).power(subPower).build());

			if (spell != null)
				spell.playEffects(EffectPosition.TARGET, currentLoc, data.builder().target(target).power(subPower).build());

			immune.add(target);
			maxHitLimit++;

			if (maxEntitiesHit > 0 && maxHitLimit >= maxEntitiesHit) stop();
			break;
		}
	}

	@Override
	public void stop() {
		stop(true);
	}

	public void stop(boolean removeTracker) {
		if (removeTracker && spell != null) ParticleProjectileSpell.getProjectileTrackers().remove(tracker);
		if (spell != null) spell.playEffects(EffectPosition.DELAYED, currentLocation, data);
		MagicSpells.cancelTask(taskId);
		if (effectSet != null) {
			for (EffectlibSpellEffect spellEffect : effectSet) {
				if (spellEffect == null) continue;
				if (spellEffect.getEffect() == null) continue;
				spellEffect.getEffect().cancel();
			}
			effectSet.clear();
		}
		if (armorStandSet != null) {
			for (ArmorStand armorStand : armorStandSet) {
				armorStand.remove();
			}
			armorStandSet.clear();
		}
		if (entityMap != null) {
			for (Entity entity : entityMap.values()) {
				entity.remove();
			}
			entityMap.clear();
		}
		data.caster(null);
		startLocation = null;
		previousLocation = null;
		currentLocation = null;
		currentVelocity = null;
		if (immune != null) {
			immune.clear();
			immune = null;
		}
		stopped = true;
	}

	public boolean isStopped() {
		return stopped;
	}

	public LivingEntity getCaster() {
		return data.caster();
	}

	public void setCaster(LivingEntity caster) {
		data.caster(caster);
	}

	public float getPower() {
		return data.power();
	}

	public void setPower(float power) {
		data.power(power);
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public Vector getCurrentVelocity() {
		return currentVelocity;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public Set<LivingEntity> getImmune() {
		return immune;
	}

	public ParticleProjectileTracker getTracker() {
		return tracker;
	}

	public void setTracker(ParticleProjectileTracker tracker) {
		this.tracker = tracker;
	}

	public ParticleProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ParticleProjectileSpell spell) {
		this.spell = spell;
	}

	public void setGroundMaterials(Set<Material> groundMaterials) {
		this.groundMaterials = groundMaterials;
	}

	public void setDisallowedGroundMaterials(Set<Material> disallowedGroundMaterials) {
		this.disallowedGroundMaterials = disallowedGroundMaterials;
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

	public void setProjectileModifiers(ModifierSet projectileModifiers) {
		this.projectileModifiers = projectileModifiers;
	}

	public void setInteractionSpells(Map<String, Subspell> interactionSpells) {
		this.interactionSpells = interactionSpells;
	}

	public void setMaxDuration(double maxDuration) {
		this.maxDuration = maxDuration;
	}

	public void setMaxDistanceSquared(double maxDistanceSquared) {
		this.maxDistanceSquared = maxDistanceSquared;
	}

	public void setHugSurface(boolean hugSurface) {
		this.hugSurface = hugSurface;
	}

	public void setChangePitch(boolean changePitch) {
		this.changePitch = changePitch;
	}

	public void setControllable(boolean controllable) {
		this.controllable = controllable;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public void setStopOnHitGround(boolean stopOnHitGround) {
		this.stopOnHitGround = stopOnHitGround;
	}

	public void setStopOnModifierFail(boolean stopOnModifierFail) {
		this.stopOnModifierFail = stopOnModifierFail;
	}

	public void setAllowCasterInteract(boolean allowCasterInteract) {
		this.allowCasterInteract = allowCasterInteract;
	}

	public void setPowerAffectsVelocity(boolean powerAffectsVelocity) {
		this.powerAffectsVelocity = powerAffectsVelocity;
	}

	public void setHitGround(boolean hitGround) {
		this.hitGround = hitGround;
	}

	public void setHitAirAtEnd(boolean hitAirAtEnd) {
		this.hitAirAtEnd = hitAirAtEnd;
	}

	public void setHitAirDuring(boolean hitAirDuring) {
		this.hitAirDuring = hitAirDuring;
	}

	public void setHitAirAfterDuration(boolean hitAirAfterDuration) {
		this.hitAirAfterDuration = hitAirAfterDuration;
	}

	public void setAcceleration(float acceleration) {
		this.acceleration = acceleration;
	}

	public void setProjectileTurn(float projectileTurn) {
		this.projectileTurn = projectileTurn;
	}

	public void setProjectileVelocity(float projectileVelocity) {
		this.projectileVelocity = projectileVelocity;
	}

	public void setVerticalRotation(double verticalRotation) {
		this.verticalRotation = verticalRotation;
	}

	public void setHorizontalRotation(double horizontalRotation) {
		this.horizontalRotation = horizontalRotation;
	}

	public void setXRotation(double xRotation) {
		this.xRotation = xRotation;
	}

	public void setProjectileVertOffset(float projectileVertOffset) {
		this.projectileVertOffset = projectileVertOffset;
	}

	public void setProjectileVertSpread(float projectileVertSpread) {
		this.projectileVertSpread = projectileVertSpread;
	}

	public void setProjectileHorizOffset(float projectileHorizOffset) {
		this.projectileHorizOffset = projectileHorizOffset;
	}

	public void setProjectileHorizSpread(float projectileHorizSpread) {
		this.projectileHorizSpread = projectileHorizSpread;
	}

	public void setProjectileVertGravity(float projectileVertGravity) {
		this.projectileVertGravity = projectileVertGravity;
	}

	public void setProjectileHorizGravity(float projectileHorizGravity) {
		this.projectileHorizGravity = projectileHorizGravity;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	public void setSpellInterval(int spellInterval) {
		this.spellInterval = spellInterval;
	}

	public void setTickSpellLimit(int tickSpellLimit) {
		this.tickSpellLimit = tickSpellLimit;
	}

	public void setMaxEntitiesHit(int maxEntitiesHit) {
		this.maxEntitiesHit = maxEntitiesHit;
	}

	public void setAccelerationDelay(int accelerationDelay) {
		this.accelerationDelay = accelerationDelay;
	}

	public void setIntermediateEffects(int intermediateEffects) {
		this.intermediateEffects = intermediateEffects;
	}

	public void setIntermediateHitboxes(int intermediateHitboxes) {
		this.intermediateHitboxes = intermediateHitboxes;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public void setGroundVerticalHitRadius(int groundVerticalHitRadius) {
		this.groundVerticalHitRadius = groundVerticalHitRadius;
	}

	public void setGroundHorizontalHitRadius(int groundHorizontalHitRadius) {
		this.groundHorizontalHitRadius = groundHorizontalHitRadius;
	}

	public void setStartXOffset(float startXOffset) {
		this.startXOffset = startXOffset;
	}

	public void setStartYOffset(float startYOffset) {
		this.startYOffset = startYOffset;
	}

	public void setStartZOffset(float startZOffset) {
		this.startZOffset = startZOffset;
	}

	public void setTargetYOffset(float targetYOffset) {
		this.targetYOffset = targetYOffset;
	}

	public void setEffectOffset(Vector effectOffset) {
		this.effectOffset = effectOffset;
	}

	public void setTicksPerSecond(float ticksPerSecond) {
		this.ticksPerSecond = ticksPerSecond;
	}

	public void setHeightFromSurface(float heightFromSurface) {
		this.heightFromSurface = heightFromSurface;
	}

	public void setVerticalHitRadius(float verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}

	public void setHorizontalHitRadius(float horizontalHitRadius) {
		this.horizontalHitRadius = horizontalHitRadius;
	}

	public void setAirSpell(Subspell airSpell) {
		this.airSpell = airSpell;
	}

	public void setTickSpell(Subspell tickSpell) {
		this.tickSpell = tickSpell;
	}

	public void setEntitySpell(Subspell entitySpell) {
		this.entitySpell = entitySpell;
	}

	public void setCasterSpell(Subspell casterSpell) {
		this.casterSpell = casterSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

	public void setDurationSpell(Subspell durationSpell) {
		this.durationSpell = durationSpell;
	}

	public void setModifierSpell(Subspell modifierSpell) {
		this.modifierSpell = modifierSpell;
	}

	public void setEntityLocationSpell(Subspell entityLocationSpell) {
		this.entityLocationSpell = entityLocationSpell;
	}

	public String[] getArgs() {
		return data.args();
	}

	public SpellData getSpellData() {
		return data;
	}

}
