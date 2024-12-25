package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.compat.CompatBasics;

public class PluginEnabledCondition extends Condition {

	private String pluginName = null;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		var = var.trim();
		if (var.isEmpty()) return false;
		pluginName = var;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return checkPlugin();
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return checkPlugin();
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return checkPlugin();
	}
	
	private boolean checkPlugin() {
		if (pluginName == null) return false;
		return CompatBasics.pluginEnabled(pluginName);
	}

}
