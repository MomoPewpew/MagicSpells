package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class GillsSpell extends BuffSpell {

	private final Map<UUID, ItemStack> entities;

	private Material headMaterial;

	private boolean headEffect;
	private boolean refillAirBar;
	
	public GillsSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String headMaterialName = getConfigString("head-block", "GLASS");
		headMaterial = Util.getMaterial(headMaterialName);

		if (headMaterial == null || !headMaterial.isBlock()) {
			headMaterial = null;
			if (!headMaterialName.isEmpty()) MagicSpells.error("GillsSpell " + internalName + " has a wrong head-block defined! '" + headMaterialName + "'");
		}
		
		headEffect = getConfigBoolean("head-effect", true);
		refillAirBar = getConfigBoolean("refill-air-bar", true);
		
		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		assert data.caster() != null;
		if (headEffect && headMaterial != null) {
            EntityEquipment equipment = data.caster().getEquipment();
            assert equipment != null;
            ItemStack helmet = equipment.getHelmet();
			entities.put(data.caster().getUniqueId(), helmet);
			equipment.setHelmet(new ItemStack(headMaterial));
			return true;
		}

		entities.put(data.caster().getUniqueId(), null);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (headEffect && headMaterial != null) {
			EntityEquipment equipment = entity.getEquipment();
			equipment.setHelmet(entities.get(entity.getUniqueId()));
		}

		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		if (headEffect && headMaterial != null) {
			for (UUID id : entities.keySet()) {
				Entity entity = Bukkit.getEntity(id);
				if (!(entity instanceof LivingEntity livingEntity)) continue;
				if (!livingEntity.isValid()) continue;
				EntityEquipment equipment = livingEntity.getEquipment();
				if (equipment == null) continue;
				equipment.setHelmet(entities.get(id));
			}
		}

		entities.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity livingEntity)) return;
		if (event.getCause() != DamageCause.DROWNING) return;
		if (!isActive(livingEntity)) return;
		if (isExpired(livingEntity)) {
			turnOff(livingEntity);
			return;
		}

		event.setCancelled(true);
		addUseAndChargeCost(livingEntity);
		if (refillAirBar) livingEntity.setRemainingAir(livingEntity.getMaximumAir());
	}

	public Map<UUID, ItemStack> getEntities() {
		return entities;
	}

	public Material getHeadMaterial() {
		return headMaterial;
	}

	public void setHeadMaterial(Material headMaterial) {
		this.headMaterial = headMaterial;
	}

	public boolean hasHeadEffect() {
		return headEffect;
	}

	public void setHeadEffect(boolean headEffect) {
		this.headEffect = headEffect;
	}

	public boolean shouldRefillAirBar() {
		return refillAirBar;
	}

	public void setRefillAirBar(boolean refillAirBar) {
		this.refillAirBar = refillAirBar;
	}

}
