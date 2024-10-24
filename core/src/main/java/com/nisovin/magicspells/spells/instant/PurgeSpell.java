package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class PurgeSpell extends InstantSpell implements TargetedLocationSpell {

	private List<EntityType> entities;

	private ConfigData<Double> radius;

	private boolean powerAffectsRadius;

	public PurgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataDouble("radius", 15);

		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);

		List<String> list = getConfigStringList("entities", null);
		if (list != null && !list.isEmpty()) {
			entities = new ArrayList<>();
			for (String s : list) {
				EntityType t = MobUtil.getEntityType(s);
				if (t != null) entities.add(t);
				else MagicSpells.error("PurgeSpell '" + internalName + "' has an invalid entity defined: " + s);
			}

			if (entities.isEmpty()) entities = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			boolean killed = purge(caster, caster.getLocation(), power, args);
			if (!killed) return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		boolean killed = purge(caster, target, power, args);
		if (killed && caster != null) playSpellEffects(EffectPosition.CASTER, caster, power, args);
		return killed;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

	private boolean purge(LivingEntity caster, Location loc, float power, String[] args) {
		double castingRange = radius.get(caster, null, power, args);
		if (powerAffectsRadius) castingRange *= power;
		castingRange = Math.min(castingRange, MagicSpells.getGlobalRadius());

		Collection<Entity> entitiesNearby = loc.getWorld().getNearbyEntities(loc, castingRange, castingRange, castingRange);
		boolean killed = false;
		for (Entity entity : entitiesNearby) {
			if (!(entity instanceof LivingEntity livingEntity)) continue;
			if (entity instanceof Player) continue;
			if (entities != null && !entities.contains(entity.getType())) continue;

			SpellData data = new SpellData(caster, livingEntity, power, args);
			playSpellEffectsTrail(loc, entity.getLocation(), data);
			playSpellEffects(EffectPosition.TARGET, entity, data);

			livingEntity.setHealth(0);
			killed = true;
		}

		return killed;
	}

	public List<EntityType> getEntities() {
		return entities;
	}

}
