package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SeeInvisibilitySpell extends BuffSpell {

	private final Set<UUID> entities;
	private static HashMap<Player, List<SeeInvisibilitySpell>> entitySeeSpellMap = new HashMap<Player, List<SeeInvisibilitySpell>>();

	private List<String> invisibilitySpellNames;
	private List<InvisibilitySpell> invisibilitySpells;

	public SeeInvisibilitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();

		invisibilitySpellNames = getConfigStringList("invisibility-spells", null);
		invisibilitySpells = new ArrayList<InvisibilitySpell>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (invisibilitySpellNames != null) {
			for (String cloneSpellName : invisibilitySpellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(cloneSpellName);
				if (spell instanceof InvisibilitySpell) {
					invisibilitySpells.add((InvisibilitySpell) spell);
				} else {
					MagicSpells.error("SeeInvisibilitySpell '" + internalName + "' has an invalid spell defined in invisibility-spells!");
					return;
				}
			}
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player player)) return false;

		entities.add(entity.getUniqueId());
		addSeeInvisibilitySpell(player, this);

		List<Entity> entities = Bukkit.getServer().getWorlds().stream()
			    .flatMap(world -> world.getEntities().stream())
			    .collect(Collectors.toList());

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity livingEntity)) continue;
			if (shouldPlayerSeeEntity(player, livingEntity)) {
				player.showEntity(MagicSpells.getInstance(), livingEntity);
			}
		}

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (!(entity instanceof Player player)) return;

		entities.remove(entity.getUniqueId());
		removeSeeInvisibilitySpell(player, this);

		List<Entity> entities = Bukkit.getServer().getWorlds().stream()
			    .flatMap(world -> world.getEntities().stream())
			    .collect(Collectors.toList());

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity livingEntity)) continue;
			if (!shouldPlayerSeeEntity(player, livingEntity)) {
				player.hideEntity(MagicSpells.getInstance(), livingEntity);
			}
		}
	}

	@Override
	protected void turnOff() {
		entities.clear();
		entitySeeSpellMap.clear();
	}

	public static void addSeeInvisibilitySpell(Player player, SeeInvisibilitySpell spell) {
	    List<SeeInvisibilitySpell> spells = entitySeeSpellMap.get(player);
	    if (spells == null) {
	        spells = new ArrayList<>();
	        entitySeeSpellMap.put(player, spells);
	    }
	    spells.add(spell);
	}

	public static void removeSeeInvisibilitySpell(Player player, SeeInvisibilitySpell spell) {
	    List<SeeInvisibilitySpell> spells = entitySeeSpellMap.get(player);
	    if (spells != null) {
	        spells.remove(spell);
	        if (spells.isEmpty()) {
	        	entitySeeSpellMap.remove(player);
	        }
	    }
	}

	public static boolean shouldPlayerSeeEntity(Player observer, LivingEntity entity) {
		if (!entitySeeSpellMap.containsKey(observer)) return false;

	    Set<InvisibilitySpell> mergedSet = new HashSet<>();
	    for (SeeInvisibilitySpell spell : entitySeeSpellMap.get(observer)) {
	    	if (spell.invisibilitySpells.isEmpty()) return true;
	        mergedSet.addAll(spell.invisibilitySpells);
	    }
	    List<InvisibilitySpell> invisibilitySpells = new ArrayList<>(mergedSet);

	    for (InvisibilitySpell spell : InvisibilitySpell.getInvisibilitySpells(entity)) {
	    	if (!invisibilitySpells.contains(spell)) {
	    		return false;
	    	}
	    }

		return true;
	}
}
