package com.nisovin.magicspells.castmodifiers.conditions;
	
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

/**
 * Condition check to see if a player has more money than the target
 * 
 * @author TheComputerGeek2
 */
public class RicherThanCondition extends Condition {
	
	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return false;
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return richer(data.caster(), data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean richer(LivingEntity caster, LivingEntity target) {
		if (!(caster instanceof Player c) || !(target instanceof Player t)) return false;
		return MagicSpells.getMoneyHandler().checkMoney(c) > MagicSpells.getMoneyHandler().checkMoney(t);
	}

}
