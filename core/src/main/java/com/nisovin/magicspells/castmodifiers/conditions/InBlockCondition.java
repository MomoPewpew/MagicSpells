package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class InBlockCondition extends Condition {

	private Set<BlockData> blockDataSet;
	private BlockData blockData;

	@Override
	public boolean initialize(String var) {
		String[] split = var.split(",(?![^\\[]*])");

		if (split.length > 1) {
			blockDataSet = new HashSet<>();

			for (String s : split) {
				BlockData data;
				try {
					data = Bukkit.createBlockData(s.trim().toLowerCase());
				} catch (IllegalArgumentException e) {
					return false;
				}

				if (!data.getMaterial().isBlock()) return false;

				blockDataSet.add(data);
			}

			return true;
		}

		try {
			blockData = Bukkit.createBlockData(var.trim().toLowerCase());
		} catch (IllegalArgumentException e) {
			return false;
		}

		return blockData.getMaterial().isBlock();
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return inBlock(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return inBlock(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return inBlock(data.location());
	}

	private boolean inBlock(Location location) {
		BlockData bd = location.getBlock().getBlockData();
		if (blockData != null) return bd.matches(blockData);

		for (BlockData data : blockDataSet)
			if (bd.matches(data))
				return true;

		return false;
	}

}
