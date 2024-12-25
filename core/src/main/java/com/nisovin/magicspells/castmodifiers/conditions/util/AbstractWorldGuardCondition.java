package com.nisovin.magicspells.castmodifiers.conditions.util;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public abstract class AbstractWorldGuardCondition extends Condition {

	@Override
	public boolean checkCaster(SpellData data) {
		return check(data.caster(), data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return check(data.target(), data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return check(data.caster(), data.location());
	}

	public boolean check(LivingEntity livingEntity, Location location) {
		if (!(livingEntity instanceof Player player)) return false;
		ProtectedRegion region = getTopPriorityRegion(location);
		if (region == null) return false;
		LocalPlayer localPlayer = new BukkitPlayer(WorldGuardPlugin.inst(), player);
		return check(region, localPlayer);
	}

	protected abstract boolean check(ProtectedRegion region, LocalPlayer player);

	protected RegionManager getRegionManager(World world) {
		return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
	}

	protected ApplicableRegionSet getRegions(Location loc) {
		RegionManager manager = getRegionManager(loc.getWorld());
		if (manager == null) return null;
		return manager.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
	}

	protected ProtectedRegion getTopPriorityRegion(Location loc) {
		ApplicableRegionSet regions = getRegions(loc);

		ProtectedRegion topRegion = null;
		int topPriority = Integer.MIN_VALUE;
		for (ProtectedRegion region: regions) {
			if (region.getPriority() > topPriority) {
				topRegion = region;
				topPriority = region.getPriority();
			}
		}
		return topRegion;
	}

}
