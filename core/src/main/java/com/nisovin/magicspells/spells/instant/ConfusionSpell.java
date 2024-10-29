package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ConfusionSpell extends InstantSpell implements TargetedLocationSpell {

	private final ConfigData<Double> radius;
	private final boolean powerAffectsRadius;
	
	public ConfusionSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataDouble("radius", 10);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			confuse(data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		confuse(data);
		return true;
	}

	private void confuse(SpellData data) {
		LivingEntity caster = data.caster();
		Location location = data.location();
		float power = data.power();

		double castingRange = radius.get(data);
		if (powerAffectsRadius) castingRange = castingRange * power;

		castingRange = Math.min(castingRange, MagicSpells.getGlobalRadius());

		Collection<Entity> entities = location.getWorld().getNearbyEntities(location, castingRange, castingRange, castingRange);
		List<LivingEntity> monsters = new ArrayList<>();

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity livingEntity)) continue;
			if (!validTargetList.canTarget(caster, e)) continue;
			monsters.add(livingEntity);
		}

		for (int i = 0; i < monsters.size(); i++) {
			int next = i + 1;
			if (next >= monsters.size()) next = 0;
			MobUtil.setTarget(monsters.get(i), monsters.get(next));

			playSpellEffects(EffectPosition.TARGET, monsters.get(i), data);
			playSpellEffectsTrail(caster.getLocation(), monsters.get(i).getLocation(), data);
		}
		playSpellEffects(EffectPosition.CASTER, caster, data);
	}

}
