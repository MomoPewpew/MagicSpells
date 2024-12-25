package com.nisovin.magicspells.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;

public class SpellTargetLocationEvent extends SpellEvent implements Cancellable {

	private boolean cancelled = false;

	public SpellTargetLocationEvent(Spell spell, SpellData data) {
		super(spell, data);
	}

	/**
	 * Gets the location that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public Location getTargetLocation() {
		return data.location();
	}

	/**
	 * Sets the spell's target to the provided location.
	 * @param target the new target
	 */
	public void setTargetLocation(Location target) {
		data.location(target);
	}

	/**
	 * Gets the current power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return data.power();
	}

	/**
	 * Sets the power level for the spell being cast.
	 * @param power the power level
	 */
	public void setPower(float power) {
		data.power(power);
	}

	/**
	 * Gets the current spell arguments.
	 * @return the spell arguments
	 */
	public String[] getSpellArgs() {
		return data.args();
	}

	/**
	 * Increases the power lever for the spell being cast by the given multiplier.
	 * @param power the power level multiplier
	 */
	public void increasePower(float power) {
		data.power(data.power() * power);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
