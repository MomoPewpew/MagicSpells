package com.nisovin.magicspells.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;

public record SpellData(LivingEntity caster, LivingEntity target, Location location, float power, String[] args) {

	public SpellData(LivingEntity caster, LivingEntity target, float power, String[] args) {
		this(caster, target, null, power, args);
	}

	public SpellData(LivingEntity caster, float power, String[] args) {
		this(caster, null, power, args);
	}

	public SpellData(LivingEntity caster) {
		this(caster, null, 1f, null);
	}

}
