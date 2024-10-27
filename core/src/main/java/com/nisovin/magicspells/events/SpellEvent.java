package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;

public abstract class SpellEvent extends Event implements IMagicSpellsCompatEvent {

	protected static final HandlerList handlers = new HandlerList();

	protected Spell spell;

	protected SpellData data;
	
	public SpellEvent(Spell spell, SpellData data) {
		this.spell = spell;
		this.data = data;
	}
	
	/**
	 * Gets the spell involved in the event.
	 * @return the spell
	 */
	public Spell getSpell() {
		return spell;
	}
	
	/**
	 * Gets the player casting the spell.
	 * @return the casting player
	 */
	public LivingEntity getCaster() {
		return data.caster();
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
}
