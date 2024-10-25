package com.nisovin.magicspells.util;

import org.bukkit.entity.LivingEntity;

public record SpellData(LivingEntity caster, LivingEntity target, float power, String[] args, CastItem castItem) {

	public SpellData(LivingEntity caster, float power, String[] args, CastItem castItem) {
		this(caster, null, power, args, castItem);
	}

	public SpellData(LivingEntity caster, LivingEntity target, float power, String[] args) {
		this(caster, target, power, args, null);
	}

	public SpellData(LivingEntity caster, float power, String[] args) {
		this(caster, null, power, args, null);
	}

	public SpellData(LivingEntity caster) {
		this(caster, null, 1f, null, null);
	}

}
