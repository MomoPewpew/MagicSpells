package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.SpellData;

public class RotateSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private ConfigData<Integer> rotationYaw;
	private ConfigData<Integer> rotationPitch;

	private boolean random;
	private boolean affectPitch;
	private boolean mimicDirection;

	private String face;

	public RotateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		rotationYaw = getConfigDataInt("rotation-yaw", 10);
		rotationPitch = getConfigDataInt("rotation-pitch", 0);

		random = getConfigBoolean("random", false);
		affectPitch = getConfigBoolean("affect-pitch", false);
		mimicDirection = getConfigBoolean("mimic-direction", false);

		face = getConfigString("face", "");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data, info);

			data = data.builder().power(info.getPower()).build();
			spinFace(data);
			playSpellEffects(data.caster(), info.target(), data);
			sendMessages(data.caster(), info.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		spinFace(data);
		playSpellEffects(data.caster(), data.target(), data);
		return true;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		spin(data.caster(), data.location());
		playSpellEffects(data.caster(), data.location(), data);
		return true;
	}

	private void spinTarget(SpellData data) {
		Location loc = data.target().getLocation();
		if (random) {
			loc.setYaw(Util.getRandomInt(360));
			if (affectPitch) loc.setPitch(Util.getRandomInt(181) - 90);
		} else {
			loc.setYaw(loc.getYaw() + rotationYaw.get(data));
			if (affectPitch) loc.setPitch(loc.getPitch() + rotationPitch.get(data));
		}
		data.target().teleportAsync(loc);
	}

	private void spinFace(SpellData data) {
		Location targetLoc = data.target().getLocation();
		Location casterLoc = data.caster().getLocation();

		if (face.isEmpty()) {
			spinTarget(data);
			return;
		}

		Location loc;
		switch (face) {
			case "target" -> data.caster().teleportAsync(changeDirection(casterLoc, targetLoc));
			case "caster" -> data.target().teleportAsync(changeDirection(targetLoc, casterLoc));
			case "away-from-caster" -> {
				loc = changeDirection(targetLoc, casterLoc);
					loc.setYaw(loc.getYaw() + 180);
				data.target().teleportAsync(loc);
			}
			case "away-from-target" -> {
				loc = changeDirection(casterLoc, targetLoc);
				loc.setYaw(loc.getYaw() + 180);
				data.caster().teleportAsync(loc);
			}
		}

	}

	private void spin(LivingEntity entity, Location target) {
		entity.teleportAsync(changeDirection(entity.getLocation(), target));
	}

	private Location changeDirection(Location pos1, Location pos2) {
		Location loc = pos1.clone();
		if (mimicDirection) {
			if (affectPitch) loc.setPitch(pos2.getPitch());
			loc.setYaw(pos2.getYaw());
		} else loc.setDirection(getVectorDir(pos1, pos2));

		return loc;
	}

	private Vector getVectorDir(Location caster, Location target) {
		return target.clone().subtract(caster.toVector()).toVector();
	}

}
