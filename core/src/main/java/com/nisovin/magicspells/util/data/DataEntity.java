package com.nisovin.magicspells.util.data;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

import org.bukkit.entity.Entity;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.util.Util;

public class DataEntity {
	
	private static Map<String, Function<Entity, String>> dataElements = new HashMap<>();
	
	static {
		try {
			dataElements.put("name", CommandSender::getName);
			dataElements.put("customname", entity -> Util.getStringFromComponent(entity.customName()));
			dataElements.put("portalcooldown", entity -> entity.getPortalCooldown() + "");
		} catch (Throwable ignored) {}
		
		dataElements.put("uuid", entity -> entity.getUniqueId().toString());
		dataElements.put("entitytype", entity -> entity.getType().name());
		dataElements.put("maxfireticks", entity -> entity.getMaxFireTicks() + "");
		dataElements.put("falldistance", entity -> entity.getFallDistance() + "");
		dataElements.put("fireticks", entity -> entity.getFireTicks() + "");
		dataElements.put("tickslived", entity -> entity.getTicksLived() + "");
		dataElements.put("height", entity -> entity.getHeight() + "");
		dataElements.put("width", entity -> entity.getWidth() + "");
		dataElements.put("class", entity -> entity.getClass().toString());
		dataElements.put("class.canonicalname", entity -> entity.getClass().getCanonicalName());
		dataElements.put("class.simplename", entity -> entity.getClass().getSimpleName());
		dataElements.put("lastdamagecause.cause", entity -> entity.getLastDamageCause().getCause().name());
		dataElements.put("lastdamagecause.amount", entity -> entity.getLastDamageCause().getDamage() + "");
		dataElements.put("velocity", entity -> entity.getVelocity().toString());
		dataElements.put("velocity.x", entity -> entity.getVelocity().getX() + "");
		dataElements.put("velocity.y", entity -> entity.getVelocity().getY() + "");
		dataElements.put("velocity.z", entity -> entity.getVelocity().getZ() + "");
		dataElements.put("velocity.length", entity -> entity.getVelocity().length() + "");
		dataElements.put("velocity.lengthsquared", entity -> entity.getVelocity().lengthSquared() + "");
		dataElements.put("world", entity -> entity.getWorld().toString());
		dataElements.put("world.name", entity -> entity.getWorld().getName());
		dataElements.put("world.ambientspawnlimit", entity -> entity.getWorld().getSpawnLimit(SpawnCategory.AMBIENT) + "");
		dataElements.put("world.animalspawnlimit", entity -> entity.getWorld().getSpawnLimit(SpawnCategory.ANIMAL) + "");
		dataElements.put("world.difficulty", entity -> entity.getWorld().getDifficulty().name());
		dataElements.put("world.environment", entity -> entity.getWorld().getEnvironment().name());
		dataElements.put("world.time", entity -> entity.getWorld().getTime() + "");
		dataElements.put("world.fulltime", entity -> entity.getWorld().getFullTime() + "");
		dataElements.put("world.gametime", entity -> entity.getWorld().getGameTime() + "");
		dataElements.put("world.maxheight", entity -> entity.getWorld().getMaxHeight() + "");
		dataElements.put("world.monsterspawnlimit", entity -> entity.getWorld().getSpawnLimit(SpawnCategory.MONSTER) + "");
		dataElements.put("world.sealevel", entity -> entity.getWorld().getSeaLevel() + "");
		dataElements.put("world.seed", entity -> entity.getWorld().getSeed() + "");
		dataElements.put("world.thunderduration", entity -> entity.getWorld().getThunderDuration() + "");
		dataElements.put("world.ticksperanimalspawn", entity -> entity.getWorld().getTicksPerSpawns(SpawnCategory.ANIMAL) + "");
		dataElements.put("world.tickspermonsterspawn", entity -> entity.getWorld().getTicksPerSpawns(SpawnCategory.MONSTER) + "");
		dataElements.put("world.wateranimalspawnlimit", entity -> entity.getWorld().getSpawnLimit(SpawnCategory.WATER_ANIMAL) + "");
		dataElements.put("world.weatherduration", entity -> entity.getWorld().getWeatherDuration() + "");
		dataElements.put("location", entity -> entity.getLocation().toString());
		dataElements.put("location.x", entity -> entity.getLocation().getX() + "");
		dataElements.put("location.blockx", entity -> entity.getLocation().getBlockX() + "");
		dataElements.put("location.y", entity -> entity.getLocation().getY() + "");
		dataElements.put("location.blocky", entity -> entity.getLocation().getBlockY() + "");
		dataElements.put("location.z", entity -> entity.getLocation().getZ() + "");
		dataElements.put("location.blockz", entity -> entity.getLocation().getBlockZ() + "");
		dataElements.put("location.pitch", entity -> entity.getLocation().getPitch() + "");
		dataElements.put("location.yaw", entity -> entity.getLocation().getYaw() + "");
	}
	
	public static Function<Entity, String> getDataFunction(String elementId) {
		return dataElements.get(elementId);
	}
	
}
