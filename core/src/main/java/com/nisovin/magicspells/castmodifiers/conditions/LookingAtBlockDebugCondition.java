package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.conditions.LookingAtBlockCondition;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.util.BlockUtils;

public class LookingAtBlockDebugCondition extends LookingAtBlockCondition {
	@Override
	public boolean check(Player player, LivingEntity target) {
		Block block = BlockUtils.getTargetBlock(null, target, dist);
		MaterialData data = block.getState().getData();
		Material type = data.getItemType();
		if(type == Material.LOG || type == Material.SAPLING || type == Material.WOOD || type == Material.LEAVES || type == Material.WOOL) {
			MagicSpells.sendDebugMessage("MagicSpells has a custom syntax for specifying the block data of a block of type '" + type.name().toLowerCase() + "'; Consult 'MagicItemNameResolver.java' if you are unsure of how to use this syntax");
		} else {
			MagicSpells.sendDebugMessage(type.name().toLowerCase() + ":" + data.getData());
		}
		return blockType.equals(block);
	}
}
