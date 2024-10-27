package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class CraftSpell extends InstantSpell {

	public CraftSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && caster instanceof Player) {
			((Player) caster).openWorkbench(null, true);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
