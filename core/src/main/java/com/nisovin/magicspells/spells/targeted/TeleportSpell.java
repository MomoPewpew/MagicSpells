package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class TeleportSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Float> yaw;
	private ConfigData<Float> pitch;

	private Vector relativeOffset;

	private String strCantTeleport;

	public TeleportSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yaw = getConfigDataFloat("yaw", 0);
		pitch = getConfigDataFloat("pitch", 0);

		relativeOffset = getConfigVector("relative-offset", "0,0.1,0");

		strCantTeleport = getConfigString("str-cant-teleport", "");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			data = data.builder().power(target.getPower()).build();
			if (!teleport(data)) return noTarget(data, strCantTeleport);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return teleport(data);
	}

	private boolean teleport(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location targetLoc = target.getLocation();
		Location startLoc = caster.getLocation();

		Vector startDir = startLoc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		targetLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		targetLoc.add(startLoc.getDirection().multiply(relativeOffset.getX()));
		targetLoc.setY(targetLoc.getY() + relativeOffset.getY());

		targetLoc.setPitch(startLoc.getPitch() - pitch.get(data));
		targetLoc.setYaw(startLoc.getYaw() + yaw.get(data));

		if (!BlockUtils.isPathable(targetLoc.getBlock())) return false;

		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		
		caster.teleportAsync(targetLoc);
		return true;
	}

}
