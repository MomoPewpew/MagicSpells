package com.nisovin.magicspells.util;

import com.Zrips.CMI.commands.list.item;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public record SpellData(@Nullable LivingEntity caster, @Nullable LivingEntity target, @Nullable Location location, @NotNull float power, @NotNull String[] args, @Nullable ItemStack castItem) {

	public static final SpellData NULL = new SpellData(null, null, null, 1f, null, null);

	public SpellData(@Nullable LivingEntity caster, @NotNull float power, @NotNull String[] args, @NotNull ItemStack castItem) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), power, args, castItem);
	}

	public SpellData(@Nullable LivingEntity caster, @NotNull LivingEntity target, @NotNull float power, @NotNull String[] args) {
		this(caster, target, Objects.requireNonNull(target).getLocation(), power, args, null);
	}

	public SpellData(@Nullable LivingEntity caster, @NotNull Location location, @NotNull float power, @NotNull String[] args) {
		this(caster, null, location, power, args, null);
	}

	public SpellData(@Nullable LivingEntity caster, LivingEntity target, Location location, float power) {
		this(caster, target, location, power, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster, @NotNull Location location, @NotNull float power) {
		this(caster, null, location, power, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster, @NotNull float power, @NotNull String[] args) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), power, args, null);
	}

	public SpellData(@Nullable LivingEntity caster, @NotNull String[] args, @Nullable ItemStack castItem) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), 1f, args, castItem);
	}

	public SpellData(@Nullable LivingEntity caster, LivingEntity target, float power) {
		this(caster, target, Objects.requireNonNull(target).getLocation(), power, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster, LivingEntity target, Location location) {
		this(caster, target, location, 1f, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster, @Nullable ItemStack castItem) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), 1f, new String[0], castItem);
	}

	public SpellData(@Nullable LivingEntity caster, @NotNull String[] args) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), 1f, args, null);
	}

	public SpellData(@Nullable LivingEntity caster, Location location) {
		this(caster, null, location, 1f, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster, LivingEntity target) {
		this(caster, target, Objects.requireNonNull(target).getLocation(), 1f, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster, float power) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), power, new String[0], null);
	}

	public SpellData(@Nullable LivingEntity caster) {
		this(caster, null, Objects.requireNonNull(caster).getLocation(), 1f, new String[0], null);
	}

	public Builder builder() {
		return new Builder(this);
	}

	public SpellData invert() {
		@NotNull LivingEntity caster = this.caster;
		LivingEntity target = this.target;

		if (this.caster != null) target = this.caster;
		if (this.target != null) caster = this.target;

		return Objects.equals(caster, this.caster) && Objects.equals(target, this.target) ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public SpellData caster(LivingEntity caster) {
		return Objects.equals(this.caster, caster) ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public SpellData target(LivingEntity target) {
		return Objects.equals(this.target, target) ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public SpellData location(Location location) {
		return Objects.equals(this.target, target) ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public SpellData power(@NotNull float power) {
		return this.power == power ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public SpellData args(@NotNull String[] args) {
		return Arrays.equals(this.args, args) ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public SpellData castItem(ItemStack castItem) {
		return this.power == power ? this : new SpellData(caster, target, location, power, args, castItem);
	}

	public static class Builder {

		private @NotNull LivingEntity caster;
		private LivingEntity target;
		private Location location;
		private @NotNull float power;
		private @NotNull String[] args;
		private ItemStack castItem;

		public Builder() {
			this(SpellData.NULL);
		}

		public Builder(SpellData data) {
			caster = data.caster;
			target = data.target;
			location = data.location;
			power = data.power;
			args = data.args;
			castItem = data.castItem;
		}

		public Builder caster(@NotNull LivingEntity caster) {
			this.caster = caster;
			return this;
		}

		public Builder target(LivingEntity target) {
			this.target = target;
			return this;
		}

		public Builder location(Location location) {
			this.location = location;
			return this;
		}

		public Builder power(@NotNull float power) {
			this.power = power;
			return this;
		}

		public Builder args(@NotNull String[] args) {
			this.args = args;
			return this;
		}

		public Builder castItem(ItemStack castItem) {
			this.castItem = castItem;
			return this;
		}

		public SpellData build() {
			return new SpellData(caster, target, location, power, args, castItem);
		}

	}
}
