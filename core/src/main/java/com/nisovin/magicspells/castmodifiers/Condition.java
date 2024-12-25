package com.nisovin.magicspells.castmodifiers;

import com.nisovin.magicspells.util.SpellData;

public abstract class Condition {

	public abstract boolean initialize(String var);

	public abstract boolean checkCaster(SpellData data);

	public abstract boolean checkTarget(SpellData data);

	public abstract boolean checkLocation(SpellData data);

}
