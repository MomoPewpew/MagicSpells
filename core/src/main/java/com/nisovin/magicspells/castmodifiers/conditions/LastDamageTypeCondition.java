package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class LastDamageTypeCondition extends Condition {

	private DamageCause cause;

	@Override
	public boolean initialize(String var) {
		for (DamageCause dc : DamageCause.values()) {
			if (dc.name().equalsIgnoreCase(var)) {
				cause = dc;
				return true;
			}
		}
		DebugHandler.debugBadEnumValue(DamageCause.class, var);
		return false;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return checkDamage(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return checkDamage(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean checkDamage(LivingEntity target) {
		EntityDamageEvent event = target.getLastDamageCause();
		return event != null && event.getCause() == cause;
	}

}
