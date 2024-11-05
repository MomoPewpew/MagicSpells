package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class GripSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private ConfigData<Double> yOffset;
	private ConfigData<Double> locationOffset;

	private boolean checkGround;

	private Vector relativeOffset;

	private String strCantGrip;

	public GripSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigDataDouble("y-offset", 0);
		locationOffset = getConfigDataDouble("location-offset", 0);

		checkGround = getConfigBoolean("check-ground", true);

		relativeOffset = getConfigVector("relative-offset", "1,1,0");

		strCantGrip = getConfigString("str-cant-grip", "");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			if (!grip(data.builder().target(target.target()).location(data.caster().getLocation()).build())) return noTarget(data, strCantGrip);
			sendMessages(data.caster(), target.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return grip(data.builder().location(data.caster().getLocation()).build());
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return grip(data);
	}

	private boolean grip(SpellData data) {

		Location loc = data.location().clone();

		Vector startDir = loc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();

		Vector relativeOffset = this.relativeOffset.clone();

		double yOffset = this.yOffset.get(data);
		if (yOffset != 0) relativeOffset.setY(yOffset);

		double locationOffset = this.locationOffset.get(data);
		if (locationOffset != 0) relativeOffset.setX(locationOffset);

		loc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		loc.add(loc.getDirection().clone().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());

		if (checkGround && !BlockUtils.isPathable(loc.getBlock())) return false;

		playSpellEffects(EffectPosition.TARGET, data.target(), data);
		playSpellEffectsTrail(data.location(), loc, data);

		data.target().teleportAsync(loc);
		return true;
	}

}
