package com.nisovin.magicspells.handlers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectHandler {

	private static final Map<String, PotionEffectType> potionEffects = new HashMap<>();

	static {
		// Add bukkit and minecraft potion effect names
		for (PotionEffectType potionEffect : PotionEffectType.values()) {
			potionEffects.put(potionEffect.getName().toLowerCase(), potionEffect);
			potionEffects.put(potionEffect.getKey().value().toLowerCase(), potionEffect);
		}
	}

	public static PotionEffectType getPotionEffectType(String identification) {
		return potionEffects.get(identification.trim().toLowerCase());
	}

	public static PotionType getPotionType(String name) {
		try {
			return PotionType.valueOf(name.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			DebugHandler.debugIllegalArgumentException(e);
		}
		return null;
	}

}
