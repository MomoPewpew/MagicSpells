package com.nisovin.magicspells.volatilecode;

import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class VolatileCodeHandle {

	protected final VolatileCodeHelper helper;

	public VolatileCodeHandle(VolatileCodeHelper helper) {
		this.helper = helper;
	}

	public abstract void addPotionGraphicalEffect(LivingEntity entity, int color, long duration);

	public abstract void sendFakeSlotUpdate(Player player, int slot, ItemStack item);

	public abstract boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire);

	public abstract void setFallingBlockHurtEntities(FallingBlock block, float damage, int max);

	public abstract void playDragonDeathEffect(Location location);

	public abstract void setClientVelocity(Player player, Vector velocity);

	public abstract void setInventoryTitle(Player player, String title);

	public abstract void startAutoSpinAttack(Player player, int ticks);

	public abstract int createFalsePlayer(Player player, boolean isSleeping, boolean cloneEquipment, Inventory inventory);

	public abstract void removeFalsePlayer(int id);
}
