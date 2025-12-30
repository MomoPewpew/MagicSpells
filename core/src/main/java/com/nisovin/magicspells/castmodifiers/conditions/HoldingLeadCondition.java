package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.MagicSpells;

public class HoldingLeadCondition extends Condition {

	private static final Set<UUID> leadingEntities = ConcurrentHashMap.newKeySet();
	private static BukkitTask resetTask;
	private static volatile boolean scannedThisTick = false;

	@Override
	public boolean initialize(String var) {
		startReset();
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isLeading(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isLeading(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	@SuppressWarnings("unused")
	// This is an unused variant that will also check whether the entity is holding a lead
	private boolean isHoldingLead(LivingEntity target) {
		EntityEquipment equipment = target.getEquipment();
		if (equipment == null) return false;

		if (!isLead(equipment.getItemInMainHand()) && !isLead(equipment.getItemInOffHand())) return false;

		return isLeading(target);
	}

	private boolean isLead(ItemStack item) {
		return item != null && item.getType() == Material.LEAD;
	}

	private boolean isLeading(LivingEntity holder) {
		ensureScanned(holder);
		return leadingEntities.contains(holder.getUniqueId());
	}

	private void ensureScanned(LivingEntity holder) {
		if (scannedThisTick) return;
		leadingEntities.clear();
		holder.getServer().getWorlds().forEach(world -> {
			for (Mob mob : world.getEntitiesByClass(Mob.class)) {
				Entity leashHolder = mob.getLeashHolder();
				if (leashHolder instanceof LivingEntity livingHolder) {
					leadingEntities.add(livingHolder.getUniqueId());
				}
			}
		});
		scannedThisTick = true;
	}

	private void startReset() {
		if (resetTask != null) return;
		resetTask = MagicSpells.getInstance().getServer().getScheduler().runTaskTimer(MagicSpells.getInstance(), () -> {
			scannedThisTick = false;
		}, 0L, 1L);
	}

}

