package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.Condition;

public class LookingAtBlockCondition extends Condition {

	private BlockData blockData;
	private int dist = 4;

	@Override
	public boolean initialize(String var) {
		String[] split = var.split(",(?![^\\[]*])");

		try {
			blockData = Bukkit.createBlockData(split[0].trim().toLowerCase());
		} catch (IllegalArgumentException e) {
			return false;
		}

		if (!blockData.getMaterial().isBlock()) return false;

		if (split.length > 1) {
			try {
				dist = Integer.parseInt(split[1].trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return lookingAt(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return lookingAt(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean lookingAt(LivingEntity target) {
		Block block = BlockUtils.getTargetBlock(null, target, dist);
		if (block == null) return false;

		return block.getBlockData().matches(blockData);
	}

}
