package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.castmodifiers.Condition;

public class BuffActiveCondition extends Condition {

	private BuffSpell buff;

	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (spell instanceof BuffSpell) {
			buff = (BuffSpell) spell;
			return true;
		}
		return false;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return active(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return active(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean active(LivingEntity target) {
		return buff.isActiveAndNotExpired(target);
	}

}
