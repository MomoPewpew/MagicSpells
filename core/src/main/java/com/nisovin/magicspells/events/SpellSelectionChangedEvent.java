package com.nisovin.magicspells.events;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.CastItem;

public class SpellSelectionChangedEvent extends SpellEvent {

	private CastItem castItem;
	private Spellbook spellbook;

	public SpellSelectionChangedEvent(Spell spell, Player caster, CastItem castItem, Spellbook spellbook) {
		super(spell, new SpellData(caster));
		this.castItem = castItem;
		this.spellbook = spellbook;
	}
	
	public CastItem getCastItem() {
		return castItem;
	}
	
	public Spellbook getSpellbook() {
		return spellbook;
	}

}
