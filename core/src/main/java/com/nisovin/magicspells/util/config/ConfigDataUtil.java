package com.nisovin.magicspells.util.config;

import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.util.ParticleUtil;
import com.nisovin.magicspells.util.SpellData;

public class ConfigDataUtil {

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			int value = config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> fdata = FunctionData.build(config.getString(path), Double::intValue);
			if (fdata == null) return (data) -> null;

			return fdata;
		}

		return (data) -> null;
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, int def) {
		if (config.isInt(path)) {
			int value = config.getInt(path, def);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> fdata = FunctionData.build(config.getString(path), Double::intValue, def);
			if (fdata == null) return (data) -> def;

			return fdata;
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Integer> def) {
		if (config.isInt(path)) {
			int value = config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> fdata = FunctionData.build(config.getString(path), Double::intValue, def);
			if (fdata == null) return def;

			return fdata;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> fdata = FunctionData.build(config.getString(path), Double::longValue);
			if (fdata == null) return (data) -> null;

			return fdata;
		}

		return (data) -> null;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, long def) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path, def);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> fdata = FunctionData.build(config.getString(path), Double::longValue, def);
			if (fdata == null) return (data) -> def;

			return fdata;
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Long> def) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> fdata = FunctionData.build(config.getString(path), Double::longValue, def);
			if (fdata == null) return def;

			return fdata;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> fdata = FunctionData.build(config.getString(path), Double::shortValue);
			if (fdata == null) return (data) -> null;

			return fdata;
		}

		return (data) -> null;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, short def) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path, def);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> fdata = FunctionData.build(config.getString(path), Double::shortValue, def);
			if (fdata == null) return (data) -> def;

			return fdata;
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Short> def) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> fdata = FunctionData.build(config.getString(path), Double::shortValue, def);
			if (fdata == null) return def;

			return fdata;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> fdata = FunctionData.build(config.getString(path), Double::byteValue);
			if (fdata == null) return (data) -> null;

			return fdata;
		}

		return (data) -> null;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, byte def) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> fdata = FunctionData.build(config.getString(path), Double::byteValue, def);
			if (fdata == null) return (data) -> def;

			return fdata;
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Byte> def) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> fdata = FunctionData.build(config.getString(path), Double::byteValue, def);
			if (fdata == null) return def;

			return fdata;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> fdata = FunctionData.build(config.getString(path), Function.identity());
			if (fdata == null) return (data) -> null;

			return fdata;
		}

		return (data) -> null;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, double def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path, def);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> fdata = FunctionData.build(config.getString(path), Function.identity(), def);
			if (fdata == null) return (data) -> def;

			return fdata;
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Double> def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> fdata = FunctionData.build(config.getString(path), Function.identity(), def);
			if (fdata == null) return def;

			return fdata;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull String string) {
		try {
            double value = Double.parseDouble(string);
			return (data) -> value;
        } catch (NumberFormatException e) {
			FunctionData<Double> fdata = FunctionData.build(string, Function.identity());
			if (fdata == null) return (data) -> null;

			return fdata;
        }
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> fdata = FunctionData.build(config.getString(path), Double::floatValue);
			if (fdata == null) return (data) -> null;

			return fdata;
		}

		return (data) -> null;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, float def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path, def);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> fdata = FunctionData.build(config.getString(path), Double::floatValue, def);
			if (fdata == null) return (data) -> def;

			return fdata;
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Float> def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return (data) -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> fdata = FunctionData.build(config.getString(path), Double::floatValue, def);
			if (fdata == null) return def;

			return fdata;
		}

		return def;
	}

	@NotNull
	public static ConfigData<String> getString(@NotNull ConfigurationSection config, @NotNull String path, @Nullable String def) {
		String value = config.getString(path, def);
		if (value == null) return (data) -> null;

		return getString(value);
	}

	@NotNull
	public static ConfigData<String> getString(@Nullable String value) {
		if (value == null) return (data) -> null;

		StringData fdata = new StringData(value);
		if (fdata.isConstant()) return (data) -> value;

		List<ConfigData<String>> values = fdata.getValues();
		List<String> fragments = fdata.getFragments();
		if (values.size() == 1 && fragments.size() == 2 && fragments.get(0).isEmpty() && fragments.get(1).isEmpty())
			return values.get(0);

		return fdata;
	}

	@NotNull
	public static ConfigData<Component> getComponent(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Component def) {
		ConfigData<String> supplier = getString(config, path, null);
		if (supplier.isConstant()) {
			String value = supplier.get(null);
			if (value == null) return (data) -> def;

			Component component = Util.getMiniMessage(value);
			return (data) -> component;
		}

		return new ConfigData<>() {

			@Override
			public Component get(SpellData data) {
				String value = supplier.get(data);
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
			return (data) -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			return (data) -> Boolean.parseBoolean(supplier.get(data));
		}

		return (data) -> null;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, boolean def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return (data) -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, Boolean.toString(def));
			return (data) -> Boolean.parseBoolean(supplier.get(data));
		}

		return (data) -> def;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Boolean> def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return (data) -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			return (data) -> {
				String value = supplier.get(data);
				return value == null ? def.get(data) : Boolean.parseBoolean(value);
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
		if (value == null) return (data) -> def;

		try {
			T val = Enum.valueOf(type, value.toUpperCase());
			return (data) -> val;
		} catch (IllegalArgumentException e) {
			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) return (data) -> def;

			return new ConfigData<>() {

				@Override
				public T get(SpellData data) {
					String val = supplier.get(data);
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
		if (value == null) return (data) -> def;

		Material val = Util.getMaterial(value);
		if (val != null) return (data) -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (data) -> def;

		return new ConfigData<>() {

			@Override
			public Material get(SpellData data) {
				String val = supplier.get(data);
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

	public static ConfigData<MagicItem> getMagicItem(@NotNull ConfigurationSection config, @NotNull String path, @Nullable String def) {
		String value = config.getString(path);
		if (value == null) return (data) -> MagicItems.getMagicItemFromString(def);

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (data) -> MagicItems.getMagicItemFromString(value);

		return new ConfigData<>() {

			@Override
			public MagicItem get(SpellData data) {
				String val = supplier.get(data);
				if (val == null) return MagicItems.getMagicItemFromString(def);

				MagicItem material = MagicItems.getMagicItemFromString(val);
				return material == null ? MagicItems.getMagicItemFromString(def) : material;
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
		if (value == null) return (data) -> def;

		PotionEffectType type = Util.getPotionEffectType(value);
		if (type != null) return (data) -> type;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (data) -> def;

		return new ConfigData<>() {

			@Override
			public PotionEffectType get(SpellData data) {
				String val = supplier.get(data);
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
		if (value == null) return (data) -> def;

		Particle val = ParticleUtil.getParticle(value);
		if (val != null) return (data) -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return (data) -> def;

		return new ConfigData<>() {

			@Override
			public Particle get(SpellData data) {
				String val = supplier.get(data);
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
		if (value == null) return (data) -> def;

		try {
			BlockData val = Bukkit.createBlockData(value.trim().toLowerCase());
			return (data) -> val;
		} catch (IllegalArgumentException e) {
			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) return (data) -> def;

			return new ConfigData<>() {

				@Override
				public BlockData get(SpellData data) {
					String val = supplier.get(data);
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
			if (value == null) return (data) -> def;

			String[] fdata = value.split(",");
			if (fdata.length != 3) return (data) -> def;

			try {
				Vector vector = new Vector(Double.parseDouble(fdata[0]), Double.parseDouble(fdata[1]), Double.parseDouble(fdata[2]));
				return (data) -> vector;
			} catch (NumberFormatException e) {
				return (data) -> def;
			}
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (data) -> def;

			ConfigData<Double> x = getDouble(section, "x", def.getX());
			ConfigData<Double> y = getDouble(section, "y", def.getY());
			ConfigData<Double> z = getDouble(section, "z", def.getZ());

			if (x.isConstant() && y.isConstant() && z.isConstant()) {
				Vector vector = new Vector(x.get(null), y.get(null), z.get(null));
				return (data) -> vector;
			}

			return (data) -> new Vector(
				x.get(data),
				y.get(data),
				z.get(data)
			);
		}

		return (data) -> def;
	}

	@NotNull
	public static ConfigData<EulerAngle> getEulerAngle(@NotNull ConfigurationSection config, @NotNull String path, @NotNull EulerAngle def) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (data) -> def;

			String[] fdata = value.split(",");
			if (fdata.length != 3) return (data) -> def;

			try {
				EulerAngle angle = new EulerAngle(Double.parseDouble(fdata[0]), Double.parseDouble(fdata[1]), Double.parseDouble(fdata[2]));
				return (data) -> angle;
			} catch (NumberFormatException e) {
				return (data) -> def;
			}
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (data) -> def;

			ConfigData<Double> x = getDouble(section, "x", def.getX());
			ConfigData<Double> y = getDouble(section, "y", def.getY());
			ConfigData<Double> z = getDouble(section, "z", def.getZ());

			if (x.isConstant() && y.isConstant() && z.isConstant()) {
				EulerAngle angle = new EulerAngle(x.get(null), y.get(null), z.get(null));
				return (data) -> angle;
			}

			return (data) -> new EulerAngle(
				x.get(data),
				y.get(data),
				z.get(data)
			);
		}

		return (data) -> def;
	}

	public static ConfigData<Color> getColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		if (config.isInt(path) || config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (data) -> def;

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				Color color = ColorUtil.getColorFromHexString(value, false);
				if (color == null) return (data) -> def;

				return (data) -> color;
			}

			return new ConfigData<>() {

				@Override
				public Color get(SpellData data) {
					Color color = ColorUtil.getColorFromHexString(supplier.get(data), false);
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
			if (section == null) return (data) -> def;

			ConfigData<Integer> red = getInteger(section, "red");
			ConfigData<Integer> green = getInteger(section, "green");
			ConfigData<Integer> blue = getInteger(section, "blue");

			if (red.isConstant() && green.isConstant() && blue.isConstant()) {
				Integer r = red.get(null);
				Integer g = green.get(null);
				Integer b = blue.get(null);
				if (r == null || g == null || b == null || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
					return (data) -> def;

				Color c = Color.fromRGB(r, g, b);
				return (data) -> c;
			}

			return new ConfigData<>() {

				@Override
				public Color get(SpellData data) {
					Integer r = red.get(data);
					Integer g = green.get(data);
					Integer b = blue.get(data);
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

		return (data) -> def;
	}

	public static ConfigData<Color> getARGBColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		if (config.isInt(path) || config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (data) -> def;

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				Color color = ColorUtil.getColorFromARGHexString(value, false);
				if (color == null) return (data) -> def;

				return (data) -> color;
			}

			return new ConfigData<>() {

				@Override
				public Color get(SpellData data) {
					Color color = ColorUtil.getColorFromARGHexString(supplier.get(data), false);
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
			if (section == null) return (data) -> def;

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
					return (data) -> def;

				Color c = Color.fromARGB(a, r, g, b);
				return (data) -> c;
			}

			return new ConfigData<>() {

				@Override
				public Color get(SpellData data) {
					Integer a = alpha.get(data);
					Integer r = red.get(data);
					Integer g = green.get(data);
					Integer b = blue.get(data);
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

		return (data) -> def;
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
			if (c == null) return (data) -> def;

			Float s = size.get(null);
			if (s == null) return (data) -> def;

			DustOptions options = new DustOptions(c, s);
			return (data) -> options;
		}

		return new ConfigData<>() {

			@Override
			public DustOptions get(SpellData data) {
				Color c = color.get(data);
				if (c == null) return def;

				Float s = size.get(data);
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
			if (c == null) return (data) -> def;

			Color tc = toColor.get(null);
			if (tc == null) return (data) -> def;

			Float s = size.get(null);
			if (s == null) return (data) -> def;

			DustTransition transition = new DustTransition(c, tc, s);
			return (data) -> transition;
		}

		return new ConfigData<>() {

			@Override
			public DustTransition get(SpellData data) {
				Color c = color.get(data);
				if (c == null) return def;

				Color tc = toColor.get(data);
				if (tc == null) return def;

				Float s = size.get(data);
				if (s == null) return def;

				return new DustTransition(c, tc, s);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

    @NotNull
    public static ConfigData<List<String>> getStringList(@NotNull ConfigurationSection config, @NotNull String path) {
        List<String> value = config.getStringList(path);
        if (value.isEmpty()) return (data) -> null;

        return getStringList(value);
    }

	@NotNull
	public static ConfigData<List<String>> getStringList(@Nullable List<String> value) {
		if (value == null || value.isEmpty()) {
			return (data) -> null;
		}
	
		List<ConfigData<String>> configDataList = new ArrayList<>();
		for (String str : value) {
			configDataList.add(getString(str));
		}
	
		return new ConfigData<>() {
	
			@Override
			public List<String> get(SpellData data) {
				List<String> results = new ArrayList<>();
				for (ConfigData<String> configData : configDataList) {
					results.add(configData.get(data));
				}
				return results;
			}
	
			@Override
			public boolean isConstant() {
				for (ConfigData<String> configData : configDataList) {
					if (!configData.isConstant()) {
						return false;
					}
				}
				return true;
			}
		};
	}

	@NotNull
	public static ConfigData<ConfigurationSection> getConfigurationSection(@NotNull ConfigurationSection config, @NotNull String path) {
		if (!config.isConfigurationSection(path)) return (data) -> null;

		ConfigurationSection section = config.getConfigurationSection(path);
		if (section == null) return (data) -> null;

		ConfigurationSection sectionData = new YamlConfiguration();


		for (String key : section.getKeys(false)) {
			if (section.isConfigurationSection(key)) {
				ConfigData<ConfigurationSection> configData = getConfigurationSection(section, key);
				sectionData.set(key, configData);
			}

			if (section.isBoolean(key)) {
				ConfigData<Boolean> configData = getBoolean(section, key);
				sectionData.set(key, configData);
			}

			if (section.isInt(key)) {
				ConfigData<Integer> configData = getInteger(section, key);
				sectionData.set(key, configData);
			}

			if (section.isDouble(key)) {
				ConfigData<Double> configData = getDouble(section, key);
				sectionData.set(key, configData);
			}

			if (section.isList(key)) {
				ConfigData<List<String>> configData = getStringList(section, key);
				sectionData.set(key, configData);
			}

			else {
				ConfigData<String> configData = getString(section, key, null);
				sectionData.set(key, configData);
			}
		}

		return new ConfigData<>() {

			@Override
			public ConfigurationSection get(SpellData data) {
				ConfigurationSection results = new YamlConfiguration();

				for (String key : sectionData.getKeys(false)) {
					ConfigData<?> fdata = (ConfigData<?>) sectionData.get(key);
					results.set(key, fdata.get(data));
				}

				return results;
			}

			@Override
			public boolean isConstant() {
				return false;
			}
		};

	}

}
