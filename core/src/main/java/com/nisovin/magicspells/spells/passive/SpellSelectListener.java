package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellSelectionChangedEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class SpellSelectListener extends PassiveListener {

	private SpellFilter filter;

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;
		filter = SpellFilter.fromString(var);
	}

	@OverridePriority
	@EventHandler
	public void onSpellSelect(SpellSelectionChangedEvent event) {
		if (!(event.getCaster() instanceof Player caster)) return;
		if (!hasSpell(caster) || !canTrigger(caster)) return;
		if (filter != null && !filter.check(event.getSpell())) return;
		passiveSpell.activate(caster);
	}

}
