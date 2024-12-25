package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.SpellData;

// Only accepts magic items and uses a much stricter match
public class HasItemPreciseCondition extends Condition {

	private MagicItemData itemData = null;
	
	@Override
	public boolean initialize(String var) {
		itemData = MagicItems.getMagicItemDataFromString(var);
		return itemData != null;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		if (data.caster() instanceof InventoryHolder holder) return checkInventory(holder.getInventory());
		else return checkEquipment(data.caster().getEquipment());
	}
	
	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		if (data.target() instanceof InventoryHolder holder) return checkInventory(holder.getInventory());
		else return checkEquipment(data.target().getEquipment());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		BlockState targetState = data.location().getBlock().getState();
		return targetState instanceof InventoryHolder holder && checkInventory(holder.getInventory());
	}

	private boolean checkInventory(Inventory inventory) {
		if (inventory == null) return false;

		for (ItemStack itemStack : inventory.getContents()) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(itemStack);
			if (data == null) continue;
			if (itemData.matches(data)) return true;
		}

		return false;
	}

	private boolean checkEquipment(EntityEquipment entityEquipment) {
		if (entityEquipment == null) return false;

		for (ItemStack itemStack : InventoryUtil.getEquipmentItems(entityEquipment)) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(itemStack);
			if (data == null) continue;
			if (itemData.matches(data)) return true;
		}

		return false;
	}

}
