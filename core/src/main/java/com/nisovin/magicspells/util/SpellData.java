package com.nisovin.magicspells.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;

public record SpellData(LivingEntity caster, LivingEntity target, Location location, float power, String[] args) {

	public SpellData(LivingEntity caster, LivingEntity target, float power, String[] args) {
		this(caster, target, caster == null ? target == null ? null : target.getLocation() : caster.getLocation(),
				power, args);
	}

	public SpellData(LivingEntity caster, Location location, float power, String[] args) {
		this(caster, null, location, power, args);
	}

	public SpellData(LivingEntity caster, float power, String[] args) {
		this(caster, caster == null ? null : caster.getLocation(), power, args);
	}

	public SpellData(LivingEntity caster, float power) {
		this(caster, power, null);
	}

	public SpellData(LivingEntity caster) {
		this(caster, 1f);
	}

}
