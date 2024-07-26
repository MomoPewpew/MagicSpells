package com.nisovin.magicspells.variables.meta;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class AttributeBaseValueVariable extends MetaVariable {

	private final Attribute attribute;

	public AttributeBaseValueVariable(Attribute attribute) {
		super();

		this.attribute = attribute;
	}

	@Override
	public double getValue(String player) {
		Player p = Bukkit.getPlayerExact(player);
		if (p == null) return 0D;

		AttributeInstance inst = p.getAttribute(attribute);
		if (inst == null) return 0D;

		return inst.getBaseValue();
	}

	@Override
	public void set(String player, double amount) {
		Player p = Bukkit.getPlayerExact(player);
		if (p == null) return;

		AttributeInstance inst = p.getAttribute(attribute);
		if (inst == null) return;

		inst.setBaseValue(amount);
	}
}