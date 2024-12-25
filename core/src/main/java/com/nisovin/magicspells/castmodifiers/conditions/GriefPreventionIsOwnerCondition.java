package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;
import com.nisovin.magicspells.util.SpellData;

@DependsOn(plugin = "GriefPrevention")
public class GriefPreventionIsOwnerCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return checkClaim(data.caster(), data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return checkClaim(data.target(), data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean checkClaim(LivingEntity target, Location location) {
		if (target == null) return false;
		Claim currentClaim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
		if (currentClaim == null) return false;
		return (target.getUniqueId().equals(currentClaim.ownerID));
	}

}
