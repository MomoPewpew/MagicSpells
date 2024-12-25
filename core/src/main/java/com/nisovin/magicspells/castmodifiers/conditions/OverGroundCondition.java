package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class OverGroundCondition extends Condition {

	private int depth;

	@Override
	public boolean initialize(String var) {
		try {
			depth = Integer.parseInt(var);
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}

		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return overGround(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return overGround(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return overGround(data.location());
	}

	private boolean overGround(Location location) {
		Block block = location.clone().subtract(0, 1, 0).getBlock();

		Material material;
		for (int i = 0; i < depth; i++) {

			material = block.getType();
			if (material.isBlock() && material.isSolid() && material.isCollidable()) {
				return true;
			}

			block = block.getRelative(BlockFace.DOWN);
		}

		return false;
	}

}
