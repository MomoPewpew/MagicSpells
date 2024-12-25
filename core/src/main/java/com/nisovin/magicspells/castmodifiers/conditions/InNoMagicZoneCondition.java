package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

public class InNoMagicZoneCondition extends Condition {

	private String zone;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		zone = var;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return checkZone(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return checkZone(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return checkZone(data.location());
	}

	private boolean checkZone(Location location) {
		NoMagicZoneManager manager = MagicSpells.getNoMagicZoneManager();
		if (manager == null) return false;
		return manager.inZone(location, zone);
	}

}
