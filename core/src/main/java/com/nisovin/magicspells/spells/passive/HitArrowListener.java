package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class HitArrowListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			s = s.trim();

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid magic item '" + s + "' in hitarrow trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			items.add(itemData);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof LivingEntity attacked)) return;
		if (!isCancelStateOk(event.isCancelled())) return;

		LivingEntity caster = getAttacker(event);
		if (caster == null || !hasSpell(caster) || !canTrigger(caster)) return;

		if (!items.isEmpty()) {
			EntityEquipment eq = caster.getEquipment();
			if (eq == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(eq.getItemInMainHand());
			if (itemData == null || !contains(itemData)) return;
		}

		boolean casted = passiveSpell.activate(new SpellData(caster, attacked));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}
	
	private LivingEntity getAttacker(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Arrow arrow)) return null;
		if (arrow.getShooter() != null && arrow.getShooter() instanceof LivingEntity shooter) return shooter;
		return null;
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.matches(itemData)) return true;
		}
		return false;
	}

}
