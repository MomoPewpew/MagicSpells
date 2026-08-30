package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.util.OverridePriority;

public class HoldingLeadCondition extends Condition {

	private static final Map<UUID, Integer> HOLDER_COUNTS = new ConcurrentHashMap<>();
	private static final Set<UUID> TRACKED_LEASHED_MOBS = ConcurrentHashMap.newKeySet();
	private static boolean listenerRegistered;

	@Override
	public boolean initialize(String var) {
		registerTracker();
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isLeading(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isLeading(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	@SuppressWarnings("unused")
	// This is an unused variant that will also check whether the entity is holding a lead
	private boolean isHoldingLead(LivingEntity target) {
		EntityEquipment equipment = target.getEquipment();
		if (equipment == null) return false;

		if (!isLead(equipment.getItemInMainHand()) && !isLead(equipment.getItemInOffHand())) return false;

		return isLeading(target);
	}

	private boolean isLead(ItemStack item) {
		return item != null && item.getType() == Material.LEAD;
	}

	private boolean isLeading(LivingEntity holder) {
		return HOLDER_COUNTS.getOrDefault(holder.getUniqueId(), 0) > 0;
	}

	private static void registerTracker() {
		if (listenerRegistered) return;
		listenerRegistered = true;
		MagicSpells.registerEvents(new LeashHolderTracker());
		if (MagicSpells.isLoaded()) bootstrapFromLoadedChunks();
	}

	private static void incrementHolder(LivingEntity holder) {
		HOLDER_COUNTS.merge(holder.getUniqueId(), 1, Integer::sum);
	}

	private static void decrementHolder(LivingEntity holder) {
		HOLDER_COUNTS.compute(holder.getUniqueId(), (uuid, count) -> {
			if (count == null || count <= 1) return null;
			return count - 1;
		});
	}

	private static void trackMobLeash(Mob mob) {
		if (!mob.isLeashed()) return;
		if (!TRACKED_LEASHED_MOBS.add(mob.getUniqueId())) return;
		Entity holder = mob.getLeashHolder();
		if (holder instanceof LivingEntity living) incrementHolder(living);
	}

	private static void untrackMobLeash(Mob mob) {
		if (!TRACKED_LEASHED_MOBS.remove(mob.getUniqueId())) return;
		Entity holder = mob.getLeashHolder();
		if (holder instanceof LivingEntity living) decrementHolder(living);
	}

	private static void bootstrapFromLoadedChunks() {
		HOLDER_COUNTS.clear();
		TRACKED_LEASHED_MOBS.clear();
		for (World world : MagicSpells.getInstance().getServer().getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				for (Entity entity : chunk.getEntities()) {
					if (entity instanceof Mob mob) trackMobLeash(mob);
				}
			}
		}
	}

	private static class LeashHolderTracker implements Listener {

		@OverridePriority
		@EventHandler
		public void onLoaded(MagicSpellsLoadedEvent event) {
			bootstrapFromLoadedChunks();
		}

		@EventHandler
		public void onLeash(PlayerLeashEntityEvent event) {
			if (event.getEntity() instanceof Mob mob) trackMobLeash(mob);
		}

		@EventHandler
		public void onUnleash(EntityUnleashEvent event) {
			if (event.getEntity() instanceof Mob mob) untrackMobLeash(mob);
		}

		@EventHandler
		public void onSpawn(EntitySpawnEvent event) {
			if (event.getEntity() instanceof Mob mob) trackMobLeash(mob);
		}

		@EventHandler
		public void onChunkLoad(ChunkLoadEvent event) {
			for (Entity entity : event.getChunk().getEntities()) {
				if (entity instanceof Mob mob) trackMobLeash(mob);
			}
		}

		@EventHandler
		public void onDeath(EntityDeathEvent event) {
			if (event.getEntity() instanceof Mob mob) untrackMobLeash(mob);
			if (event.getEntity() instanceof LivingEntity living) HOLDER_COUNTS.remove(living.getUniqueId());
		}

	}

}
