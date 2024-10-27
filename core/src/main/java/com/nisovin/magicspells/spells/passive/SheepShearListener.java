package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerShearEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable that can either be set to a dye color to accept or "all"
public class SheepShearListener extends PassiveListener {

	private final EnumSet<DyeColor> dyeColors = EnumSet.noneOf(DyeColor.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			try {
				DyeColor color = DyeColor.valueOf(s.trim().toUpperCase());
				dyeColors.add(color);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid dye color '" + s + "' in sheepshear trigger on passive spell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onSheepShear(PlayerShearEntityEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof Sheep)) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		Sheep target = (Sheep) event.getEntity();
		if (!dyeColors.isEmpty() && !dyeColors.contains(target.getColor())) return;

		boolean casted = passiveSpell.activate(new SpellData(caster, target));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
