package com.nisovin.magicspells.castmodifiers.conditions.util;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class OperatorCondition extends Condition {

	public boolean equals;
	public boolean moreThan;
	public boolean lessThan;

	@Override
	public boolean initialize(String var) {
		switch (var.charAt(0)) {
			case '=', ':' -> equals = true;
			case '>' -> moreThan = true;
			case '<' -> lessThan = true;
			default -> {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return false;
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return false;
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

}
