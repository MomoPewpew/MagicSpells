package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.projectile.ProjectileManager;
import com.nisovin.magicspells.util.projectile.ProjectileManagers;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class HomingProjectileSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private HomingProjectileSpell thisSpell;

	private NoMagicZoneManager zoneManager;

	private List<HomingProjectileMonitor> monitors;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> airSpellInterval;
	private ConfigData<Integer> specialEffectInterval;
	private ConfigData<Integer> intermediateSpecialEffects;

	private ConfigData<Float> velocity;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> verticalHitRadius;

	private boolean stopOnModifierFail;
	private boolean powerAffectsVelocity;

	private ConfigData<Double> maxDuration;

	private String hitSpellName;
	private String airSpellName;
	private String groundSpellName;
	private String modifierSpellName;
	private String durationSpellName;

	private Component projectileName;

	private Subspell hitSpell;
	private Subspell airSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;

	private ModifierSet homingModifiers;
	private List<String> homingModifiersStrings;

	public HomingProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		thisSpell = this;

		monitors = new ArrayList<>();

		projectileManager = ProjectileManagers.getManager(getConfigString("projectile-type",  "arrow"));

		relativeOffset = getConfigVector("relative-offset", "0.5,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

		tickInterval = getConfigDataInt("tick-interval", 1);
		airSpellInterval = getConfigDataInt("spell-interval", 20);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 0);
		intermediateSpecialEffects = getConfigDataInt("intermediate-special-effect-locations", 0);

		velocity = getConfigDataFloat("velocity", 1F);
		hitRadius = getConfigDataFloat("hit-radius", 2F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", 2F);

		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);
		powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);

		maxDuration = getConfigDataDouble("max-duration", 10);

		hitSpellName = getConfigString("spell", "");
		airSpellName = getConfigString("spell-on-hit-air", "");
		projectileName = Util.getMiniMessage(getConfigString("projectile-name", ""));
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");

		homingModifiersStrings = getConfigStringList("homing-modifiers", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (homingModifiersStrings != null && !homingModifiersStrings.isEmpty()) {
			homingModifiers = new ModifierSet(homingModifiersStrings, this);
			homingModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			hitSpell = null;
			if (!hitSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
		}

		airSpell = new Subspell(airSpellName);
		if (!airSpell.process()) {
			airSpell = null;
			if (!airSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-hit-air defined!");
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process()) {
			durationSpell = null;
			if (!durationSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-after-duration defined!");
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process()) {
			if (!modifierSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
		}

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public void turnOff() {
		for (HomingProjectileMonitor monitor : monitors) {
			monitor.stop();
		}

		monitors.clear();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);

			new HomingProjectileMonitor(data.builder().target(targetInfo.target()).power(targetInfo.getPower()).build());
			sendMessages(data.caster(), targetInfo.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		new HomingProjectileMonitor(data);
		return true;
	}

	@EventHandler
	public void onProjectileHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity entity)) return;
		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Projectile projectile)) return;

		for (HomingProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;
			if (monitor.data.target() == null) continue;
			if (!monitor.data.target().equals(entity)) continue;

			if (hitSpell != null) hitSpell.subcast(monitor.data.builder().target(entity).build());
			playSpellEffects(EffectPosition.TARGET, entity, monitor.data);
			event.setCancelled(true);

			monitor.stop();
			break;
		}
	}

	@EventHandler
	public void onProjectileBlockHit(ProjectileHitEvent e) {
		Projectile projectile = e.getEntity();
		Block block = e.getHitBlock();
		if (block == null) return;
		for (HomingProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;
			if (monitor.data.caster() == null) continue;
			if (groundSpell != null) groundSpell.subcast(monitor.data.builder().location(projectile.getLocation()).build());
			monitor.stop();
		}

	}

	private class HomingProjectileMonitor implements Runnable {

		private Projectile projectile;
		private SpellData data;
		private Location currentLocation;
		private Location previousLocation;
		private Location startLocation;
		private BoundingBox hitBox;
		private Vector currentVelocity;
		private long startTime;

		private int airSpellInterval;
		private int specialEffectInterval;
		private int intermediateSpecialEffects;

		private float velocity;

		private double maxDuration;

		private int taskId;
		private int counter = 0;

		private HomingProjectileMonitor(SpellData data) {
			this.data = data;
			startLocation = data.caster().getLocation();

			initialize(data);
		}

		private void initialize(SpellData data) {
			startTime = System.currentTimeMillis();

			Vector startDir = startLocation.clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
			startLocation.setY(startLocation.getY() + relativeOffset.getY());

			airSpellInterval = HomingProjectileSpell.this.airSpellInterval.get(data);
			specialEffectInterval = HomingProjectileSpell.this.specialEffectInterval.get(data);

			intermediateSpecialEffects = HomingProjectileSpell.this.intermediateSpecialEffects.get(data);
			if (intermediateSpecialEffects < 0) intermediateSpecialEffects = 0;

			velocity = HomingProjectileSpell.this.velocity.get(data);
			if (powerAffectsVelocity) velocity *= data.power();

			maxDuration = HomingProjectileSpell.this.maxDuration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND;

			float hitRadius = HomingProjectileSpell.this.hitRadius.get(data);
			float verticalHitRadius = HomingProjectileSpell.this.verticalHitRadius.get(data);
			hitBox = new BoundingBox(startLocation, hitRadius, verticalHitRadius);

			playSpellEffects(EffectPosition.CASTER, startLocation, data);

			projectile = startLocation.getWorld().spawn(startLocation, projectileManager.getProjectileClass());

			currentLocation = startLocation.clone();

			if (projectileName != null) {
				projectile.customName(projectileName);
				projectile.setCustomNameVisible(true);
			}

			currentVelocity = data.target().getLocation().add(0, 0.75, 0).toVector().subtract(projectile.getLocation().toVector()).normalize();
			currentVelocity.multiply(velocity);
			currentVelocity.setY(currentVelocity.getY() + 0.15);
			projectile.setVelocity(currentVelocity);

			playSpellEffects(EffectPosition.PROJECTILE, projectile, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), data.caster(), projectile, data);
			monitors.add(this);

			int tickInterval = HomingProjectileSpell.this.tickInterval.get(data);
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
		}

		@Override
		public void run() {
			LivingEntity caster = data.caster();
			LivingEntity target = data.target();

			if ((caster != null && !caster.isValid()) || !target.isValid()) {
				stop();
				return;
			}

			if (projectile == null || projectile.isDead()) {
				stop();
				return;
			}

			if (!projectile.getLocation().getWorld().equals(target.getWorld())) {
				stop();
				return;
			}

			if (zoneManager.willFizzle(currentLocation, thisSpell)) {
				stop();
				return;
			}

			if (homingModifiers != null) {
				ModifierResult result = homingModifiers.apply(caster, data);
				data = result.data();

				if (!result.check()) {
					if (modifierSpell != null) modifierSpell.subcast(data.builder().location(currentLocation).build());

					if (stopOnModifierFail) stop();
					return;
				}
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (durationSpell != null) durationSpell.subcast(data.builder().location(currentLocation).build());
				stop();
				return;
			}

			previousLocation = projectile.getLocation();

			Vector oldVelocity = new Vector(currentVelocity.getX(), currentVelocity.getY(), currentVelocity.getZ());

			Location targetLoc = target.getLocation().clone();
			Vector startDir = targetLoc.clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ())).getBlock().getLocation();
			targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
			targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());

			currentVelocity = targetLoc.toVector().subtract(projectile.getLocation().toVector()).normalize();
			currentVelocity.multiply(velocity);
			currentVelocity.setY(currentVelocity.getY() + 0.15);
			projectile.setVelocity(currentVelocity);
			currentLocation = projectile.getLocation();

			if (counter % airSpellInterval == 0 && airSpell != null) airSpell.subcast(data.builder().location(currentLocation).build());

			if (intermediateSpecialEffects > 0) playIntermediateEffectLocations(previousLocation, oldVelocity);

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) playSpellEffects(EffectPosition.SPECIAL, currentLocation, data);

			counter++;

			hitBox.setCenter(currentLocation);
			if (hitBox.contains(targetLoc)) {
				SpellTargetEvent targetEvent = new SpellTargetEvent(thisSpell, data);
				if (!targetEvent.callEvent()) return;

				LivingEntity subTarget = targetEvent.getTarget();
				float subPower = targetEvent.getPower();

				playSpellEffects(EffectPosition.TARGET, subTarget, data.builder().target(subTarget).power(subPower).build());
				if (hitSpell != null) hitSpell.subcast(data.builder().target(subTarget).power(subPower).build());
				stop();
			}
		}

		private void playIntermediateEffectLocations(Location old, Vector movement) {
			int divideFactor = intermediateSpecialEffects + 1;
			movement.setX(movement.getX() / divideFactor);
			movement.setY(movement.getY() / divideFactor);
			movement.setZ(movement.getZ() / divideFactor);
			for (int i = 0; i < intermediateSpecialEffects; i++) {
				old = old.add(movement).setDirection(movement);
				playSpellEffects(EffectPosition.SPECIAL, old, data);
			}
		}

		private void stop() {
			playSpellEffects(EffectPosition.DELAYED, currentLocation, data);
			MagicSpells.cancelTask(taskId);
			data.caster(null);
			data.target(null);
			currentLocation = null;
			if (projectile != null) projectile.remove();
			projectile = null;
		}

	}

}
