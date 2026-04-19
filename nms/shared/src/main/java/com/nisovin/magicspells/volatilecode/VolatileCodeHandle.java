package com.nisovin.magicspells.volatilecode;

import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;
import net.kyori.adventure.text.Component;

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

	public abstract int createFalsePlayer(Player player, Location location, String pose, boolean cloneEquipment);

	public abstract void removeFalsePlayer(int id);

	public abstract boolean isRelatedToFalsePlayer(Display entityDisplay);

	public abstract void updateFalsePlayer(Display entityDisplay);

	public abstract void updateAllFalsePlayers();

	public abstract void playHurtAnimation(LivingEntity entity, float yaw);

	public abstract void sendToastEffect(Player receiver, ItemStack icon, Frame frameType, Component text);

	/**
	 * Spawn a client-side-only dropped item (packet-only) for nearby players.
	 * <p>
	 * Implementations should not add an entity to the world; the item must be unpickable.
	 *
	 * @throws UnsupportedOperationException if not supported by this volatile handler
	 */
	public void spawnFakeItemSpray(Location location, ItemStack item, Vector velocity, int lifetimeTicks, double viewDistance) {
		throw new UnsupportedOperationException("Fake item spray not supported by this volatile handler.");
	}
}
