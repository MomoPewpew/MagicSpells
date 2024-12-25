package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

// TODO this should be refactored along with the other 'has item' related conditions to reduce redundant code
public class HoldingPreciseCondition extends Condition {

	private MagicItemData itemData = null;
	
	@Override
	public boolean initialize(String var) {
		itemData = MagicItems.getMagicItemDataFromString(var);
		return itemData != null;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return checkHolding(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return checkHolding(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}
	
	private boolean checkHolding(LivingEntity target) {
		EntityEquipment equipment = target.getEquipment();
		if (equipment == null) return false;

		ItemStack item = equipment.getItemInMainHand();
		MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
		if (data == null) return false;

		return itemData.matches(data);
	}

}
