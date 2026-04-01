package com.nisovin.magicspells.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;

public class SpellData {

	private LivingEntity caster;
	private LivingEntity target;
	private Location location;
	private float power;
	private String[] args;

	public SpellData(LivingEntity caster, LivingEntity target, Location location, float power, String[] args) {
		this.caster = caster;
		this.target = target;
		this.location = location;
		this.power = power;
		this.args = args;
	}

	public SpellData(LivingEntity caster, LivingEntity target, Location location, float power) {
		this(caster, target, location, power, null);
	}

	public SpellData(LivingEntity caster, LivingEntity target, float power, String[] args) {
		this(caster, target, caster == null ? target == null ? null : target.getLocation() : caster.getLocation(),
				power, args);
	}

	public SpellData(LivingEntity caster, Location location, float power, String[] args) {
		this(caster, null, location, power, args);
	}

	public SpellData(LivingEntity caster, Location location, float power) {
		this(caster, null, location, power, null);
	}

	public SpellData(LivingEntity caster, float power, String[] args) {
		this(caster, caster == null ? null : caster.getLocation(), power, args);
	}

	public SpellData(LivingEntity caster, float power) {
		this(caster, power, null);
	}

	public SpellData(LivingEntity caster, Location location) {
		this(caster, location, 1f, null);
	}

	public SpellData(LivingEntity caster) {
		this(caster, 1f);
	}

	public LivingEntity caster() {
		return caster;
	}

	public void setCaster(LivingEntity caster) {
		this.caster = caster;
	}

	public LivingEntity target() {
		return target;
	}

	public void setTarget(LivingEntity target) {
		this.target = target;
	}

	public Location location() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public float power() {
		return power;
	}

	public void setPower(float power) {
		this.power = power;
	}

	public String[] args() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SpellData spellData = (SpellData) o;
		return Float.compare(spellData.power, power) == 0 &&
				java.util.Objects.equals(caster, spellData.caster) &&
				java.util.Objects.equals(target, spellData.target) &&
				java.util.Objects.equals(location, spellData.location) &&
				java.util.Arrays.equals(args, spellData.args);
	}

	@Override
	public int hashCode() {
		int result = java.util.Objects.hash(caster, target, location, power);
		result = 31 * result + java.util.Arrays.hashCode(args);
		return result;
	}

	@Override
	public String toString() {
		return "SpellData[" +
				"caster=" + caster +
				", target=" + target +
				", location=" + location +
				", power=" + power +
				", args=" + java.util.Arrays.toString(args) +
				']';
	}

}
