package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class EntityEffect extends SpellEffect {

	public static final Set<Entity> entities = new HashSet<>();

	public static final String ENTITY_TAG = "MS_ENTITY";

	private EntityData entityData;

	private int duration;

	private boolean silent;
	private boolean gravity;
	private boolean enableAI;
	private boolean riding;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		ConfigurationSection section = config.getConfigurationSection("entity");
		if (section == null) return;

		entityData = new EntityData(section);

		duration = section.getInt("duration", 0);

		silent = section.getBoolean("silent", false);
		gravity = section.getBoolean("gravity", false);
		enableAI = section.getBoolean("ai", true);
		riding = section.getBoolean("riding", false);
	}

	@Override
	protected Entity playEntityEffectLocation(Location location, SpellData data) {
		Entity entity = spawnEntity(location, data);
		entities.add(entity);

		if (duration > 0) MagicSpells.scheduleDelayedTask(() -> {
			entities.remove(entity);
			entity.remove();
		}, duration);

		return entity;
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		playEntityEffectLocation(location, data);
		return null;
	}

	protected Entity spawnEntity(Location location, SpellData data) {
		Location loc = location.clone();
		if (riding && data != null && data.caster() != null) {
			loc.add(0, data.caster().getHeight(), 0);
			loc.setPitch(0);
		}

		return entityData.spawn(loc, data, entity -> {
			entity.addScoreboardTag(ENTITY_TAG);
			entity.setGravity(gravity);
			entity.setSilent(silent);

			if (entity instanceof LivingEntity livingEntity) livingEntity.setAI(enableAI);

			if (riding && data != null && data.caster() != null){
				MagicSpells.scheduleDelayedTask(() -> {
					data.caster().addPassenger(entity);
				}, 1);
			}
		});
	}

	@Override
	public void turnOff() {
		for (Entity entity : entities) {
			entity.remove();
		}
		entities.clear();
	}

}
