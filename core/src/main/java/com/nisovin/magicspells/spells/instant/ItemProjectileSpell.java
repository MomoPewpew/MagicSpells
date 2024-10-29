package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.trackers.ItemProjectileTracker;

public class ItemProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private static Set<ItemProjectileTracker> trackerSet;

	private final String spellOnTickName;
	private final String spellOnDelayName;
	private final String spellOnHitEntityName;
	private final String spellOnHitGroundName;

	private ConfigData<String> magicItemName;

	private Component itemName;

	private ConfigData<Integer> spellDelay;
	private ConfigData<Integer> pickupDelay;
	private ConfigData<Integer> removeDelay;
	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> spellInterval;
	private ConfigData<Integer> itemNameDelay;
	private ConfigData<Integer> specialEffectInterval;
	private ConfigData<Integer> entityHitDelay;

	private ConfigData<Float> speed;
	private ConfigData<Float> yOffset;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> vertSpeed;
	private ConfigData<Float> vertHitRadius;
	private ConfigData<Float> rotationOffset;

	private boolean checkPlugins;
	private boolean stopOnHitGround;
	private boolean stopOnHitEntity;
	private boolean projectileHasGravity;

	private Vector relativeOffset;

	private Subspell spellOnTick;
	private Subspell spellOnDelay;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	private NoMagicZoneManager zoneManager;

	public ItemProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		magicItemName = getConfigDataString("item", "iron_sword");

		spellDelay = getConfigDataInt("spell-delay", 40);
		pickupDelay = getConfigDataInt("pickup-delay", 100);
		removeDelay = getConfigDataInt("remove-delay", 100);
		tickInterval = getConfigDataInt("tick-interval", 1);
		spellInterval = getConfigDataInt("spell-interval", 2);
		itemNameDelay = getConfigDataInt("item-name-delay", 10);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 2);
		entityHitDelay = getConfigDataInt("entity-hit-delay", 0);

		speed = getConfigDataFloat("speed", 1F);
		yOffset = getConfigDataFloat("y-offset", 0F);
		hitRadius = getConfigDataFloat("hit-radius", 1F);
		vertSpeed = getConfigDataFloat("vert-speed", 0F);
		vertHitRadius = getConfigDataFloat("vertical-hit-radius", 1.5F);
		rotationOffset = getConfigDataFloat("rotation-offset", 0F);

		checkPlugins = getConfigBoolean("check-plugins", true);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		projectileHasGravity = getConfigBoolean("gravity", true);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");

		itemName = Util.getMiniMessage(getConfigString("item-name", null));
		spellOnTickName = getConfigString("spell-on-tick", "");
		spellOnDelayName = getConfigString("spell-on-delay", "");
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnTick = new Subspell(spellOnTickName);
		if (!spellOnTick.process()) {
			if (!spellOnTickName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-tick defined!");
			spellOnTick = null;
		}

		spellOnDelay = new Subspell(spellOnDelayName);
		if (!spellOnDelay.process()) {
			if (!spellOnDelayName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-delay defined!");
			spellOnDelay = null;
		}

		spellOnHitEntity = new Subspell(spellOnHitEntityName);
		if (!spellOnHitEntity.process()) {
			if (!spellOnHitEntityName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			spellOnHitEntity = null;
		}

		spellOnHitGround = new Subspell(spellOnHitGroundName);
		if (!spellOnHitGround.process()) {
			if (!spellOnHitGroundName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			spellOnHitGround = null;
		}

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public void turnOff() {
		for (ItemProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			ItemProjectileTracker tracker = new ItemProjectileTracker(data);

			setupTracker(tracker, data);
			tracker.start();
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		ItemProjectileTracker tracker = new ItemProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.start();
		return true;
	}

	private void setupTracker(ItemProjectileTracker tracker, SpellData data) {
		tracker.setSpell(this);

		tracker.setItemName(itemName);
		tracker.setItem(MagicItems.getMagicItemFromString(magicItemName.get(data), data).getItemStack());

		tracker.setSpellDelay(spellDelay.get(data));
		tracker.setPickupDelay(pickupDelay.get(data));
		tracker.setRemoveDelay(removeDelay.get(data));
		tracker.setTickInterval(tickInterval.get(data));
		tracker.setSpellInterval(spellInterval.get(data));
		tracker.setItemNameDelay(itemNameDelay.get(data));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(data));
		tracker.setentityHitDelay(entityHitDelay.get(data));

		tracker.setSpeed(speed.get(data));

		float yOffset = this.yOffset.get(data);
		tracker.setYOffset(yOffset);

		float vertSpeed = this.vertSpeed.get(data);
		tracker.setVertSpeed(vertSpeed);

		tracker.setHitRadius(hitRadius.get(data));
		tracker.setVertHitRadius(vertHitRadius.get(data));
		tracker.setRotationOffset(rotationOffset.get(data));

		tracker.setCallEvents(checkPlugins);
		tracker.setVertSpeedUsed(vertSpeed != 0);
		tracker.setStopOnHitGround(stopOnHitGround);
		tracker.setStopOnHitEntity(stopOnHitEntity);
		tracker.setProjectileHasGravity(projectileHasGravity);

		Vector relativeOffset = yOffset != 0 ? this.relativeOffset.clone().setY(yOffset) : this.relativeOffset;
		tracker.setRelativeOffset(relativeOffset);

		tracker.setSpellOnTick(spellOnTick);
		tracker.setSpellOnDelay(spellOnDelay);
		tracker.setSpellOnHitGround(spellOnHitGround);
		tracker.setSpellOnHitEntity(spellOnHitEntity);

		tracker.setTargetList(validTargetList);
	}

	public static Set<ItemProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public void playEffects(EffectPosition position, Location loc, SpellData data) {
		playSpellEffects(position, loc, data);
	}

	public void playEffects(EffectPosition position, Entity entity, SpellData data) {
		playSpellEffects(position, entity, data);
	}

}
