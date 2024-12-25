package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.SpellData;

public class OffHandPreciseCondition extends Condition {

	private MagicItemData itemData = null;
	
	@Override
	public boolean initialize(String var) {
		itemData = MagicItems.getMagicItemDataFromString(var);
		return itemData != null;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return offHand(data.caster());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return offHand(data.target());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}
	
	private boolean offHand(LivingEntity target) {
		EntityEquipment equipment = target.getEquipment();
		if (equipment == null) return false;
		ItemStack item = equipment.getItemInOffHand();

		MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
		if (data == null) return false;

		return itemData.matches(data);
	}

}
