package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class ManaChangeListener extends PassiveListener {

	private final EnumSet<ManaChangeReason> reasons = EnumSet.noneOf(ManaChangeReason.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] data = var.split(",");
		for (String datum : data) {
			try {
				reasons.add(ManaChangeReason.valueOf(datum.toUpperCase()));
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid mana change reason '" + datum + "' in manachange trigger on passive spell '" + passiveSpell.getName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onManaChange(ManaChangeEvent event) {
		if (!reasons.isEmpty() && !reasons.contains(event.getReason())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster) || !hasSpell(caster)) return;

		passiveSpell.activate(new SpellData(caster));
	}

}
