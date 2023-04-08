package com.nisovin.magicspells.volatilecode;

import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class VolatileCodeDisabled extends VolatileCodeHandle {

	public VolatileCodeDisabled() {
		super(null);
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, long duration) {

	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {

	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
		block.setHurtEntities(true);
	}

	@Override
	public void playDragonDeathEffect(Location location) {

	}

	@Override
	public void setClientVelocity(Player player, Vector velocity) {

	}

	@Override
	public void setInventoryTitle(Player player, String title) {

	}

	@Override
	public void startAutoSpinAttack(Player player, int ticks) {

	}

	@Override
	public int createFalsePlayer(Player player, Location location, String pose, boolean cloneEquipment) {
		return 0;
	}

	@Override
	public void removeFalsePlayer(int id) {

	}
}
