package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.handlers.DebugHandler;

public class ChestContainsCondition extends Condition {

	//world,x,y,z,item

	private MagicLocation location;

	private MagicItemData itemData;

	@Override
	public boolean initialize(String var) {
		try {
			String[] vars = var.split(",");
			location = new MagicLocation(vars[0], Integer.parseInt(vars[1]), Integer.parseInt(vars[2]), Integer.parseInt(vars[3]));

			itemData = MagicItems.getMagicItemDataFromString(vars[4].trim());
			return itemData != null;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return checkChest();
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return checkChest();
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return checkChest();
	}

	private boolean checkChest() {
		Block block = location.getLocation().getBlock();
		if (!BlockUtils.isChest(block)) return false;

		Chest chest = (Chest) block.getState();
		ItemStack[] items = chest.getInventory().getContents();
		if (items.length == 0) return false;

		for (ItemStack item : items) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
			if (data == null) continue;
			if (itemData.matches(data)) return true;
		}

		return false;
	}

}
