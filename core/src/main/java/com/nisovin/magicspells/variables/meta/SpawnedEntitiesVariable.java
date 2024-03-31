package com.nisovin.magicspells.variables.meta;

import com.nisovin.magicspells.spells.targeted.SpawnEntitySpell;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class SpawnedEntitiesVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		return (double) SpawnEntitySpell.getTotalEntities();
	}

	@Override
	public void set(String player, double amount) {
		SpawnEntitySpell.setTotalEntities((int) amount);
	}

}
