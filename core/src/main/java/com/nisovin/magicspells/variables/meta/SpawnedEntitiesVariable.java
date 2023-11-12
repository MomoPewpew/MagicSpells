package com.nisovin.magicspells.variables.meta;

import com.nisovin.magicspells.spells.targeted.SpawnEntitySpell;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class SpawnedEntitiesVariable extends MetaVariable {

	@Override
	public String getStringValue(String player) {
		return String.valueOf(SpawnEntitySpell.getTotalEntities());
	}

	@Override
	public double getValue(String player) {
		return (double) SpawnEntitySpell.getTotalEntities();
	}

}
