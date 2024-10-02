package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable option is optional
// If not defined, it will trigger regardless of entity type
// If specified, it should be a comma separated list of entity types to accept
public class RightClickEntityListener extends PassiveListener {

	private final EnumSet<EntityType> entities = EnumSet.noneOf(EntityType.class);
	private boolean targetCitizens = false;

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty())
			return;

		String[] split = var.replace(" ", "").toUpperCase().split(",");
		for (String s : split) {
			if (s.toLowerCase().equals("citizen") || s.toLowerCase().equals("citizens")) {
				targetCitizens = true;
				continue;
			}

			EntityType type = MobUtil.getEntityType(s);
			if (type == null) {
				MagicSpells.error("Invalid entity type '" + s + "' in rightclickentity trigger on passive spell '"
						+ passiveSpell.getInternalName() + "'");
				continue;
			}

			entities.add(type);
		}
	}

	@OverridePriority
	@EventHandler
	public void onRightClickEntity(PlayerInteractAtEntityEvent event) {
		if (!isCancelStateOk(event.isCancelled()))
			return;

		Entity entity = event.getRightClicked();

		String entityClassName = entity.getClass().getCanonicalName();
		boolean isNPC = entityClassName.contains(".entity.EntityHumanNPC.PlayerNPC");

		if ((targetCitizens && (entities.isEmpty() ? !isNPC : (!entities.contains(entity.getType()) && !isNPC))) ||
				(!targetCitizens && ((!entities.isEmpty() && !entities.contains(entity.getType())) || isNPC))) {
			return;
		}

		Player caster = event.getPlayer();
		if (!hasSpell(caster) || !canTrigger(caster))
			return;

		boolean casted = entity instanceof LivingEntity ? passiveSpell.activate(caster, (LivingEntity) entity)
				: passiveSpell.activate(caster, entity.getLocation());
		if (cancelDefaultAction(casted))
			event.setCancelled(true);
	}

}
