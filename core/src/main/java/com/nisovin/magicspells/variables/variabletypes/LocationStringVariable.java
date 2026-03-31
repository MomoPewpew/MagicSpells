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

public class LocationStringVariable extends Variable {

	private final Map<BlockLocation, Region<String>> blockMap = new HashMap<>();
	private final Set<Region<String>> regions = new HashSet<>();

	@Override
	public void set(String player, double amount) {
		set(player, amount + "");
	}

	public void set(String player, String value) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			set(pl.getLocation(), value);
		}
	}

	@Override
	public void parseAndSet(String player, String textValue) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			set(pl.getLocation(), textValue);
		}
	}

	@Override
	public double getValue(String player) {
		try {
			return Double.parseDouble(getStringValue(player));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public String getStringValue(String player) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			return getStringValue(pl.getLocation());
		}
		return defaultStringValue;
	}

	@Override
	public void reset(String player) {
		Player pl = Bukkit.getPlayerExact(player);
		if (pl != null) {
			reset(pl.getLocation());
		}
	}

	public String getStringValue(Location location) {
		if (location == null) return defaultStringValue;
		BlockLocation block = new BlockLocation(location);
		Region<String> region = blockMap.get(block);
		return region != null ? region.getValue() : defaultStringValue;
	}

	public void set(Location location, String value) {
		if (location == null) return;
		BlockLocation block = new BlockLocation(location);
		Region<String> region = blockMap.get(block);
		if (region != null) {
			region.setValue(value);
		}
	}

	public void reset(Location location) {
		if (location == null) return;
		set(location, defaultStringValue);
	}

	public void defineRegion(Set<BlockLocation> blocks, String value) {
		Region<String> newRegion = new Region<>(value);
		for (BlockLocation block : blocks) {
			Region<String> oldRegion = blockMap.get(block);
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

	public Set<Region<String>> getRegions() {
		return Collections.unmodifiableSet(regions);
	}

	public Map<BlockLocation, Region<String>> getBlockMap() {
		return Collections.unmodifiableMap(blockMap);
	}

}
