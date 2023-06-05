package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.function.BiConsumer;

import org.joml.Vector3f;
import org.joml.Quaternionf;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.kyori.adventure.text.Component;

import org.bukkit.Color;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.util.Consumer;
import org.bukkit.util.EulerAngle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class EntityData {

	private final Multimap<EntityType, Transformer<?, ?>> options = MultimapBuilder.enumKeys(EntityType.class).arrayListValues().build();

	private ConfigData<EntityType> entityType;

	private final ConfigData<BlockData> fallingBlockData;
	private final ConfigData<Material> dropItemMaterial;
	private final ConfigData<Vector> relativeOffset;

	// Legacy support for DisguiseSpell section format

	// Ageable
	private final ConfigData<Boolean> baby;

	// AbstractHorse & Pig
	private final ConfigData<Boolean> saddled;

	// ChestedHorse
	private final ConfigData<Boolean> chested;

	// Creeper
	private final ConfigData<Boolean> powered;

	// Enderman
	private final ConfigData<BlockData> carriedBlockData;

	// Horse
	private final ConfigData<Horse.Color> horseColor;
	private final ConfigData<Horse.Style> horseStyle;

	// Llama
	private final ConfigData<Llama.Color> llamaColor;

	// Parrot
	private final ConfigData<Parrot.Variant> parrotVariant;

	// Puffer Fish & Slime
	private final ConfigData<Integer> size;

	// Sheep
	private final ConfigData<Boolean> sheared;

	// Sheep & Tropical Fish & Wolf
	private final ConfigData<DyeColor> color;

	// Tameable
	private final ConfigData<Boolean> tamed;

	// Tropical Fish
	private final ConfigData<TropicalFish.Pattern> tropicalFishPattern;
	private final ConfigData<DyeColor> tropicalFishPatternColor;

	// Villager
	private final ConfigData<Villager.Profession> profession;

	public EntityData(ConfigurationSection config) {
		entityType = ConfigDataUtil.getEnum(config, "entity", EntityType.class, null);

		relativeOffset = ConfigDataUtil.getVector(config, "relative-offset", new Vector(0, 0, 0));

		Multimap<Class<?>, Transformer<?, ?>> transformers = MultimapBuilder.linkedHashKeys().arrayListValues().build();

		addOptBoolean(transformers, config, "visible-by-default", Entity.class, Entity::setVisibleByDefault);

		// Ageable
		baby = addBoolean(transformers, config, "baby", false, Ageable.class, (ageable, baby) -> {
			if (baby) ageable.setBaby();
			else ageable.setAdult();
		});

		addOptInteger(transformers, config, "age", Ageable.class, Ageable::setAge);

		// Tameable
		tamed = addBoolean(transformers, config, "tamed", false, Tameable.class, Tameable::setTamed);

		// AbstractHorse
		saddled = addBoolean(transformers, config, "saddled", false, AbstractHorse.class, (horse, saddled) -> {
			if (saddled) horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
		});

		// Armor Stand
		addBoolean(transformers, config, "small", false, ArmorStand.class, ArmorStand::setSmall);
		addBoolean(transformers, config, "marker", false, ArmorStand.class, ArmorStand::setMarker);
		addBoolean(transformers, config, "visible", true, ArmorStand.class, ArmorStand::setVisible);
		addBoolean(transformers, config, "has-arms", true, ArmorStand.class, ArmorStand::setArms);
		addBoolean(transformers, config, "has-base-plate", true, ArmorStand.class, ArmorStand::setBasePlate);

		EulerAngle def = new EulerAngle(0, 0, 0);
		addEulerAngle(transformers, config, "head-angle", def, ArmorStand.class, ArmorStand::setHeadPose);
		addEulerAngle(transformers, config, "body-angle", def, ArmorStand.class, ArmorStand::setBodyPose);
		addEulerAngle(transformers, config, "left-arm-angle", def, ArmorStand.class, ArmorStand::setLeftArmPose);
		addEulerAngle(transformers, config, "right-arm-angle", def, ArmorStand.class, ArmorStand::setRightArmPose);
		addEulerAngle(transformers, config, "left-leg-angle", def, ArmorStand.class, ArmorStand::setLeftLegPose);
		addEulerAngle(transformers, config, "right-leg-angle", def, ArmorStand.class, ArmorStand::setRightLegPose);

		// Axolotl
		addOptEnum(transformers, config, "type", Axolotl.class, Axolotl.Variant.class, Axolotl::setVariant);

		// Cat
		addOptEnum(transformers, config, "type", Cat.class, Cat.Type.class, Cat::setCatType);

		// ChestedHorse
		chested = addBoolean(transformers, config, "chested", false, ChestedHorse.class, ChestedHorse::setCarryingChest);

		// Creeper
		powered = addBoolean(transformers, config, "powered", false, Creeper.class, Creeper::setPowered);

		// Dropped Item
		dropItemMaterial = ConfigDataUtil.getMaterial(config, "material", null);

		// Enderman
		carriedBlockData = addBlockData(transformers, config, "material", null, Enderman.class, Enderman::setCarriedBlock);

		// Falling Block
		fallingBlockData = ConfigDataUtil.getBlockData(config, "material", null);

		// Fox
		addOptEnum(transformers, config, "type", Fox.class, Fox.Type.class, Fox::setFoxType);

		// Frog
		addOptEnum(transformers, config, "type", Frog.class, Frog.Variant.class, Frog::setVariant);

		// Horse
		horseColor = addOptEnum(transformers, config, "color", Horse.class, Horse.Color.class, Horse::setColor);
		horseStyle = addOptEnum(transformers, config, "style", Horse.class, Horse.Style.class, Horse::setStyle);

		// Llama
		llamaColor = addOptEnum(transformers, config, "color", Llama.class, Llama.Color.class, Llama::setColor);
		addOptMaterial(transformers, config, "material", Llama.class, (llama, material) -> llama.getInventory().setDecor(new ItemStack(material)));

		// Mushroom Cow
		addOptEnum(transformers, config, "type", MushroomCow.class, MushroomCow.Variant.class, MushroomCow::setVariant);

		// Panda
		addOptEnum(transformers, config, "main-gene", Panda.class, Panda.Gene.class, Panda::setMainGene);
		addOptEnum(transformers, config, "hidden-gene", Panda.class, Panda.Gene.class, Panda::setHiddenGene);

		// Parrot
		parrotVariant = addOptEnum(transformers, config, "type", Parrot.class, Parrot.Variant.class, Parrot::setVariant);

		// Phantom
		addInteger(transformers, config, "size", 0, Phantom.class, Phantom::setSize);

		// Puffer Fish
		size = addInteger(transformers, config, "size", 0, PufferFish.class, PufferFish::setPuffState);

		// Rabbit
		addOptEnum(transformers, config, "type", Rabbit.class, Rabbit.Type.class, Rabbit::setRabbitType);

		// Sheep
		sheared = addBoolean(transformers, config, "sheared", false, Sheep.class, Sheep::setSheared);
		color = addOptEnum(transformers, config, "color", Sheep.class, DyeColor.class, Sheep::setColor);

		// Slime
		addInteger(transformers, config, "size", 0, Slime.class, Slime::setSize);

		// Steerable
		addBoolean(transformers, config, "saddled", false, Steerable.class, Steerable::setSaddle);

		// Tropical Fish
		addOptEnum(transformers, config, "color", TropicalFish.class, DyeColor.class, TropicalFish::setBodyColor);
		tropicalFishPatternColor = addOptEnum(transformers, config, "pattern-color", TropicalFish.class, DyeColor.class, TropicalFish::setPatternColor);
		tropicalFishPattern = addOptEnum(transformers, config, "type", TropicalFish.class, TropicalFish.Pattern.class, TropicalFish::setPattern);

		// Villager
		profession = addOptEnum(transformers, config, "type", Villager.class, Villager.Profession.class, Villager::setProfession);

		// Wolf
		addBoolean(transformers, config, "angry", false, Wolf.class, Wolf::setAngry);
		addOptEnum(transformers, config, "color", Wolf.class, DyeColor.class, Wolf::setCollarColor);

		if (Bukkit.getMinecraftVersion().contains("1.19.4")) {
			// Display
			ConfigData<Quaternionf> leftRotation = getQuaternion(config, "transformation.left-rotation");
			ConfigData<Quaternionf> rightRotation = getQuaternion(config, "transformation.right-rotation");
			ConfigData<Vector3f> translation = getVector(config, "transformation.translation");
			ConfigData<Vector3f> scale = getVector(config, "transformation.scale");
			ConfigData<Transformation> transformation = (caster, target, power, args) -> null;
			if (checkNull(leftRotation) && checkNull(rightRotation) && checkNull(translation) && checkNull(scale)) {
				if (leftRotation.isConstant() && rightRotation.isConstant() && translation.isConstant() && scale.isConstant()) {
					Quaternionf lr = leftRotation.get(null);
					Quaternionf rr = rightRotation.get(null);
					Vector3f t = translation.get(null);
					Vector3f s = scale.get(null);

					Transformation transform = new Transformation(t, lr, s, rr);
					transformation = (caster, target, power, args) -> transform;
				} else {
					transformation = (caster, target, power, args) -> {
						Quaternionf lr = leftRotation.get(caster, target, power, args);
						if (lr == null) return null;

						Quaternionf rr = rightRotation.get(caster, target, power, args);
						if (rr == null) return null;

						Vector3f t = translation.get(caster, target, power, args);
						if (t == null) return null;

						Vector3f s = scale.get(caster, target, power, args);
						if (s == null) return null;

						return new Transformation(t, lr, s, rr);
					};
				}
			}
			transformers.put(Display.class, new Transformer<>(transformation, Display::setTransformation, true));

			addOptInteger(transformers, config, "interpolation-duration", Display.class, Display::setInterpolationDuration);
			addOptFloat(transformers, config, "view-range", Display.class, Display::setViewRange);
			addOptFloat(transformers, config, "shadow-radius", Display.class, Display::setShadowRadius);
			addOptFloat(transformers, config, "shadow-strength", Display.class, Display::setShadowStrength);
			addOptFloat(transformers, config, "width", Display.class, Display::setDisplayWidth);
			addOptFloat(transformers, config, "height", Display.class, Display::setDisplayHeight);
			addOptInteger(transformers, config, "interpolation-delay", Display.class, Display::setInterpolationDelay);
			addOptEnum(transformers, config, "billboard", Display.class, Display.Billboard.class, Display::setBillboard);
			addOptColor(transformers, config, "glow-color-override", Display.class, Display::setGlowColorOverride);

			ConfigData<Integer> blockLight = ConfigDataUtil.getInteger(config, "brightness.block");
			ConfigData<Integer> skyLight = ConfigDataUtil.getInteger(config, "brightness.sky");
			ConfigData<Display.Brightness> brightness = (caster, target, power, args) -> null;
			if (checkNull(blockLight) && checkNull(skyLight)) {
				if (blockLight.isConstant() && skyLight.isConstant()) {
					int bl = blockLight.get(null);
					int sl = skyLight.get(null);

					if (0 <= bl && bl <= 15 && 0 <= sl && sl <= 15) {
						Display.Brightness b = new Display.Brightness(bl, sl);
						brightness = (caster, target, power, args) -> b;
					}
				} else {
					brightness = (caster, target, power, args) -> {
						Integer bl = blockLight.get(caster, target, power, args);
						if (bl == null || bl < 0 || bl > 15) return null;

						Integer sl = skyLight.get(caster, target, power, args);
						if (sl == null || sl < 0 || sl > 15) return null;

						return new Display.Brightness(bl, sl);
					};
				}
			}
			transformers.put(Display.class, new Transformer<>(brightness, Display::setBrightness, true));

			// BlockDisplay
			addOptBlockData(transformers, config, "block", BlockDisplay.class, BlockDisplay::setBlock);

			// ItemDisplay
			MagicItem magicItem = MagicItems.getMagicItemFromString(config.getString("item"));
			if (magicItem != null) {
				ItemStack item = magicItem.getItemStack();
				transformers.put(ItemDisplay.class, new Transformer<>((caster, target, power, args) -> item, ItemDisplay::setItemStack));
			}

			addOptEnum(transformers, config, "item-display-transform", ItemDisplay.class, ItemDisplay.ItemDisplayTransform.class, ItemDisplay::setItemDisplayTransform);

			// TextDisplay
			addOptComponent(transformers, config, "text", TextDisplay.class, TextDisplay::text);
			addOptInteger(transformers, config, "line-width", TextDisplay.class, TextDisplay::setLineWidth);
			addOptColor(transformers, config, "background", TextDisplay.class, TextDisplay::setBackgroundColor);
			addOptByte(transformers, config, "text-opacity", TextDisplay.class, TextDisplay::setTextOpacity);
			addOptBoolean(transformers, config, "shadow", TextDisplay.class, TextDisplay::setShadowed);
			addOptBoolean(transformers, config, "see-through", TextDisplay.class, TextDisplay::setSeeThrough);
			addOptBoolean(transformers, config, "default-background", TextDisplay.class, TextDisplay::setDefaultBackground);
			addOptEnum(transformers, config, "alignment", TextDisplay.class, TextDisplay.TextAlignment.class, TextDisplay::setAlignment);
		}

		for (EntityType entityType : EntityType.values()) {
			Class<? extends Entity> entityClass = entityType.getEntityClass();
			if (entityClass == null) continue;

			for (Class<?> transformerType : transformers.keys())
				if (transformerType.isAssignableFrom(entityClass))
					options.putAll(entityType, transformers.get(transformerType));
		}
	}

	@Nullable
	public Entity spawn(@NotNull Location location) {
		return spawn(location, null, null);
	}

	@Nullable
	public Entity spawn(@NotNull Location location, @Nullable Consumer<Entity> consumer) {
		return spawn(location, null, consumer);
	}

	@Nullable
	public Entity spawn(@NotNull Location location, @Nullable SpellData data, @Nullable Consumer<Entity> consumer) {
		Location startLoc = location.clone();
		Vector dir = startLoc.getDirection().normalize();
		Vector relativeOffset = this.relativeOffset.get(data);

		Vector horizOffset = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
		startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		startLoc.add(startLoc.getDirection().clone().multiply(relativeOffset.getX()));
		startLoc.setY(startLoc.getY() + relativeOffset.getY());

		EntityType entityType = this.entityType.get(data);
		if (entityType == null || (!entityType.isSpawnable() && entityType != EntityType.FALLING_BLOCK && entityType != EntityType.DROPPED_ITEM))
			return null;

		boolean[] displayHack = new boolean[] {false, false};
		Entity entity = switch (entityType) {
			case FALLING_BLOCK -> {
				BlockData blockData = fallingBlockData.get(data);
				if (blockData == null) yield null;

				Entity e = startLoc.getWorld().spawnFallingBlock(startLoc, blockData);
				if (consumer != null) consumer.accept(e);

				yield e;
			}
			case DROPPED_ITEM -> {
				Material material = dropItemMaterial.get(data);
				if (material == null) yield null;

				Entity e = startLoc.getWorld().dropItem(startLoc, new ItemStack(material));
				if (consumer != null) consumer.accept(e);

				yield e;
			}
			default -> {
				Class<? extends Entity> entityClass = entityType.getEntityClass();
				if (entityClass == null) yield null;

				yield startLoc.getWorld().spawn(startLoc, entityClass, e -> {
					Collection<Transformer<?, ?>> transformers = options.get(entityType);
					//noinspection rawtypes
					for (Transformer transformer : transformers)
						//noinspection unchecked
						transformer.apply(e, data);

					if (consumer != null) consumer.accept(e);

					if (Bukkit.getMinecraftVersion().contains("1.19.4") && e instanceof Display) {
						displayHack[0] = true;
						displayHack[1] = e.isVisibleByDefault();

						e.setVisibleByDefault(false);
					}
				});
			}
		};

		if (displayHack[0]) {
			entity.teleport(startLoc);
			entity.setVisibleByDefault(displayHack[1]);
		}

		return entity;
	}

	private <T> ConfigData<Boolean> addBoolean(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, boolean def, Class<T> type, BiConsumer<T, Boolean> setter) {
		ConfigData<Boolean> supplier = ConfigDataUtil.getBoolean(config, name, def);
		transformers.put(type, new Transformer<>(supplier, setter));

		return supplier;
	}

	private <T> ConfigData<Integer> addInteger(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, int def, Class<T> type, BiConsumer<T, Integer> setter) {
		ConfigData<Integer> supplier = ConfigDataUtil.getInteger(config, name, def);
		transformers.put(type, new Transformer<>(supplier, setter));

		return supplier;
	}

	private <T> ConfigData<BlockData> addBlockData(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, BlockData def, Class<T> type, BiConsumer<T, BlockData> setter) {
		ConfigData<BlockData> supplier = ConfigDataUtil.getBlockData(config, name, def);
		transformers.put(type, new Transformer<>(supplier, setter));

		return supplier;
	}

	private <T> void addEulerAngle(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, EulerAngle def, Class<T> type, BiConsumer<T, EulerAngle> setter) {
		ConfigData<EulerAngle> supplier = ConfigDataUtil.getEulerAngle(config, name, def);
		transformers.put(type, new Transformer<>(supplier, setter));
	}

	private <T> void addOptBoolean(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Boolean> setter) {
		ConfigData<Boolean> supplier = ConfigDataUtil.getBoolean(config, name);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	private <T> void addOptByte(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Byte> setter) {
		ConfigData<Byte> supplier = ConfigDataUtil.getByte(config, name);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	private <T> void addOptInteger(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Integer> setter) {
		ConfigData<Integer> supplier = ConfigDataUtil.getInteger(config, name);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	private <T> void addOptFloat(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Float> setter) {
		ConfigData<Float> supplier = ConfigDataUtil.getFloat(config, name);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	private <T, E extends Enum<E>> ConfigData<E> addOptEnum(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, Class<E> enumType, BiConsumer<T, E> setter) {
		ConfigData<E> supplier = ConfigDataUtil.getEnum(config, name, enumType, null);
		transformers.put(type, new Transformer<>(supplier, setter, true));

		return supplier;
	}

	private <T> void addOptMaterial(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Material> setter) {
		ConfigData<Material> supplier = ConfigDataUtil.getMaterial(config, name, null);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	private <T> void addOptColor(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Color> setter) {
		ConfigData<Color> supplier = ConfigDataUtil.getColor(config, name, null);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}


	private <T> void addOptComponent(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, Component> setter) {
		ConfigData<Component> supplier = ConfigDataUtil.getComponent(config, name, null);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	private <T> void addOptBlockData(Multimap<Class<?>, Transformer<?, ?>> transformers, ConfigurationSection config, String name, Class<T> type, BiConsumer<T, BlockData> setter) {
		ConfigData<BlockData> supplier = ConfigDataUtil.getBlockData(config, name, null);
		transformers.put(type, new Transformer<>(supplier, setter, true));
	}

	public ConfigData<Vector3f> getVector(ConfigurationSection config, String path) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (caster, target, power, args) -> null;

			String[] data = value.split(",");
			if (data.length != 3) return (caster, target, power, args) -> null;

			try {
				Vector3f vector = new Vector3f(Float.parseFloat(data[0]), Float.parseFloat(data[1]), Float.parseFloat(data[2]));
				return (caster, target, power, args) -> vector;
			} catch (NumberFormatException e) {
				return (caster, target, power, args) -> null;
			}
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return (caster, target, power, args) -> null;

			ConfigData<Float> x = ConfigDataUtil.getFloat(section, "x");
			ConfigData<Float> y = ConfigDataUtil.getFloat(section, "y");
			ConfigData<Float> z = ConfigDataUtil.getFloat(section, "z");

			if (checkNull(x) && checkNull(y) && checkNull(z)) {
				if (x.isConstant() && y.isConstant() && z.isConstant()) {
					float vx = x.get(null);
					float vy = y.get(null);
					float vz = z.get(null);

					Vector3f vector = new Vector3f(vx, vy, vz);
					return (caster, target, power, args) -> vector;
				}

				return new ConfigData<>() {

					@Override
					public Vector3f get(LivingEntity caster, LivingEntity target, float power, String[] args) {
						Float vx = x.get(caster, target, power, args);
						if (vx == null) return null;

						Float vy = y.get(caster, target, power, args);
						if (vy == null) return null;

						Float vz = z.get(caster, target, power, args);
						if (vz == null) return null;

						return new Vector3f(vx, vy, vz);
					}

					@Override
					public boolean isConstant() {
						return false;
					}

				};
			}
		}

		return (caster, target, power, args) -> null;
	}

	private ConfigData<Quaternionf> getQuaternion(ConfigurationSection config, String path) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return (caster, target, power, args) -> null;

			String[] data = value.split(",");
			if (data.length != 4) return (caster, target, power, args) -> null;

			try {
				Quaternionf rot = new Quaternionf(Float.parseFloat(data[0]), Float.parseFloat(data[1]), Float.parseFloat(data[2]), Float.parseFloat(data[3]));
				return (caster, target, power, args) -> rot;
			} catch (NumberFormatException e) {
				return (caster, target, power, args) -> null;
			}
		}

		ConfigData<Float> angle = ConfigDataUtil.getFloat(config, path + ".angle");
		ConfigData<Vector3f> axis = getVector(config, path + ".axis");
		if (checkNull(angle) && checkNull(axis)) {
			if (angle.isConstant() && axis.isConstant()) {
				Vector3f ax = axis.get(null);
				float ang = angle.get(null);

				Quaternionf rot = new Quaternionf();
				rot.setAngleAxis(ang, ax.x, ax.y, ax.z);

				return (caster, target, power, args) -> rot;
			}

			return new ConfigData<>() {

				@Override
				public Quaternionf get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					Float ang = angle.get(caster, target, power, args);
					if (ang == null) return null;

					Vector3f ax = axis.get(null);
					if (ax == null) return null;

					return new Quaternionf().setAngleAxis(ang, ax.x, ax.y, ax.z);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		ConfigData<Float> x = ConfigDataUtil.getFloat(config, path + ".x");
		ConfigData<Float> y = ConfigDataUtil.getFloat(config, path + ".y");
		ConfigData<Float> z = ConfigDataUtil.getFloat(config, path + ".z");
		ConfigData<Float> w = ConfigDataUtil.getFloat(config, path + ".w");
		if (checkNull(x) && checkNull(y) && checkNull(z) && checkNull(w)) {
			if (x.isConstant() && y.isConstant() && z.isConstant() && w.isConstant()) {
				float qx = x.get(null);
				float qy = y.get(null);
				float qz = z.get(null);
				float qw = w.get(null);

				Quaternionf rot = new Quaternionf(qx, qy, qz, qw);
				return (caster, target, power, args) -> rot;
			}

			return new ConfigData<>() {

				@Override
				public Quaternionf get(LivingEntity caster, LivingEntity target, float power, String[] args) {
					Float qx = x.get(caster, target, power, args);
					if (qx == null) return null;

					Float qy = y.get(caster, target, power, args);
					if (qy == null) return null;

					Float qz = z.get(caster, target, power, args);
					if (qz == null) return null;

					Float qw = w.get(caster, target, power, args);
					if (qw == null) return null;

					return new Quaternionf(qx, qy, qz, qw);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		return (caster, target, power, args) -> null;
	}

	private boolean checkNull(ConfigData<?> data) {
		return !data.isConstant() || data.get(null) != null;
	}

	public Multimap<EntityType, Transformer<?, ?>> getOptions() {
		return options;
	}

	public ConfigData<EntityType> getEntityType() {
		return entityType;
	}

	public ConfigData<Vector> getRelativeOffset() {
		return relativeOffset;
	}

	public void setEntityType(ConfigData<EntityType> entityType) {
		this.entityType = entityType;
	}

	@ApiStatus.Internal
	public ConfigData<Material> getDroppedItemStack() {
		return dropItemMaterial;
	}

	@ApiStatus.Internal
	public ConfigData<BlockData> getFallingBlockData() {
		return fallingBlockData;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getBaby() {
		return baby;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getChested() {
		return chested;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getPowered() {
		return powered;
	}

	@ApiStatus.Internal
	public ConfigData<BlockData> getCarriedBlockData() {
		return carriedBlockData;
	}

	@ApiStatus.Internal
	public ConfigData<Horse.Color> getHorseColor() {
		return horseColor;
	}

	@ApiStatus.Internal
	public ConfigData<Horse.Style> getHorseStyle() {
		return horseStyle;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getSaddled() {
		return saddled;
	}

	@ApiStatus.Internal
	public ConfigData<Llama.Color> getLlamaColor() {
		return llamaColor;
	}

	@ApiStatus.Internal
	public ConfigData<Parrot.Variant> getParrotVariant() {
		return parrotVariant;
	}

	@ApiStatus.Internal
	public ConfigData<Integer> getSize() {
		return size;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getSheared() {
		return sheared;
	}

	@ApiStatus.Internal
	public ConfigData<DyeColor> getColor() {
		return color;
	}

	@ApiStatus.Internal
	public ConfigData<Boolean> getTamed() {
		return tamed;
	}

	@ApiStatus.Internal
	public ConfigData<TropicalFish.Pattern> getTropicalFishPattern() {
		return tropicalFishPattern;
	}

	@ApiStatus.Internal
	public ConfigData<DyeColor> getTropicalFishPatternColor() {
		return tropicalFishPatternColor;
	}

	@ApiStatus.Internal
	public ConfigData<Villager.Profession> getProfession() {
		return profession;
	}

	public record Transformer<T, C>(ConfigData<C> supplier, BiConsumer<T, C> setter, boolean optional) {

		public Transformer(ConfigData<C> supplier, BiConsumer<T, C> setter) {
			this(supplier, setter, false);
		}

		public void apply(T entity, SpellData data) {
			C value = supplier.get(data);
			if (!optional || value != null) setter.accept(entity, value);
		}

	}

}