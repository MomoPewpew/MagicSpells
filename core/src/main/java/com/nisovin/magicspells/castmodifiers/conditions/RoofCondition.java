package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.castmodifiers.Condition;

public class RoofCondition extends Condition {

	private int height = 10;
	
	@Override
	public boolean initialize(String var) {
		if (var != null && RegexUtil.matches(RegexUtil.SIMPLE_INT_PATTERN, var)) {
			height = Integer.parseInt(var);
		}
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return hasRoof(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return hasRoof(data.target().getLocation());
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		return hasRoof(data.location());
	}

	private boolean hasRoof(Location location) {
		Block b = location.clone().add(0, 2, 0).getBlock();
		for (int i = 0; i < height; i++) {
			if (!BlockUtils.isAir(b.getType())) return true;
			b = b.getRelative(BlockFace.UP);
		}
		return false;
	}

}
