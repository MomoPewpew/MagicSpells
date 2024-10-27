package com.nisovin.magicspells.util.trackers;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.instant.ItemProjectileSpell;

public class ItemProjectileTracker implements Runnable, Tracker {

	private ItemProjectileSpell spell;

	private SpellData data;

	private Component itemName;

	private ItemStack item;

	private int spellDelay;
	private int pickupDelay;
	private int removeDelay;
	private int tickInterval;
	private int spellInterval;
	private int itemNameDelay;
	private int specialEffectInterval;
	private int entityHitDelay;

	private float speed;
	private float yOffset;
	private float hitRadius;
	private float vertSpeed;
	private float vertHitRadius;
	private float targetYOffset;
	private float rotationOffset;

	private boolean callEvents;
	private boolean changePitch;
	private boolean vertSpeedUsed;
	private boolean stopOnHitGround;
	private boolean stopOnHitEntity;
	private boolean projectileHasGravity;

	private Vector relativeOffset;

	private float startXOffset;
	private float startYOffset;
	private float startZOffset;

	private Subspell spellOnTick;
	private Subspell spellOnDelay;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	private NoMagicZoneManager zoneManager;

	private ValidTargetList targetList;

	private Item entity;
	private Vector velocity;
	private Location currentLocation;
	private Location previousLocation;

	private boolean landed = false;
	private boolean groundSpellCasted = false;
	private boolean stopped = false;

	private int taskId;
	private int count = 0;

	public ItemProjectileTracker(SpellData data) {
		this.data = data;
	}

	public void start() {
		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();

		//relativeOffset
        assert data.location() != null;
        Location startLocation = data.location().clone();

		Vector startDirection = startLocation.getDirection().normalize();
		Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
		startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
		startLocation.setY(startLocation.getY() + relativeOffset.getY());

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();

		if (vertSpeedUsed) velocity = startLocation.clone().getDirection().setY(0).multiply(speed).setY(vertSpeed);
		else velocity = startLocation.clone().getDirection().multiply(speed);
		Util.rotateVector(velocity, rotationOffset);
		entity = startLocation.getWorld().dropItem(startLocation, item.clone());
		entity.setGravity(projectileHasGravity);
		entity.setPickupDelay(pickupDelay);
		entity.setVelocity(velocity);

		if (spell != null) {
			spell.playEffects(EffectPosition.CASTER, data.caster(), data);
			spell.playEffects(EffectPosition.PROJECTILE, entity, data);
			spell.playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, entity.getLocation(), data.caster(), entity, data);
		}

		taskId = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);

		if (itemName != null) {
			MagicSpells.scheduleDelayedTask(() -> {
				entity.customName(itemName);
				entity.setCustomNameVisible(true);
			}, itemNameDelay);
		}

		MagicSpells.scheduleDelayedTask(this::stop, removeDelay);
	}

	@Override
	public void run() {
		if (entity == null || !entity.isValid() || entity.isDead()) {
			stop();
			return;
		}

		count++;

		previousLocation = currentLocation.clone();
		currentLocation = entity.getLocation();
		currentLocation.setDirection(entity.getVelocity());

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) {
				return;
			}
		}

		if (spell != null && specialEffectInterval > 0 && count % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, currentLocation, data);

		if (zoneManager.willFizzle(currentLocation, spell)) {
			stop();
			return;
		}

		if (count % spellInterval == 0 && spellOnTick != null) {
			spellOnTick.subcast(data.builder().location(currentLocation).build());
		}

		for (Entity e : entity.getNearbyEntities(count > entityHitDelay ? hitRadius : 0.1, count > entityHitDelay ? vertHitRadius : 0.1, count > entityHitDelay ? hitRadius : 0.1)) {
			if (!(e instanceof LivingEntity target)) continue;
			if (!targetList.canTarget(data.caster(), e)) continue;

			SpellTargetEvent event = new SpellTargetEvent(spell, data.builder().target(target).build());
			if (!event.callEvent()) continue;

			target = event.getTarget();
			float subPower = event.getPower();

			if (spell != null) spell.playEffects(EffectPosition.TARGET, target, data.builder().target(target).power(subPower).build());
			if (spellOnHitEntity != null) spellOnHitEntity.subcast(data.builder().target(target).power(subPower).build());
			if (stopOnHitEntity) stop();
			return;
		}

		if (entity.isOnGround()) {
			if (spellOnHitGround != null && !groundSpellCasted) {
				spellOnHitGround.subcast(data.builder().location(entity.getLocation()).build());
				groundSpellCasted = true;
			}
			if (stopOnHitGround) {
				stop();
				return;
			}
			if (!landed) MagicSpells.scheduleDelayedTask(() -> {
				if (spellOnDelay != null) spellOnDelay.subcast(data.builder().location(entity.getLocation()).build());
				stop();
			}, spellDelay);
			landed = true;
		}
	}

	@Override
	public void stop() {
		stop(true);
	}

	public void stop(boolean removeTracker) {
		if (spell != null) {
			if (entity != null) spell.playEffects(EffectPosition.DELAYED, entity.getLocation(), data);
			if (removeTracker) ItemProjectileSpell.getProjectileTrackers().remove(this);
		}
		if (entity != null) entity.remove();
		MagicSpells.cancelTask(taskId);
		stopped = true;
	}

	public LivingEntity getCaster() {
		return data.caster();
	}

	public void setCaster(LivingEntity caster) {
		data.caster(caster);
	}

	public Item getEntity() {
		return entity;
	}

	public void setEntity(Item entity) {
		this.entity = entity;
	}

	public Vector getVelocity() {
		return velocity;
	}

	public void setVelocity(Vector velocity) {
		this.velocity = velocity;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public float getPower() {
		return data.power();
	}

	public void setPower(float power) {
		data.power(power);
	}

	public void setItemName(Component itemName) {
		this.itemName = itemName;
	}

	public ItemStack getItem() {
		return item;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	public void setSpellDelay(int spellDelay) {
		this.spellDelay = spellDelay;
	}

	public void setPickupDelay(int pickupDelay) {
		this.pickupDelay = pickupDelay;
	}

	public void setRemoveDelay(int removeDelay) {
		this.removeDelay = removeDelay;
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

	public void setItemNameDelay(int itemNameDelay) {
		this.itemNameDelay = itemNameDelay;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public void setentityHitDelay(int entityHitDelay) {
		this.entityHitDelay = entityHitDelay;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public void setYOffset(float yOffset) {
		this.yOffset = yOffset;
	}

	public float getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(float hitRadius) {
		this.hitRadius = hitRadius;
	}

	public void setVertSpeed(float vertSpeed) {
		this.vertSpeed = vertSpeed;
	}

	public void setVertHitRadius(float vertHitRadius) {
		this.vertHitRadius = vertHitRadius;
	}

	public void setRotationOffset(float rotationOffset) {
		this.rotationOffset = rotationOffset;
	}

	public void setVertSpeedUsed(boolean vertSpeedUsed) {
		this.vertSpeedUsed = vertSpeedUsed;
	}

	public void setStopOnHitGround(boolean stopOnHitGround) {
		this.stopOnHitGround = stopOnHitGround;
	}

	public void setStopOnHitEntity(boolean stopOnHitEntity) {
		this.stopOnHitEntity = stopOnHitEntity;
	}

	public void setProjectileHasGravity(boolean projectileHasGravity) {
		this.projectileHasGravity = projectileHasGravity;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public void setSpellOnTick(Subspell spellOnTick) {
		this.spellOnTick = spellOnTick;
	}

	public void setSpellOnDelay(Subspell spellOnDelay) {
		this.spellOnDelay = spellOnDelay;
	}

	public void setSpellOnHitEntity(Subspell spellOnHitEntity) {
		this.spellOnHitEntity = spellOnHitEntity;
	}

	public void setSpellOnHitGround(Subspell spellOnHitGround) {
		this.spellOnHitGround = spellOnHitGround;
	}

	public ItemProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ItemProjectileSpell spell) {
		this.spell = spell;
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

}
