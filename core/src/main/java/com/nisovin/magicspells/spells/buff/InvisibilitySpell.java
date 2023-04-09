package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

public class InvisibilitySpell extends BuffSpell {

	private final Set<UUID> entities;
	private static HashMap<LivingEntity, List<InvisibilitySpell>> entitySpellMap = new HashMap<LivingEntity, List<InvisibilitySpell>>();

	private ConfigData<Double> mobRadius;

	private boolean preventPickups;

	public InvisibilitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		mobRadius = getConfigDataDouble("mob-radius", 30);

		preventPickups = getConfigBoolean("prevent-pickups", true);

		entities = new HashSet<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		makeInvisible(entity, power, args);
		entities.add(entity.getUniqueId());
		addInvisibilitySpell(entity, this);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		removeInvisibilitySpell(entity, this);
		makeVisible(entity);
	}

	@Override
	protected void turnOff() {
		for (Entity entity : entitySpellMap.keySet()) {
			Util.forEachPlayerOnline(p -> p.showEntity(MagicSpells.getInstance(), entity));
		}

		entities.clear();
		entitySpellMap.clear();
	}

	private void makeInvisible(LivingEntity entity, float power, String[] args) {
		hideEntity(entity);

		double radius = Math.min(mobRadius.get(entity, null, power, args), MagicSpells.getGlobalRadius());
		for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
			if (!(e instanceof Creature creature)) continue;
			LivingEntity target = creature.getTarget();
			if (target == null) continue;
			if (!target.equals(entity)) continue;
			creature.setTarget(null);
		}
	}

	private static void hideEntity(LivingEntity entity) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (!SeeInvisibilitySpell.shouldPlayerSeeEntity(p, entity)) {
				p.hideEntity(MagicSpells.getInstance(), entity);
			}
		}
	}

	private void makeVisible(LivingEntity entity) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (SeeInvisibilitySpell.shouldPlayerSeeEntity(p, entity)) {
				p.showEntity(MagicSpells.getInstance(), entity);
			}
		}
	}

	@EventHandler
	public void onEntityItemPickup(EntityPickupItemEvent event) {
		if (!preventPickups) return;
		LivingEntity entity = event.getEntity();
		if (!isActive(entity)) return;
		event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (!(target instanceof LivingEntity)) return;
		if (!isActive((LivingEntity) target)) return;

		event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		for (LivingEntity entity : entitySpellMap.keySet()) {
			if (!SeeInvisibilitySpell.shouldPlayerSeeEntity(player, entity)) {
				player.hideEntity(MagicSpells.getInstance(), entity);
			}
		}

		if (isActive(player)) hideEntity((LivingEntity) player);

	}

	public static void addInvisibilitySpell(LivingEntity entity, InvisibilitySpell spell) {
	    List<InvisibilitySpell> spells = entitySpellMap.get(entity);
	    if (spells == null) {
	        spells = new ArrayList<>();
	        entitySpellMap.put(entity, spells);
	    }
	    spells.add(spell);
	}

	public static void removeInvisibilitySpell(LivingEntity entity, InvisibilitySpell spell) {
	    List<InvisibilitySpell> spells = entitySpellMap.get(entity);
	    if (spells != null) {
	        spells.remove(spell);
	        if (spells.isEmpty()) {
	            entitySpellMap.remove(entity);
	        }
	    }
	}

	public static List<InvisibilitySpell> getInvisibilitySpells(LivingEntity entity) {
	    List<InvisibilitySpell> spells = entitySpellMap.get(entity);
	    if (spells != null) {
	        return spells;
	    }
	    return Collections.emptyList();
	}
}
