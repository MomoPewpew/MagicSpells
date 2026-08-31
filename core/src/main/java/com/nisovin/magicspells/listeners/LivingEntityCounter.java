package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.util.OverridePriority;

public class LivingEntityCounter implements Listener {

	private static final Map<UUID, Integer> COUNTS = new ConcurrentHashMap<>();

	public static int getCount(World world) {
		return COUNTS.getOrDefault(world.getUID(), 0);
	}

	private static void increment(World world) {
		COUNTS.merge(world.getUID(), 1, Integer::sum);
	}

	private static void decrement(World world) {
		COUNTS.compute(world.getUID(), (uuid, count) -> {
			if (count == null || count <= 1) return null;
			return count - 1;
		});
	}

	private static void bootstrap() {
		COUNTS.clear();
		for (World world : Bukkit.getWorlds()) {
			int count = 0;
			for (Chunk chunk : world.getLoadedChunks()) {
				for (Entity entity : chunk.getEntities()) {
					if (entity instanceof LivingEntity) count++;
				}
			}
			if (count > 0) COUNTS.put(world.getUID(), count);
		}
	}

	@OverridePriority
	@EventHandler
	public void onLoaded(MagicSpellsLoadedEvent event) {
		bootstrap();
	}

	@EventHandler
	public void onSpawn(EntitySpawnEvent event) {
		if (event.getEntity() instanceof LivingEntity) increment(event.getLocation().getWorld());
	}

	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity() instanceof Player) return;
		if (event.getEntity() instanceof LivingEntity) decrement(event.getEntity().getWorld());
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		increment(event.getPlayer().getWorld());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		decrement(event.getPlayer().getWorld());
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		World world = event.getWorld();
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof LivingEntity) increment(world);
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		World world = event.getWorld();
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof LivingEntity) decrement(world);
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		decrement(event.getFrom());
		increment(event.getPlayer().getWorld());
	}

	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		COUNTS.remove(event.getWorld().getUID());
	}

	public static void register() {
		MagicSpells.registerEvents(new LivingEntityCounter());
		if (MagicSpells.isLoaded()) bootstrap();
	}

}
