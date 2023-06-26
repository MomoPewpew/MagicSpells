package com.nisovin.magicspells.util.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

import org.bukkit.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.util.ParticleUtil;

public class ConfigDataUtil {

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			int value = config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> data = FunctionData.build(config.getString(path), Double::intValue);
			if (data == null) return (caster, target, power, args) -> null;

			return data;
		}

		return (caster, target, power, args) -> null;
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, int def) {
		if (config.isInt(path)) {
			int value = config.getInt(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> data = FunctionData.build(config.getString(path), Double::intValue, def);
			if (data == null) return (caster, target, power, args) -> def;

			return data;
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Integer> def) {
		if (config.isInt(path)) {
			int value = config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> data = FunctionData.build(config.getString(path), Double::intValue, def);
			if (data == null) return def;

			return data;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> data = FunctionData.build(config.getString(path), Double::longValue);
			if (data == null) return (caster, target, power, args) -> null;

			return data;
		}

		return (caster, target, power, args) -> null;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, long def) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> data = FunctionData.build(config.getString(path), Double::longValue, def);
			if (data == null) return (caster, target, power, args) -> def;

			return data;
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Long> def) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> data = FunctionData.build(config.getString(path), Double::longValue, def);
			if (data == null) return def;

			return data;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> data = FunctionData.build(config.getString(path), Double::shortValue);
			if (data == null) return (caster, target, power, args) -> null;

			return data;
		}

		return (caster, target, power, args) -> null;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, short def) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> data = FunctionData.build(config.getString(path), Double::shortValue, def);
			if (data == null) return (caster, target, power, args) -> def;

			return data;
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Short> def) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> data = FunctionData.build(config.getString(path), Double::shortValue, def);
			if (data == null) return def;

			return data;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> data = FunctionData.build(config.getString(path), Double::byteValue);
			if (data == null) return (caster, target, power, args) -> null;

			return data;
		}

		return (caster, target, power, args) -> null;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, byte def) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> data = FunctionData.build(config.getString(path), Double::byteValue, def);
			if (data == null) return (caster, target, power, args) -> def;

			return data;
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Byte> def) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> data = FunctionData.build(config.getString(path), Double::byteValue, def);
			if (data == null) return def;

			return data;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> data = FunctionData.build(config.getString(path), Function.identity());
			if (data == null) return (caster, target, power, args) -> null;

			return data;
		}

		return (caster, target, power, args) -> null;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, double def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> data = FunctionData.build(config.getString(path), Function.identity(), def);
			if (data == null) return (caster, target, power, args) -> def;

			return data;
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Double> def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> data = FunctionData.build(config.getString(path), Function.identity(), def);
			if (data == null) return def;

			return data;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> data = FunctionData.build(config.getString(path), Double::floatValue);
			if (data == null) return (caster, target, power, args) -> null;

			return data;
		}

		return (caster, target, power, args) -> null;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, float def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> data = FunctionData.build(config.getString(path), Double::floatValue, def);
			if (data == null) return (caster, target, power, args) -> def;

			return data;
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Float> def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> data = FunctionData.build(config.getString(path), Double::floatValue, def);
			if (data == null) return def;

			return data;
		}

		return def;
	}

	@NotNull
	public static ConfigData<String> getString(@NotNull ConfigurationSection config, @NotNull String path, @Nullable String def) {
		String value = config.getString(path, def);
		if (value == null) return (caster, target, power, args) -> null;

		return getString(value);
	}

	@NotNull
	public static ConfigData<String> getString(@Nullable String value) {
		if (value == null) return (caster, target, power, args) -> null;

		StringData data = new StringData(value);
		if (data.isConstant()) return (caster, target, power, args) -> value;

		List<ConfigData<String>> values = data.getValues();
		List<String> fragments = data.getFragments();
		if (values.size() == 1 && fragments.size() == 2 && fragments.get(0).isEmpty() && fragments.get(1).isEmpty())
			return values.get(0);

		return data;
	}

	@NotNull
	public static ConfigData<Component> getComponent(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Component def) {
		ConfigData<String> supplier = getString(config, path, null);
		if (supplier.isConstant()) {
			String value = supplier.get(null);
			if (value == null) return (caster, target, power, args) -> def;

			Component component = Util.getMiniMessage(value);
			return (caster, target, power, args) -> component;
		}

		return new ConfigData<>() {

			@Override
			public Component get(LivingEntity caster, LivingEntity target, float power, String[] args) {
				String value = supplier.get(caster, target, power, args);
				if (value == null) return def;

				return Util.getMiniMessage(value);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return (caster, target, power, args) -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			return (caster, target, power, args) -> Boolean.parseBoolean(supplier.get(caster, target, power, args));
		}

		return (caster, target, power, args) -> null;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, boolean def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return (caster, target, power, args) -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, Boolean.toString(def));
			return (caster, target, power, args) -> Boolean.parseBoolean(supplier.get(caster, target, power, args));
		}

		return (caster, target, power, args) -> def;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Boolean> def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return (caster, target, power, args) -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			return (caster, target, power, args) -> {
				String value = supplier.get(caster, target, power, args);
				return value == null ? def.get(caster, target, power, args) : Boolean.parseBoolean(value);
			};
		}

		return def;
	}

	@NotNull
	public static <T extends Enum<T>> ConfigData<T> getEnum(@NotNull ConfigurationSection config,
															@NotNull String path,
															@NotNull Class<T> type,
															@Nullable T def) {
		String value = config.getString(path);
		if (value == null) return (caster, target, power, args) -> def;

		try {
			T val = Enum.valueOf(type, value.toUpperCase());
			return (caster, target, power, args) -> val;
		} catch (IllegalArgumentException e) {
			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) return (caster, target, power, args) -> def;

			return new ConfigData<>() {

				@Override
				public T get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					String val = supplier.get(caster, target, power, args);
					if (val == null) return def;

					try {
						return Enum.valueOf(type, val.toUpperCase());
					} catch (IllegalArgumentException ex) {
						return def;
					}
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}
	}

	public static ConfigData<Material> getMaterial(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Material def) {
		String value = config.getString(path);
		if (value == null) return (caster, target, power, args) -> def;

		Material val = Util.getMaterial(value);
		if (val != null) return (caster, target, power, args) -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (caster, target, power, args) -> def;

		return new ConfigData<>() {

			@Override
			public Material get(LivingEntity caster, LivingEntity target, float power, String[] args) {
				String val = supplier.get(caster, target, power, args);
				if (val == null) return def;

				Material material = Util.getMaterial(val);
				return material == null ? def : material;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<PotionEffectType> getPotionEffectType(@NotNull ConfigurationSection config, @NotNull String path, @Nullable PotionEffectType def) {
		String value = config.getString(path);
		if (value == null) return (caster, target, power, args) -> def;

		PotionEffectType type = Util.getPotionEffectType(value);
		if (type != null) return (caster, target, power, args) -> type;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (caster, target, power, args) -> def;

		return new ConfigData<>() {

			@Override
			public PotionEffectType get(LivingEntity caster, LivingEntity target, float power, String[] args) {
				String val = supplier.get(caster, target, power, args);
				if (val == null) return def;

				PotionEffectType type = Util.getPotionEffectType(val);
				return type == null ? def : type;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<Particle> getParticle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Particle def) {
		String value = config.getString(path);
		if (value == null) return (caster, target, power, args) -> def;

		Particle val = ParticleUtil.getParticle(value);
		if (val != null) return (caster, target, power, args) -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (caster, target, power, args) -> def;

		return new ConfigData<>() {

			@Override
			public Particle get(LivingEntity caster, LivingEntity target, float power, String[] args) {
				String val = supplier.get(caster, target, power, args);
				if (val == null) return def;

				Particle particle = ParticleUtil.getParticle(val);
				return particle == null ? def : particle;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<BlockData> getBlockData(@NotNull ConfigurationSection config, @NotNull String path, @Nullable BlockData def) {
		String value = config.getString(path);
		if (value == null) return (caster, target, power, args) -> def;

		try {
			BlockData val = Bukkit.createBlockData(value.trim().toLowerCase());
			return (caster, target, power, args) -> val;
		} catch (IllegalArgumentException e) {
			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) return (caster, target, power, args) -> def;

			return new ConfigData<>() {

				@Override
				public BlockData get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					String val = supplier.get(caster, target, power, args);
					if (val == null) return def;

					try {
						return Bukkit.createBlockData(val.trim().toLowerCase());
					} catch (IllegalArgumentException e) {
						return def;
					}
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}
	}

	@NotNull
	public static ConfigData<Vector> getVector(@NotNull ConfigurationSection config, @NotNull String path, @NotNull Vector def) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (caster, target, power, args) -> def;

			String[] data = value.split(",");
			if (data.length != 3) return (caster, target, power, args) -> def;

			try {
				Vector vector = new Vector(Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]));
				return (caster, target, power, args) -> vector;
			} catch (NumberFormatException e) {
				return (caster, target, power, args) -> def;
			}
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (caster, target, power, args) -> def;

			ConfigData<Double> x = getDouble(section, "x", def.getX());
			ConfigData<Double> y = getDouble(section, "y", def.getY());
			ConfigData<Double> z = getDouble(section, "z", def.getZ());

			if (x.isConstant() && y.isConstant() && z.isConstant()) {
				Vector vector = new Vector(x.get(null), y.get(null), z.get(null));
				return (caster, target, power, args) -> vector;
			}

			return (caster, target, power, args) -> new Vector(
				x.get(caster, target, power, args),
				y.get(caster, target, power, args),
				z.get(caster, target, power, args)
			);
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<EulerAngle> getEulerAngle(@NotNull ConfigurationSection config, @NotNull String path, @NotNull EulerAngle def) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (caster, target, power, args) -> def;

			String[] data = value.split(",");
			if (data.length != 3) return (caster, target, power, args) -> def;

			try {
				EulerAngle angle = new EulerAngle(Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]));
				return (caster, target, power, args) -> angle;
			} catch (NumberFormatException e) {
				return (caster, target, power, args) -> def;
			}
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (caster, target, power, args) -> def;

			ConfigData<Double> x = getDouble(section, "x", def.getX());
			ConfigData<Double> y = getDouble(section, "y", def.getY());
			ConfigData<Double> z = getDouble(section, "z", def.getZ());

			if (x.isConstant() && y.isConstant() && z.isConstant()) {
				EulerAngle angle = new EulerAngle(x.get(null), y.get(null), z.get(null));
				return (caster, target, power, args) -> angle;
			}

			return (caster, target, power, args) -> new EulerAngle(
				x.get(caster, target, power, args),
				y.get(caster, target, power, args),
				z.get(caster, target, power, args)
			);
		}

		return (caster, target, power, args) -> def;
	}

	public static ConfigData<Color> getColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		if (config.isInt(path) || config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (caster, target, power, args) -> def;

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				Color color = ColorUtil.getColorFromHexString(value, false);
				if (color == null) return (caster, target, power, args) -> def;

				return (caster, target, power, args) -> color;
			}

			return new ConfigData<>() {

				@Override
				public Color get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					Color color = ColorUtil.getColorFromHexString(supplier.get(caster, target, power, args), false);
					return color == null ? def : color;
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (caster, target, power, args) -> def;

			ConfigData<Integer> red = getInteger(section, "red");
			ConfigData<Integer> green = getInteger(section, "green");
			ConfigData<Integer> blue = getInteger(section, "blue");

			if (red.isConstant() && green.isConstant() && blue.isConstant()) {
				Integer r = red.get(null);
				Integer g = green.get(null);
				Integer b = blue.get(null);
				if (r == null || g == null || b == null || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
					return (caster, target, power, args) -> def;

				Color c = Color.fromRGB(r, g, b);
				return (caster, target, power, args) -> c;
			}

			return new ConfigData<>() {

				@Override
				public Color get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					Integer r = red.get(caster, target, power, args);
					Integer g = green.get(caster, target, power, args);
					Integer b = blue.get(caster, target, power, args);
					if (r == null || g == null || b == null || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
						return def;

					return Color.fromRGB(r, g, b);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		return (caster, target, power, args) -> def;
	}

	public static ConfigData<Color> getARGBColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		if (config.isInt(path) || config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (caster, target, power, args) -> def;

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				Color color = ColorUtil.getColorFromARGHexString(value, false);
				if (color == null) return (caster, target, power, args) -> def;

				return (caster, target, power, args) -> color;
			}

			return new ConfigData<>() {

				@Override
				public Color get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					Color color = ColorUtil.getColorFromARGHexString(supplier.get(caster, target, power, args), false);
					return color == null ? def : color;
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (caster, target, power, args) -> def;

			ConfigData<Integer> alpha = getInteger(section, "alpha");
			ConfigData<Integer> red = getInteger(section, "red");
			ConfigData<Integer> green = getInteger(section, "green");
			ConfigData<Integer> blue = getInteger(section, "blue");

			if (alpha.isConstant() && red.isConstant() && green.isConstant() && blue.isConstant()) {
				Integer a = alpha.get(null);
				Integer r = red.get(null);
				Integer g = green.get(null);
				Integer b = blue.get(null);
				if (a == null || r == null || g == null || b == null || a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
					return (caster, target, power, args) -> def;

				Color c = Color.fromARGB(a, r, g, b);
				return (caster, target, power, args) -> c;
			}

			return new ConfigData<>() {

				@Override
				public Color get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					Integer a = alpha.get(caster, target, power, args);
					Integer r = red.get(caster, target, power, args);
					Integer g = green.get(caster, target, power, args);
					Integer b = blue.get(caster, target, power, args);
					if (a == null || r == null || g == null || b == null || a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
						return def;

					return Color.fromARGB(a, r, g, b);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		return (caster, target, power, args) -> def;
	}

	@NotNull
	public static ConfigData<DustOptions> getDustOptions(@NotNull ConfigurationSection config,
														 @NotNull String colorPath,
														 @NotNull String sizePath,
														 @Nullable DustOptions def) {
		ConfigData<Color> color = getColor(config, colorPath, def == null ? null : def.getColor());
		ConfigData<Float> size = def == null ? getFloat(config, sizePath) : getFloat(config, sizePath, def.getSize());

		if (color.isConstant() && size.isConstant()) {
			Color c = color.get(null);
			if (c == null) return (caster, target, power, args) -> def;

			Float s = size.get(null);
			if (s == null) return (caster, target, power, args) -> def;

			DustOptions options = new DustOptions(c, s);
			return (caster, target, power, args) -> options;
		}

		return new ConfigData<>() {

			@Override
			public DustOptions get(LivingEntity caster, LivingEntity target, float power, String[] args) {
				Color c = color.get(caster, target, power, args);
				if (c == null) return def;

				Float s = size.get(caster, target, power, args);
				if (s == null) return def;

				return new DustOptions(c, s);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<DustTransition> getDustTransition(@NotNull ConfigurationSection config,
															   @NotNull String colorPath,
															   @NotNull String toColorPath,
															   @NotNull String sizePath,
															   @Nullable DustTransition def) {
		ConfigData<Color> color = getColor(config, colorPath, def == null ? null : def.getColor());
		ConfigData<Color> toColor = getColor(config, toColorPath, def == null ? null : def.getToColor());
		ConfigData<Float> size = def == null ? getFloat(config, sizePath) : getFloat(config, sizePath, def.getSize());

		if (color.isConstant() && toColor.isConstant() && size.isConstant()) {
			Color c = color.get(null);
			if (c == null) return (caster, target, power, args) -> def;

			Color tc = toColor.get(null);
			if (tc == null) return (caster, target, power, args) -> def;

			Float s = size.get(null);
			if (s == null) return (caster, target, power, args) -> def;

			DustTransition transition = new DustTransition(c, tc, s);
			return (caster, target, power, args) -> transition;
		}

		return new ConfigData<>() {

			@Override
			public DustTransition get(LivingEntity caster, LivingEntity target, float power, String[] args) {
				Color c = color.get(caster, target, power, args);
				if (c == null) return def;

				Color tc = toColor.get(caster, target, power, args);
				if (tc == null) return def;

				Float s = size.get(caster, target, power, args);
				if (s == null) return def;

				return new DustTransition(c, tc, s);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

}
