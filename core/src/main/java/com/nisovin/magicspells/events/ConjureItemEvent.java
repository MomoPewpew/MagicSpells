package com.nisovin.magicspells.events;

import javax.annotation.Nullable;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ConjureItemEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

    private final LivingEntity entity;
    private final ItemStack itemStack;

    public ConjureItemEvent(@Nullable LivingEntity entity, @NotNull ItemStack itemStack) {
        this.entity = entity;
        this.itemStack = itemStack;
    }

    @Nullable
    public LivingEntity getEntity() {
        return entity;
    }
    
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

	public static HandlerList getHandlerList() {
		return handlers;
	}
    
}
