package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.SpellData;

public class MoneyCondition extends OperatorCondition {

	private float money;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			money = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return money(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return money(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean money(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		if (equals) return MagicSpells.getMoneyHandler().checkMoney(pl) == money;
		else if (moreThan) return MagicSpells.getMoneyHandler().checkMoney(pl) > money;
		else if (lessThan) return MagicSpells.getMoneyHandler().checkMoney(pl) < money;
		return false;
	}

}
