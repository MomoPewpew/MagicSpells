package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.block.Biome;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class BiomeCondition extends Condition {
	
	private final EnumSet<Biome> biomes = EnumSet.noneOf(Biome.class);

	@Override
	public boolean initialize(String var) {
		String[] s = var.split(",");

		for (String value : s) {
			Biome biome = Util.enumValueSafe(Biome.class, value.toUpperCase());
			if (biome == null) {
				DebugHandler.debugBadEnumValue(Biome.class, value.toUpperCase());
				continue;
			}
			biomes.add(biome);
		}
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return biome(data.caster().getLocation());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return biome(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return biome(data.location());
	}

	private boolean biome(Location location) {
		return biomes.contains(location.getBlock().getBiome());
	}
	
}
