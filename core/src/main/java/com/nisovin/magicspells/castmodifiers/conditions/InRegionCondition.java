package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;
import com.nisovin.magicspells.util.SpellData;

@DependsOn(plugin = "WorldGuard")
public class InRegionCondition extends Condition {

	private String worldName;
	private String regionName;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		String[] split = var.split(":");
		if (split.length == 2) {
			worldName = split[0];
			regionName = split[1];
			return true;
		}
		return false;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return checkRegion(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return checkRegion(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return checkRegion(data.location());
	}

	private boolean checkRegion(Location location) {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return false;
		if (world != location.getWorld()) return false;

		RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) return false;

		ProtectedRegion region = regionManager.getRegion(regionName);
		return region != null && region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

}
