package com.nisovin.magicspells.events;

import java.util.Arrays;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;

public class SpellPreImpactEvent extends SpellEvent implements Cancellable {

	private Spell deliverySpell;
	private boolean redirect;
	private boolean cancelled;

	public SpellPreImpactEvent(Spell spellPayload, Spell deliverySpell, SpellData data) {
		super(spellPayload, data);
		this.data = data;
		this.deliverySpell = deliverySpell;
		redirect = false;
		cancelled = false;
		if (DebugHandler.isSpellPreImpactEventCheckEnabled()) MagicSpells.plugin.getLogger().info(toString());
	}
	
	public LivingEntity getTarget() {
		return data.target();
	}
	
	public boolean getRedirected() {
		return redirect;
	}
	
	public void setRedirected(boolean redirect) {
		this.redirect = redirect;
	}
	
	public float getPower() {
		return data.power();
	}
	
	public void setPower(float power) {
		data.power(power);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	@Override
	public String toString() {
		String casterLabel = "Caster: " + (data.caster() == null ? "null" : data.caster().toString());
		String targetLabel = "Target: " + (data.target() == null ? "null" : data.target().toString());
		String spellLabel = "SpellPayload: " + (spell == null ? "null" : spell.toString());
		String payloadSpellLabel = "Delivery Spell: " + (deliverySpell == null ? "null" : deliverySpell.toString());
		return Arrays.deepToString(new String[]{ casterLabel, targetLabel, spellLabel, payloadSpellLabel });
	}
	
}
