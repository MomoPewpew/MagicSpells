package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable used here
public class DeathListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (!hasSpell(entity) || !canTrigger(entity)) return;

		passiveSpell.activate(new SpellData(entity));
	}

}
