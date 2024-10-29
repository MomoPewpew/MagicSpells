package com.nisovin.magicspells.util.trackers;

import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.instant.ProjectileSpell;
import com.nisovin.magicspells.util.projectile.ProjectileManager;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;

public class ProjectileTracker implements Runnable, Tracker {

	private final Random rand = ThreadLocalRandom.current();

	private Set<EffectlibSpellEffect> effectSet;
	private Map<SpellEffect, Entity> entityMap;

	private ProjectileSpell spell;

	private NoMagicZoneManager zoneManager;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;
	private Vector effectOffset;

	private int tickInterval;
	private int tickSpellInterval;
	private int specialEffectInterval;
	private int intermediateEffects;
	private int intermediateHitboxes;

	private float rotation;
	private float velocity;
	private float hitRadius;
	private float vertSpread;
	private float horizSpread;
	private float verticalHitRadius;

	private boolean visible;
	private boolean gravity;
	private boolean charged;
	private boolean incendiary;
	private boolean callEvents;
	private boolean stopOnModifierFail;

	private double maxDuration;

	private Component projectileName;

	private Subspell hitSpell;
	private Subspell tickSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;
	private Subspell entityLocationSpell;

	private ModifierSet projectileModifiers;

	private Projectile projectile;
	private Location previousLocation;
	private Location currentLocation;
	private Vector currentVelocity;
	private SpellData data;
	private long startTime;

	private ValidTargetList targetList;

	private int taskId;
	private int counter = 0;

	private boolean stopped = false;

	public ProjectileTracker(SpellData data) {
		this.data = data.builder().build();
	}

	public void start() {
		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();
		startTime = System.currentTimeMillis();
		taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);

        assert data.location() != null;
        Location startLocation = data.location().clone();

		Vector startDir = startLocation.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0D, startDir.getX()).normalize();
		startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
		startLocation.setY(startLocation.getY() + relativeOffset.getY());

		currentLocation = startLocation.clone();

		projectile = startLocation.getWorld().spawn(startLocation, projectileManager.getProjectileClass());
		currentVelocity = startLocation.getDirection();
		currentVelocity.multiply(velocity * data.power());
		if (rotation != 0) Util.rotateVector(currentVelocity, rotation);
		if (horizSpread > 0 || vertSpread > 0) {
			float rx = -1 + rand.nextFloat() * 2;
			float ry = -1 + rand.nextFloat() * 2;
			float rz = -1 + rand.nextFloat() * 2;
			currentVelocity.add(new Vector(rx * horizSpread, ry * vertSpread, rz * horizSpread));
		}

		projectile.setVisibleByDefault(visible);
		projectile.setVelocity(currentVelocity);
		projectile.setGravity(gravity);
		projectile.setShooter(data.caster());
		if (projectileName != null && !Util.getPlainString(projectileName).isEmpty()) {
			projectile.customName(projectileName);
			projectile.setCustomNameVisible(true);
		}
		if (projectile instanceof WitherSkull witherSkull) witherSkull.setCharged(charged);
		if (projectile instanceof Explosive explosive) explosive.setIsIncendiary(incendiary);

		if (spell != null) {
			spell.playEffects(EffectPosition.CASTER, startLocation, data);
			effectSet = spell.playEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			entityMap = spell.playEntityEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			spell.playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), data.caster(), projectile, data);
		}
		ProjectileSpell.getProjectileTrackers().add(this);
	}

	@Override
	public void run() {
		if ((data.caster() != null && !data.caster().isValid())) {
			stop();
			return;
		}

		if (projectile == null || projectile.isDead()) {
			stop();
			return;
		}

		if (zoneManager.willFizzle(currentLocation, spell)) {
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

		if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
			if (durationSpell != null) durationSpell.subcast(data.builder().build());
			stop();
			return;
		}

		previousLocation = currentLocation.clone();
		currentLocation = projectile.getLocation().clone();
		currentLocation.setDirection(projectile.getVelocity());

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) return;
		}

		if (counter % tickSpellInterval == 0 && tickSpell != null) tickSpell.subcast(data.builder().build());

		if (spell != null) {
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, currentLocation, data);
			if (intermediateEffects > 0) playIntermediateEffects(previousLocation, currentVelocity);
		}

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

		if (entityMap != null) {
			// Changing the effect location
			Vector dir = currentLocation.getDirection().normalize();
			Vector horizOffset = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
			Location effectLoc = currentLocation.clone();
			effectLoc.add(horizOffset.multiply(effectOffset.getZ()));
			effectLoc.add(effectLoc.getDirection().multiply(effectOffset.getX()));
			effectLoc.setY(effectLoc.getY() + effectOffset.getY());

			effectLoc = Util.makeFinite(effectLoc);

			for (var entry : entityMap.entrySet()) {
				entry.getValue().teleportAsync(entry.getKey().applyOffsets(effectLoc.clone()));
			}
		}

		counter++;

		if (intermediateHitboxes > 0) checkIntermediateHitboxes(previousLocation, currentVelocity);
		checkHitbox(currentLocation);
	}

	public void playIntermediateEffects(Location old, Vector movement) {
		if (old == null) return;
		int divideFactor = intermediateEffects + 1;
		Vector v = movement.clone();

		v.setX(v.getX() / divideFactor);
		v.setY(v.getY() / divideFactor);
		v.setZ(v.getZ() / divideFactor);

		for (int i = 0; i < intermediateEffects; i++) {
			old = old.add(v).setDirection(v);
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, old, data);
		}
	}

	public void checkIntermediateHitboxes(Location old, Vector movement) {
		if (old == null) return;
		int divideFactor = intermediateHitboxes + 1;
		Vector v = movement.clone();

		v.setX(v.getX() / divideFactor);
		v.setY(v.getY() / divideFactor);
		v.setZ(v.getZ() / divideFactor);

		for (int i = 0; i < intermediateHitboxes; i++) {
			old = old.add(v).setDirection(v);
			checkHitbox(old);
		}
	}

	public void checkHitbox(Location location) {
		if (location == null) return;
		if (data.caster() == null) return;
		for (LivingEntity entity : projectile.getLocation().getNearbyLivingEntities(hitRadius, verticalHitRadius, hitRadius)) {
			if (!targetList.canTarget(data.caster(), entity)) continue;

			SpellTargetEvent event = new SpellTargetEvent(spell, data.builder().build());
			if (!event.callEvent()) continue;

			if (hitSpell != null) hitSpell.subcast(data.builder().build());
			if (entityLocationSpell != null) entityLocationSpell.subcast(data.builder().build());

			stop();
			return;
		}
	}

	@Override
	public void stop() {
		stop(true);
	}

	public void stop(boolean removeTracker) {
		if (spell != null) {
			spell.playEffects(EffectPosition.DELAYED, currentLocation, data);
			if (removeTracker) ProjectileSpell.getProjectileTrackers().remove(this);
		}
		MagicSpells.cancelTask(taskId);
		if (effectSet != null) {
			for (EffectlibSpellEffect spellEffect : effectSet) {
				if (spellEffect == null) continue;
				if (spellEffect.getEffect() == null) continue;
				spellEffect.getEffect().cancel();
			}
			effectSet.clear();
		}
		if (entityMap != null) {
			for (Entity entity : entityMap.values()) {
				entity.remove();
			}
			entityMap.clear();
		}
		data.caster(null);
		currentLocation = null;
		if (projectile != null) projectile.remove();
		projectile = null;
		stopped = true;
	}

	public ProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ProjectileSpell spell) {
		this.spell = spell;
	}

	public NoMagicZoneManager getZoneManager() {
		return zoneManager;
	}

	public void setZoneManager(NoMagicZoneManager zoneManager) {
		this.zoneManager = zoneManager;
	}

	public void setProjectileManager(ProjectileManager projectileManager) {
		this.projectileManager = projectileManager;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public void setEffectOffset(Vector effectOffset) {
		this.effectOffset = effectOffset;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	public void setTickSpellInterval(int tickSpellInterval) {
		this.tickSpellInterval = tickSpellInterval;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public void setIntermediateEffects(int intermediateEffects) {
		this.intermediateEffects = intermediateEffects;
	}
	
	public void setIntermediateHitboxes(int intermediateHitboxes) {
		this.intermediateHitboxes = intermediateHitboxes;
	}
	
	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public float getVelocity() {
		return velocity;
	}

	public void setVelocity(float velocity) {
		this.velocity = velocity;
	}

	public float getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(float hitRadius) {
		this.hitRadius = hitRadius;
	}
	
	public void setVertSpread(float vertSpread) {
		this.vertSpread = vertSpread;
	}
	
	public void setHorizSpread(float horizSpread) {
		this.horizSpread = horizSpread;
	}
	
	public void setVerticalHitRadius(float verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setGravity(boolean gravity) {
		this.gravity = gravity;
	}
	
	public void setCharged(boolean charged) {
		this.charged = charged;
	}
	
	public void setIncendiary(boolean incendiary) {
		this.incendiary = incendiary;
	}
	
	public void setStopOnModifierFail(boolean stopOnModifierFail) {
		this.stopOnModifierFail = stopOnModifierFail;
	}
	
	public void setMaxDuration(double maxDuration) {
		this.maxDuration = maxDuration;
	}
	
	public void setProjectileName(Component projectileName) {
		this.projectileName = projectileName;
	}

	public Subspell getHitSpell() {
		return hitSpell;
	}

	public void setHitSpell(Subspell hitSpell) {
		this.hitSpell = hitSpell;
	}
	
	public void setTickSpell(Subspell tickSpell) {
		this.tickSpell = tickSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}
	
	public void setModifierSpell(Subspell modifierSpell) {
		this.modifierSpell = modifierSpell;
	}

	public void setDurationSpell(Subspell durationSpell) {
		this.durationSpell = durationSpell;
	}

	public void setEntityLocationSpell(Subspell entityLocationSpell) {
		this.entityLocationSpell = entityLocationSpell;
	}

	public void setProjectileModifiers(ModifierSet projectileModifiers) {
		this.projectileModifiers = projectileModifiers;
	}

	public Projectile getProjectile() {
		return projectile;
	}

	public LivingEntity getCaster() {
		return data.caster();
	}

	public float getPower() {
		return data.power();
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public String[] getArgs() {
		return data.args();
	}

    public SpellData getSpellData() {
		return data;
    }
}
