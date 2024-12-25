package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class PlayerOnlineCondition extends Condition {
	
	private String name;
	
	@Override
	public boolean initialize(String var) {
		name = var;
		return true;
	}
	
	@Override
	public boolean checkCaster(SpellData data) {
		return isOnline();
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		return isOnline();
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return isOnline();
	}

	private boolean isOnline() {
		return PlayerNameUtils.getPlayerExact(name) != null;
	}

}
