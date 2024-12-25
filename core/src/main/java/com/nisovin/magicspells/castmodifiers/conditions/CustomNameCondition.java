package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class CustomNameCondition extends Condition {

	private boolean requireReplacement;
	private String name;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;

		requireReplacement = MagicSpells.requireReplacement(var);
		name = var.replace("__", " ");

		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return checkName(data.caster(), data.target());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return checkName(data.caster(), data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean checkName(LivingEntity caster, LivingEntity target) {
		String checkedName = requireReplacement ? MagicSpells.doReplacements(name, caster, target) : name;
		return target.customName() != null && Util.getMiniMessage(checkedName).equals(target.customName());
	}

}
