package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class OnFireCondition extends Condition {

	private final EnumSet<Material> fireTypes = EnumSet.of(Material.FIRE, Material.SOUL_FIRE);

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return onFire(data.caster(), null);
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return onFire(data.target(), null);
	}
	
	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return onFire(data.caster(), data.location());
	}

	private boolean onFire(LivingEntity target, Location location) {
		if (location != null) {
			Block b = location.getBlock();
			return fireTypes.contains(b.getType()) || fireTypes.contains(b.getRelative(BlockFace.UP).getType());
		}
		return target.getFireTicks() > 0;
	}

}
