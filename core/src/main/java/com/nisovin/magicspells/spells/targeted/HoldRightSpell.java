package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class HoldRightSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private ConfigData<Integer> resetTime;

	private ConfigData<Float> maxDuration;
	private ConfigData<Float> maxDistance;

	private boolean targetEntity;
	private boolean targetLocation;

	private Subspell spellToCast;
	private String spellToCastName;

	private Map<UUID, CastData> casting;

	public HoldRightSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		resetTime = getConfigDataInt("reset-time", 250);

		maxDuration = getConfigDataFloat("max-duration", 0F);
		maxDistance = getConfigDataFloat("max-distance", 0F);

		targetEntity = getConfigBoolean("target-entity", true);
		targetLocation = getConfigBoolean("target-location", false);

		spellToCastName = getConfigString("spell", "");

		casting = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			spellToCast = null;
			MagicSpells.error("HoldRightSpell '" + internalName + "' has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity caster = data.caster();
			CastData cdata = casting.get(caster.getUniqueId());
			if (cdata != null && cdata.isValid(caster)) {
				cdata.cast();
				return PostCastAction.ALREADY_HANDLED;
			}

			if (targetEntity) {
				TargetInfo<LivingEntity> target = getTargetedEntity(data);
				if (target.noTarget()) return noTarget(data, target);

				cdata = new CastData(data.builder().target(target.target()).power(target.getPower()).build());
			} else if (targetLocation) {
				Block block = getTargetedBlock(data);
				if (block == null || block.getType().isAir()) return noTarget(data);

				cdata = new CastData(data.builder().location(block.getLocation().add(0.5, 0.5, 0.5)).build());
			} else cdata = new CastData(data);

			cdata.cast();
			casting.put(caster.getUniqueId(), cdata);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		LivingEntity caster = data.caster();
		if (!targetLocation) return false;
		CastData cdata = casting.get(caster.getUniqueId());
		if (cdata != null && cdata.isValid(caster)) {
			cdata.cast();
			return true;
		}

		cdata = new CastData(data);
		cdata.cast();
		casting.put(caster.getUniqueId(), cdata);

		return true;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		LivingEntity caster = data.caster();
		if (!targetEntity || !validTargetList.canTarget(caster, data.target())) return false;
		CastData cdata = casting.get(caster.getUniqueId());
		if (cdata != null && cdata.isValid(caster)) {
			cdata.cast();
			return true;
		}

		cdata = new CastData(data);
		cdata.cast();
		casting.put(caster.getUniqueId(), cdata);

		return true;
	}

	private class CastData {

		private final SpellData data;

		private final float maxDistance;
		private final float maxDuration;
		private final int resetTime;

		private long start = System.currentTimeMillis();
		private long lastCast = 0;

		private CastData(SpellData data) {
			this.data = data;

			maxDistance = HoldRightSpell.this.maxDistance.get(data);
			maxDuration = HoldRightSpell.this.maxDuration.get(data);
			resetTime = HoldRightSpell.this.resetTime.get(data);
		}

		private boolean isValid(LivingEntity livingEntity) {
			if (lastCast < System.currentTimeMillis() - resetTime) return false;
			if (maxDuration > 0 && System.currentTimeMillis() - start > maxDuration * TimeUtil.MILLISECONDS_PER_SECOND)
				return false;
			if (maxDistance > 0) {
				Location l = data.location();
				if (data.target() != null) l = data.target().getLocation();
				if (l == null) return false;
				if (!l.getWorld().equals(livingEntity.getWorld())) return false;
				if (l.distanceSquared(livingEntity.getLocation()) > maxDistance * maxDistance) return false;
			}
			return true;
		}

		private void cast() {
			spellToCast.subcast(data);
		}

	}

}
