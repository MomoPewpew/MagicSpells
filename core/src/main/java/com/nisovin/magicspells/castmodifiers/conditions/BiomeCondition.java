package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.HashSet;
import java.util.Set;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class BiomeCondition extends Condition {

	private final Set<Biome> biomes = new HashSet<>();

	@Override
	public boolean initialize(String var) {
		if (var.isEmpty()) return false;

		Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

		for (String value : var.split(",")) {
			NamespacedKey key = NamespacedKey.fromString(value);
			if (key == null) return false;

			Biome biome = registry.get(key);
			if (biome == null) return false;

			biomes.add(biome);
		}

		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return biome(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return biome(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return biome(location);
	}

	private boolean biome(Location location) {
		return biomes.contains(location.getBlock().getBiome());
	}
	
}
