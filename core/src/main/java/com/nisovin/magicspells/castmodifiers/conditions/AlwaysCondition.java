package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class AlwaysCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return true;
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return true;
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return true;
	}

}
