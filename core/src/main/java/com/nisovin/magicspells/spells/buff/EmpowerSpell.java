package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class EmpowerSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Float> maxPower;
	private ConfigData<Float> extraPower;

	private boolean powerAffectsMultiplier;

	private SpellFilter filter;

	public EmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxPower = getConfigDataFloat("max-power-multiplier", 1.5F);
		extraPower = getConfigDataFloat("power-multiplier", 1.5F);

		powerAffectsMultiplier = getConfigBoolean("power-affects-multiplier", true);

		filter = getConfigSpellFilter();

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
        assert data.caster() != null;
        entities.put(data.caster().getUniqueId(), data);
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpellCast(SpellCastEvent event) {
		LivingEntity caster = event.getCaster();
		if (caster == null || !isActive(caster)) return;
		if (!filter.check(event.getSpell())) return;

		SpellData data = entities.get(caster.getUniqueId());

		float p = extraPower.get(data);
		if (powerAffectsMultiplier) p *= data.power();
		p = Math.min(p, maxPower.get(data));

		addUseAndChargeCost(caster);
		event.increasePower(p);
	}

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

}
