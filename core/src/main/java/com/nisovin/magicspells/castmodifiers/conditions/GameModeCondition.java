package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;


public class GameModeCondition extends Condition {

	private GameMode mode;

	@Override
	public boolean initialize(String var) {
		try {
			mode = GameMode.valueOf(var.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			mode = null;
			DebugHandler.debugIllegalArgumentException(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return gameMode(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return gameMode(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean gameMode(LivingEntity target) {
		if (mode == null) return false;
		if (!(target instanceof Player pl)) return false;
		return pl.getGameMode() == mode;
	}

}
