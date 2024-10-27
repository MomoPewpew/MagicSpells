package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityToggleSwimEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class StartSwimListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onSwim(EntityToggleSwimEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!event.isSwimming()) return;
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(new SpellData(caster));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
