package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ShadowstepSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Float> yaw;
	private ConfigData<Float> pitch;

	private ConfigData<Double> distance;

	private Vector relativeOffset;

	private String strNoLandingSpot;

	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yaw = getConfigDataFloat("yaw", 0);
		pitch = getConfigDataFloat("pitch", 0);

		distance = getConfigDataDouble("distance", -1);

		relativeOffset = getConfigVector("relative-offset", "-1,0,0");

		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			data = data.builder().power(target.getPower()).build();
			boolean done = shadowstep(data);
			if (!done) return noTarget(data, strNoLandingSpot);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return shadowstep(data);
	}

	private boolean shadowstep(SpellData data) {
		Location targetLoc = data.target().getLocation().clone();
		targetLoc.setPitch(0);

		Vector startDir = targetLoc.getDirection().setY(0).normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();

		double distance = this.distance.get(data);
		Vector relativeOffset = distance != -1 ? this.relativeOffset.clone().setX(distance) : this.relativeOffset;

		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(targetLoc.getDirection().setY(0).multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(pitch.get(data));
		targetLoc.setYaw(targetLoc.getYaw() + yaw.get(data));

		Block b = targetLoc.getBlock();
		if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) return false;

		playSpellEffects(data.caster().getLocation(), targetLoc, data);
		data.caster().teleportAsync(targetLoc);

		return true;
	}

}
