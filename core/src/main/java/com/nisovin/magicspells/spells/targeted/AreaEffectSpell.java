package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
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

import org.apache.commons.math4.core.jdkmath.AccurateMath;

public class AreaEffectSpell extends TargetedSpell implements TargetedLocationSpell {

	private List<Subspell> spells;
	private List<String> spellNames;

	private ConfigData<Integer> maxTargets;

	private ConfigData<Double> cone;
	private ConfigData<Double> horizontalCone;
	private ConfigData<Double> vRadius;
	private ConfigData<Double> hRadius;

	private boolean pointBlank;
	private boolean circleShape;
	private boolean useProximity;
	private boolean passTargeting;
	private boolean failIfNoTargets;
	private boolean reverseProximity;
	private boolean spellSourceInCenter;

	public AreaEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellNames = getConfigStringList("spells", null);

		maxTargets = getConfigDataInt("max-targets", 0);

		cone = getConfigDataDouble("cone", 0);
		horizontalCone = getConfigDataDouble("horizontal-cone", 0);

		vRadius = getConfigDataDouble("vertical-radius", 5);
		hRadius = getConfigDataDouble("horizontal-radius", 10);

		pointBlank = getConfigBoolean("point-blank", true);
		circleShape = getConfigBoolean("circle-shape", false);
		useProximity = getConfigBoolean("use-proximity", false);
		passTargeting = getConfigBoolean("pass-targeting", true);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		reverseProximity = getConfigBoolean("reverse-proximity", false);
		spellSourceInCenter = getConfigBoolean("spell-source-in-center", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		spells = new ArrayList<>();

		if (spellNames == null || spellNames.isEmpty()) {
			MagicSpells.error("AreaEffectSpell '" + internalName + "' has no spells defined!");
			return;
		}

		for (String spellName : spellNames) {
			Subspell spell = new Subspell(spellName);

			if (!spell.process()) {
				MagicSpells.error("AreaEffectSpell '" + internalName + "' attempted to use invalid spell '" + spellName + '\'');
				continue;
			}

			spells.add(spell);
		}

		spellNames.clear();
		spellNames = null;
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			if (pointBlank) {
				data = data.builder().location(data.caster().getLocation()).build();
            }
			else {
				try {
					Block block = getTargetedBlock(data);
					if (block != null && !BlockUtils.isAir(block.getType())) data = data.builder().location(block.getLocation().add(0.5, 0, 0.5)).build();
				}
				catch (IllegalStateException ignored) {}
			}

			if (data.location() == null) return noTarget(data);

			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data);
			EventUtil.call(event);
			if (event.isCancelled()) data = data.builder().location(null).build();
			else {
				data = data.builder().location(event.getTargetLocation()).power(event.getPower()).build();
			}

			if (data.location() == null) return noTarget(data);

			boolean done = doAoe(data);
			if (!done) return noTarget(data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		return doAoe(data);
	}

	private boolean doAoe(SpellData data) {
		int count = 0;

		LivingEntity caster = data.caster();
		Location location = Util.makeFinite(data.location().clone());
		float basePower = data.power();

		int maxTargets = this.maxTargets.get(data);

		double cone = this.cone.get(data);
		double horizontalCone = this.horizontalCone.get(data);

		double vRadius = Math.min(this.vRadius.get(data), MagicSpells.getGlobalRadius());
		double hRadius = Math.min(this.hRadius.get(data), MagicSpells.getGlobalRadius());

		double vRadiusSquared = vRadius * vRadius;
		double hRadiusSquared = hRadius * hRadius;

		SpellTargetEvent event;

		double hDistance;
		double vDistance;

		if (validTargetList.canTargetOnlyCaster()) {
			if (caster == null) return false;

			LivingEntity target = caster;
			data = data.builder().power(basePower).build();

			if (!target.getWorld().equals(location.getWorld())) return false;

			hDistance = NumberConversions.square(target.getLocation().getX() - location.getX()) + NumberConversions.square(target.getLocation().getZ() - location.getZ());
			if (hDistance > hRadiusSquared) return false;
			vDistance = NumberConversions.square(target.getLocation().getY() - location.getY());
			if (vDistance > vRadiusSquared) return false;

			event = new SpellTargetEvent(this, data);
			EventUtil.call(event);
			if (event.isCancelled()) return false;

			target = event.getTarget();
			data = data.builder().power(event.getPower()).build();

			castSpells(data);

			playSpellEffects(EffectPosition.TARGET, target, data);
			playSpellEffects(EffectPosition.SPECIAL, location, data);
			if (spellSourceInCenter) playSpellEffects(caster, location, target, data);
			else playSpellEffectsTrail(caster.getLocation(), target.getLocation(), data);

			return true;
		}

		List<LivingEntity> entities = new ArrayList<>(location.getWorld().getNearbyLivingEntities(location, hRadius, vRadius, hRadius));

		if (useProximity) {
			// check world before distance
			for (LivingEntity entity : new ArrayList<>(entities)) {
				if (entity.getWorld().equals(location.getWorld())) continue;
				entities.remove(entity);
			}
			Location finalLocation = location;
			Comparator<LivingEntity> comparator = Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(finalLocation));
			if (reverseProximity) comparator = comparator.reversed();
			entities.sort(comparator);
		}

		for (LivingEntity target : entities) {
			if (target.isDead()) continue;
			if (!validTargetList.canTarget(caster, target)) continue;

			if (circleShape) {
				hDistance = NumberConversions.square(target.getLocation().getX() - location.getX()) + NumberConversions.square(target.getLocation().getZ() - location.getZ());
				if (hDistance > hRadiusSquared) continue;
				vDistance = NumberConversions.square(target.getLocation().getY() - location.getY());
				if (vDistance > vRadiusSquared) continue;
			}

			if (horizontalCone > 0 && horizontalAngle(location, target.getLocation()) > horizontalCone) continue;

			if (cone > 0) {
				Vector dir = target.getLocation().toVector().subtract(location.toVector());
				if (AccurateMath.toDegrees(AccurateMath.abs(dir.angle(location.getDirection()))) > cone) continue;
			}

			data = data.builder().power(basePower).build();

			event = new SpellTargetEvent(this, data, target);
			EventUtil.call(event);
			if (event.isCancelled()) continue;

			target = event.getTarget();
			data = data.builder().power(event.getPower()).build();

			castSpells(data);

			playSpellEffects(EffectPosition.TARGET, target, data);

			if (spellSourceInCenter) playSpellEffects(caster, location, target, data);
			else if (caster != null) playSpellEffectsTrail(caster.getLocation(), target.getLocation(), data);

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;
		}

		boolean success = count > 0 || !failIfNoTargets;
		if (success) {
			playSpellEffects(EffectPosition.SPECIAL, location, data);
			if (caster != null) playSpellEffects(EffectPosition.CASTER, caster, data);
		}

		return success;
	}

	private void castSpells(SpellData data) {
		for (Subspell spell : spells) {
			spell.subcast(data, passTargeting);
		}
	}

	private double horizontalAngle(Location from, Location to) {
		Location startLoc = from.clone();
		Location endLoc = to.clone();

		startLoc.setY(0.0D);
		startLoc.setPitch(0.0F);

		endLoc.setY(0.0D);
		endLoc.setPitch(0.0F);

		Vector direction = endLoc.toVector().subtract(startLoc.toVector()).normalize();

		return AccurateMath.toDegrees(direction.angle(startLoc.getDirection()));
	}

}
