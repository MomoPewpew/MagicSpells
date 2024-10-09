package com.nisovin.magicspells.spelleffects;

import java.util.Map;
import java.util.HashMap;

public enum EffectPosition {

	START_CAST("start", "startcast"),
	CASTER("caster", "actor"),
	TARGET("target"),
	START_POSITION("startposition", "startpos", "pos1", "position1"),
	END_POSITION( "endposition", "endpos", "pos2", "position2"),
	TRAIL("line", "trail"),
	DISABLED("disabled"),
	DELAYED("delayed"),
	SPECIAL("special"),
	BUFF("buff", "active"),
	BUFF_EFFECTLIB("buffeffectlib"),
	ORBIT("orbit"),
	ORBIT_EFFECTLIB("orbiteffectlib"),
	REVERSE_LINE("reverse_line", "reverseline", "rline"),
	PROJECTILE("projectile"),
	DYNAMIC_CASTER_PROJECTILE_LINE("casterprojectile", "casterprojectileline"),
	BLOCK_DESTRUCTION("blockdestroy", "blockdestruction"),
	COOLDOWN("cooldown"),
	MISSING_REAGENTS("missingreagents"),
	CHARGE_USE("chargeuse"),

	;

	private final String[] names;

	private static Map<String, EffectPosition> nameMap = new HashMap<>();
	private static boolean initialized = false;

	EffectPosition(String... names) {
		this.names = names;
	}

	private static void initializeNameMap() {
		if (nameMap == null) nameMap = new HashMap<>();
		nameMap.clear();
		for (EffectPosition position : EffectPosition.values()) {
			// For all of the names
			for (String name : position.names) {
				nameMap.put(name.toLowerCase(), position);
			}
		}
		initialized = true;
	}

	public static EffectPosition getPositionFromString(String position) {
		if (!initialized) initializeNameMap();
		return nameMap.get(position.toLowerCase());
	}

}
