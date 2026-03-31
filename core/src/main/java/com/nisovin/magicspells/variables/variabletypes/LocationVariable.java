package com.nisovin.magicspells.variables.variabletypes;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.BlockLocation;
import com.nisovin.magicspells.util.Region;

public class LocationVariable extends Variable {

	private final Map<BlockLocation, Region<Double>> blockMap = new HashMap<>();
	private final Set<Region<Double>> regions = new HashSet<>();

	@Override
	public void set(String player, double amount) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			set(pl.getLocation(), amount);
		}
	}

	@Override
	public double getValue(String player) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			return getValue(pl.getLocation());
		}
		return defaultValue;
	}

	@Override
	public void reset(String player) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			reset(pl.getLocation());
		}
	}

	public double getValue(Location location) {
		if (location == null) return defaultValue;
		BlockLocation block = new BlockLocation(location);
		Region<Double> region = blockMap.get(block);
		return region != null ? region.getValue() : defaultValue;
	}

	public void set(Location location, double amount) {
		if (location == null) return;
		BlockLocation block = new BlockLocation(location);
		Region<Double> region = blockMap.get(block);
		if (region != null) {
			double min = getMinValue(null);
			double max = getMaxValue(null);
			if (amount > max) amount = max;
			else if (amount < min) amount = min;
			region.setValue(amount);
		}
	}

	public void reset(Location location) {
		if (location == null) return;
		set(location, defaultValue);
	}

	public void defineRegion(Set<BlockLocation> blocks, double value) {
		Region<Double> newRegion = new Region<>(value);
		for (BlockLocation block : blocks) {
			Region<Double> oldRegion = blockMap.get(block);
			if (oldRegion != null) {
				oldRegion.removeBlock(block);
				if (oldRegion.isEmpty()) {
					regions.remove(oldRegion);
				}
			}
			newRegion.addBlock(block);
			blockMap.put(block, newRegion);
		}
		if (!newRegion.isEmpty()) {
			regions.add(newRegion);
		}
	}

	public Set<Region<Double>> getRegions() {
		return Collections.unmodifiableSet(regions);
	}

	public Map<BlockLocation, Region<Double>> getBlockMap() {
		return Collections.unmodifiableMap(blockMap);
	}

}
