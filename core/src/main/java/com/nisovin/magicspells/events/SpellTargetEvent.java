package com.nisovin.magicspells.events;

import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;

/**
 * This event is fired whenever a TargetedSpell is trying to target an entity.
 * Cancelling this event will prevent the spell from targeting the entity.
 *
 */
public class SpellTargetEvent extends SpellEvent implements Cancellable {

	private boolean cancelled = false;
	private boolean castCancelled = false;

	public SpellTargetEvent(Spell spell, SpellData data) {
		super(spell, data);
	}

	public SpellTargetEvent(Spell spell, SpellData spellData, LivingEntity target) {
		this(spell, spellData.builder().target(target).build());
	}

	/**
	 * Gets the living entity that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public LivingEntity getTarget() {
		return data.target();
	}
	
	/**
	 * Sets the spell's target to the provided living entity.
	 * @param target the new target
	 */
	public void setTarget(LivingEntity target) {
		data = data.target(target);
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
		data = data.power(power);
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
		data = data.power(data.power() * power);
	}

	/**
	 * Gets the spell data for the associated cast.
	 * @return the spell data
	 */
	public SpellData getSpellData() {
		return data;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;		
	}

	public boolean isCastCancelled() {
		return castCancelled;
	}

	public void setCastCancelled(boolean castCancelled) {
		this.castCancelled = castCancelled;
		cancelled = true;
	}

}