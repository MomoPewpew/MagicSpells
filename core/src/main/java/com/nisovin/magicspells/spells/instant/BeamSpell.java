package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

public class BeamSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private final ConfigData<Double> yOffset;
	private final ConfigData<Double> hitRadius;
	private final ConfigData<Double> maxDistance;
	private final ConfigData<Double> verticalHitRadius;
	private final ConfigData<Double> verticalRotation;
	private final ConfigData<Double> horizontalRotation;

	private final ConfigData<Float> gravity;
	private final ConfigData<Float> interval;
	private final ConfigData<Float> rotation;
	private final ConfigData<Float> beamVertOffset;
	private final ConfigData<Float> beamHorizOffset;

	private final ConfigData<Float> beamVerticalSpread;
	private final ConfigData<Float> beamHorizontalSpread;

	private boolean changePitch;
	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;

	private Subspell hitSpell;
	private Subspell endSpell;
	private Subspell travelSpell;
	private Subspell groundSpell;
	private Subspell entityLocationSpell;

	private final String hitSpellName;
	private final String endSpellName;
	private final String travelSpellName;
	private final String groundSpellName;
	private final String entityLocationSpellName;

	private NoMagicZoneManager zoneManager;

	private static final double ANGLE_Y = AccurateMath.toRadians(-90);

	public BeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

		yOffset = getConfigDataDouble("y-offset", 0F);
		hitRadius = getConfigDataDouble("hit-radius", 2);
		maxDistance = getConfigDataDouble("max-distance", 30);
		verticalHitRadius = getConfigDataDouble("vertical-hit-radius", 2);

		verticalRotation = getConfigDataDouble("vertical-rotation", 0D);
		horizontalRotation = getConfigDataDouble("horizontal-rotation", 0D);

		gravity = getConfigDataFloat("gravity", 0F);
		interval = getConfigDataFloat("interval", 1F);
		rotation = getConfigDataFloat("rotation", 0F);
		beamVertOffset = getConfigDataFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigDataFloat("beam-horiz-offset", 0F);

		ConfigData<Float> beamSpread = getConfigDataFloat("beam-spread", 0F);
		beamVerticalSpread = getConfigDataFloat("beam-vertical-spread", beamSpread);
		beamHorizontalSpread = getConfigDataFloat("beam-horizontal-spread", beamSpread);

		changePitch = getConfigBoolean("change-pitch", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		hitSpellName = getConfigString("spell", "");
		endSpellName = getConfigString("spell-on-end", "");
		travelSpellName = getConfigString("spell-on-travel", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			if (!hitSpellName.isEmpty())
				MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell defined!");

			hitSpell = null;
		}

		endSpell = new Subspell(endSpellName);
		if (!endSpell.process()) {
			if (!endSpellName.isEmpty())
				MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-end defined!");

			endSpell = null;
		}

		travelSpell = new Subspell(travelSpellName);
		if (!travelSpell.process()) {
			if (!travelSpellName.isEmpty())
				MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-travel defined!");

			travelSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process()) {
			if (!groundSpellName.isEmpty())
				MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");

			groundSpell = null;
		}

		entityLocationSpell = new Subspell(entityLocationSpellName);
		if (!entityLocationSpell.process()) {
			if (!entityLocationSpellName.isEmpty())
				MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-entity-location defined!");

			entityLocationSpell = null;
		}

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) shootBeam(data);
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.caster())) return false;
		shootBeam(data);
		return true;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		shootBeam(data);
		return true;
	}

	private void shootBeam(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location from = data.location();
		float power = data.power();
		String[] args = data.args();

		playSpellEffects(EffectPosition.CASTER, caster, data);

		Location loc = from.clone();
		if (!changePitch) loc.setPitch(0);

		float beamVertOffset = this.beamVertOffset.get(data);
		if (beamVertOffset != 0) loc.setPitch(loc.getPitch() - beamVertOffset);

		float beamHorizOffset = this.beamHorizOffset.get(data);
		if (beamHorizOffset != 0) loc.setYaw(loc.getYaw() + beamHorizOffset);

		Vector startDir;
		if (target == null) startDir = loc.getDirection();
		else startDir = target.getLocation().toVector().subtract(loc.toVector()).normalize();

		//apply relative offset
		Vector relativeOffset;

		double yOffset = this.yOffset.get(data);
		if (yOffset != 0) relativeOffset = this.relativeOffset.clone().setY(yOffset);
		else relativeOffset = this.relativeOffset;

		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
		loc.add(horizOffset.multiply(relativeOffset.getZ()));
		loc.add(loc.getDirection().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());

		float interval = this.interval.get(data);
		if (interval < 0.01) interval = 0.01f;

		Vector dir;
		if (target == null) dir = loc.getDirection().multiply(interval);
		else {
			//apply target relative offset
			Location targetLoc = target.getLocation();
			Vector targetDir = targetLoc.getDirection();

			Vector targetHorizOffset = new Vector(-targetDir.getZ(), 0, targetDir.getX()).normalize();
			targetLoc.add(targetHorizOffset.multiply(targetRelativeOffset.getZ()));
			targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
			targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());

			dir = targetLoc.toVector().subtract(loc.toVector()).normalize().multiply(interval);
		}

		Vector dirNormalized = dir.clone().normalize();

		Vector angleZ = Util.makeFinite(new Vector(-dirNormalized.getZ(), 0D, dirNormalized.getX()).normalize());
		Vector angleY = Util.makeFinite(dirNormalized.rotateAroundAxis(angleZ, ANGLE_Y).normalize());

		double verticalRotation = this.verticalRotation.get(data);
		double horizontalRotation = this.horizontalRotation.get(data);

		if (verticalRotation != 0) dir.rotateAroundAxis(angleZ, AccurateMath.toRadians(verticalRotation));
		if (horizontalRotation != 0) dir.rotateAroundAxis(angleY, AccurateMath.toRadians(horizontalRotation));

		float beamVerticalSpread = this.beamVerticalSpread.get(data);
		float beamHorizontalSpread = this.beamHorizontalSpread.get(data);
		if (beamVerticalSpread > 0 || beamHorizontalSpread > 0) {
			float rx = -1 + random.nextFloat() * 2;
			float ry = -1 + random.nextFloat() * 2;
			float rz = -1 + random.nextFloat() * 2;
			dir.add(new Vector(rx * beamHorizontalSpread, ry * beamVerticalSpread, rz * beamHorizontalSpread));
		}

		double verticalHitRadius = this.verticalHitRadius.get(data);
		double maxDistance = this.maxDistance.get(data);
		double hitRadius = this.hitRadius.get(data);

		float rotation = this.rotation.get(data);
		float gravity = -this.gravity.get(data);

		Set<Entity> immune = new HashSet<>();
		float d = 0;

		mainLoop:
		while (d < maxDistance) {
			d += interval;
			loc.add(dir);

			if (rotation != 0) Util.rotateVector(dir, rotation);
			if (gravity != 0) dir.add(new Vector(0, gravity, 0));
			if (rotation != 0 || gravity != 0) loc.setDirection(dir);

			loc = Util.makeFinite(loc);

			if (zoneManager.willFizzle(loc, this)) break;

			//check block collision
			if (!isTransparent(loc.getBlock())) {
				playSpellEffects(EffectPosition.DISABLED, loc, data);
				if (groundSpell != null) groundSpell.subcast(data);
				if (stopOnHitGround) break;
			}

			playSpellEffects(EffectPosition.SPECIAL, loc, data);

			if (travelSpell != null) travelSpell.subcast(data);

			//check entities in the beam range
			for (LivingEntity e : loc.getNearbyLivingEntities(hitRadius, verticalHitRadius)) {
				if (e == caster || !e.isValid() || immune.contains(e)) continue;
				if (validTargetList != null && !validTargetList.canTarget(e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(this, data);
				if (!event.callEvent()) continue;

				LivingEntity entity = event.getTarget();

				if (hitSpell != null) hitSpell.subcast(data);
				if (entityLocationSpell != null) entityLocationSpell.subcast(data);

				playSpellEffects(EffectPosition.TARGET, entity, data);
				playSpellEffectsTrail(caster.getLocation(), entity.getLocation(), data);
				immune.add(e);

				if (stopOnHitEntity) break mainLoop;
			}
		}

		//end of the beam
		if (!zoneManager.willFizzle(loc, this) && d >= maxDistance) {
			playSpellEffects(EffectPosition.DELAYED, loc, data);
			if (endSpell != null) endSpell.subcast(data);
		}
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		return false;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

}
