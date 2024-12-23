package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.SpellData;

public class ParticleCloudSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

	private Vector relativeOffset;

	private Component customName;

	private Particle particle;
	private String particleName;

	private Material material;
	private String materialName;

	private BlockData blockData;
	private ItemStack itemStack;

	private float dustSize;
	private String colorHex;
	private Color dustColor;
	private DustOptions dustOptions;

	private boolean none = true;
	private boolean item = false;
	private boolean dust = false;
	private boolean block = false;

	private ConfigData<Integer> color;
	private ConfigData<Integer> waitTime;
	private ConfigData<Integer> ticksDuration;
	private ConfigData<Integer> durationOnUse;
	private ConfigData<Integer> reapplicationDelay;

	private ConfigData<Float> radius;
	private ConfigData<Float> radiusOnUse;
	private ConfigData<Float> radiusPerTick;

	private boolean useGravity;
	private boolean canTargetEntities;
	private boolean canTargetLocation;

	private Set<PotionEffect> potionEffects;

	public ParticleCloudSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");

		customName = Util.getMiniMessage(getConfigString("custom-name", null));

		particleName = getConfigString("particle-name", "EXPLOSION_NORMAL");
		particle = Util.getParticle(particleName);

		materialName = getConfigString("material", "");
		material = Util.getMaterial(materialName);

		dustSize = getConfigFloat("size", 1);
		colorHex = getConfigString("dust-color", "FF0000");
		dustColor = ColorUtil.getColorFromHexString(colorHex);
		if (dustColor != null) dustOptions = new DustOptions(dustColor, dustSize);

		if ((particle == Particle.BLOCK || particle == Particle.FALLING_DUST) && material != null && material.isBlock()) {
			block = true;
			blockData = material.createBlockData();
			none = false;
		} else if (particle == Particle.ITEM && material != null && material.isItem()) {
			item = true;
			itemStack = new ItemStack(material);
			none = false;
		} else if (particle == Particle.DUST && dustOptions != null) {
			dust = true;
			none = false;
		}

		if (particle == null) MagicSpells.error("ParticleCloudSpell '" + internalName + "' has a wrong particle-name defined! '" + particleName + "'");

		if ((particle == Particle.BLOCK || particle == Particle.FALLING_DUST) && (material == null || !material.isBlock())) {
			particle = null;
			MagicSpells.error("ParticleCloudSpell '" + internalName + "' has a wrong material defined! '" + materialName + "'");
		}

		if (particle == Particle.ITEM && (material == null || !material.isItem())) {
			particle = null;
			MagicSpells.error("ParticleCloudSpell '" + internalName + "' has a wrong material defined! '" + materialName + "'");
		}

		if (particle == Particle.DUST && dustColor == null) {
			particle = null;
			MagicSpells.error("ParticleCloudSpell '" + internalName + "' has a wrong dust-color defined! '" + colorHex + "'");
		}

		color = getConfigDataInt("color", 0xFF0000);
		waitTime = getConfigDataInt("wait-time-ticks", 10);
		ticksDuration = getConfigDataInt("duration-ticks", 3 * TimeUtil.TICKS_PER_SECOND);
		durationOnUse = getConfigDataInt("duration-ticks-on-use", 0);
		reapplicationDelay = getConfigDataInt("reapplication-delay-ticks", 3 * TimeUtil.TICKS_PER_SECOND);

		radius = getConfigDataFloat("radius", 5F);
		radiusOnUse = getConfigDataFloat("radius-on-use", 0F);
		radiusPerTick = getConfigDataFloat("radius-per-tick", 0F);

		useGravity = getConfigBoolean("use-gravity", false);
		canTargetEntities = getConfigBoolean("can-target-entities", true);
		canTargetLocation = getConfigBoolean("can-target-location", true);

		List<String> potionEffectStrings = getConfigStringList("potion-effects", null);
		if (potionEffectStrings == null) potionEffectStrings = new ArrayList<>();

		potionEffects = new HashSet<>();

		for (String effect: potionEffectStrings) {
			potionEffects.add(getPotionEffectFromString(effect));
		}
	}

	private static PotionEffect getPotionEffectFromString(String s) {
		String[] splits = s.split(" ");
		PotionEffectType type = Util.getPotionEffectType(splits[0]);

		int durationTicks = Integer.parseInt(splits[1]);
		int amplifier = Integer.parseInt(splits[2]);

		boolean ambient = Boolean.parseBoolean(splits[3]);
		boolean particles = Boolean.parseBoolean(splits[4]);
		boolean icon = Boolean.parseBoolean(splits[5]);

		return new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Location locToSpawn = null;
			LivingEntity target = null;

			if (canTargetEntities) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
				if (targetInfo.cancelled()) return PostCastAction.ALREADY_HANDLED;

				if (!targetInfo.empty()) {
					data = data.builder().power(targetInfo.getPower()).build();
					target = targetInfo.target();
					locToSpawn = target.getLocation();
				}
			}

			if (canTargetLocation && locToSpawn == null) {
				Block targetBlock = getTargetedBlock(data.caster(), data.power());
				if (targetBlock != null) locToSpawn = targetBlock.getLocation().add(0.5, 1, 0.5);
			}

			if (locToSpawn == null) return noTarget(data);

			locToSpawn.setDirection(data.caster().getLocation().getDirection());

			AreaEffectCloud cloud = spawnCloud(data.builder().location(locToSpawn).target(target).build());
			cloud.setSource(data.caster());
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		if (!canTargetLocation) return false;
		AreaEffectCloud cloud = spawnCloud(data);
		cloud.setSource(data.caster());
		return true;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!canTargetEntities || !validTargetList.canTarget(data.caster(), data.target())) return false;
		AreaEffectCloud cloud = spawnCloud(data.builder().location(data.target().getLocation()).build());
		cloud.setSource(data.caster());
		return true;
	}

	private AreaEffectCloud spawnCloud(SpellData data) {
		Location location = data.location().clone();
		Vector startDir = location.getDirection().normalize();

		//apply relative offset
		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
		location.add(horizOffset.multiply(relativeOffset.getZ()));
		location.add(location.getDirection().clone().multiply(relativeOffset.getX()));
		location.setY(location.getY() + relativeOffset.getY());

		AreaEffectCloud cloud = location.getWorld().spawn(location, AreaEffectCloud.class);
		if (block) cloud.setParticle(particle, blockData);
		else if (item) cloud.setParticle(particle, itemStack);
		else if (dust) cloud.setParticle(particle, dustOptions);
		else if (none) cloud.setParticle(particle);

		cloud.setColor(Color.fromRGB(color.get(data)));
		cloud.setRadius(radius.get(data));
		cloud.setGravity(useGravity);
		cloud.setWaitTime(waitTime.get(data));
		cloud.setDuration(ticksDuration.get(data));
		cloud.setDurationOnUse(durationOnUse.get(data));
		cloud.setRadiusOnUse(radiusOnUse.get(data));
		cloud.setRadiusPerTick(radiusPerTick.get(data));
		cloud.setReapplicationDelay(reapplicationDelay.get(data));

		for (PotionEffect eff : potionEffects) {
			cloud.addCustomEffect(eff, true);
		}

		if (customName != null) {
			cloud.customName(customName);
			cloud.setCustomNameVisible(true);
		}

		return cloud;
	}

}
