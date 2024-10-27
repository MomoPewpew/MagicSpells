package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class DummySpell extends BuffSpell {

	private final Set<UUID> entities;
	
	public DummySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
        assert data.caster() != null;
        entities.add(data.caster().getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}
	
	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}
		entities.clear();
	}

	public Set<UUID> getEntities() {
		return entities;
	}

}
