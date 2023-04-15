package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is used here
public class FatalDamageListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (event.getFinalDamage() < caster.getHealth()) return;
		if (!canTrigger(caster) || !hasSpell(caster)) return;

		LivingEntity attacker = getAttacker(event);

		boolean casted = passiveSpell.activate(caster, attacker);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private LivingEntity getAttacker(EntityDamageEvent event) {
		if (!(event instanceof EntityDamageByEntityEvent)) return null;
		Entity e = ((EntityDamageByEntityEvent) event).getDamager();
		if (e instanceof LivingEntity) return (LivingEntity) e;
		if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof LivingEntity) {
			return (LivingEntity) ((Projectile) e).getShooter();
		}
		return null;
	}
}
