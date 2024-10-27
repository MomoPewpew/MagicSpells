package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerFishEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable can optionally include a comma-separated list of
// fish event states and entity types
public class FishListener extends PassiveListener {

	private final EnumSet<PlayerFishEvent.State> states = EnumSet.noneOf(PlayerFishEvent.State.class);
	private final EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] data = var.replace(" ", "").toUpperCase().split(",");
		for (String val : data) {
			try {
				states.add(PlayerFishEvent.State.valueOf(val.toUpperCase()));
			} catch (IllegalArgumentException e) {
				EntityType type = MobUtil.getEntityType(val);
				if (type == null) {
					MagicSpells.error("Invalid fish event state or entity type '" + val
						+ "' in fish trigger on passive spell '" + passiveSpell.getName() + "'");
					continue;
				}

				types.add(type);
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onFish(PlayerFishEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		if (!states.isEmpty() && !states.contains(event.getState())) return;

		Entity caught = event.getCaught();
		if (!types.isEmpty() && (caught == null || !types.contains(caught.getType()))) return;

		boolean casted = caught instanceof LivingEntity ? passiveSpell.activate(new SpellData(caster, (LivingEntity) caught)) :
			passiveSpell.activate(new SpellData(caster, event.getHook().getLocation()));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
