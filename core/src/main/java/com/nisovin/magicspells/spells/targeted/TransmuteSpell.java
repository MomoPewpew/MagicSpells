package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.util.SpellData;

public class TransmuteSpell extends TargetedSpell implements TargetedLocationSpell {

	private List<Material> blockTypes;

	private Material transmuteType;
	
	public TransmuteSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> list = getConfigStringList("transmutable-types", null);
		blockTypes = new ArrayList<>();
		if (list != null && !list.isEmpty()) {
			for (String s : list) {
				Material material = Util.getMaterial(s);
				if (material == null || !material.isBlock()) continue;
				blockTypes.add(material);
			}
		} else blockTypes.add(Material.IRON_BLOCK);

		String materialName = getConfigString("transmute-type", "gold_block");
		transmuteType = Util.getMaterial(materialName);
		if (transmuteType == null || !transmuteType.isBlock()) {
			MagicSpells.error("TransmuteSpell '" + internalName + "' has an transmute-type defined!");
			transmuteType = null;
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Block block = getTargetedBlock(data.caster(), data.power(), data.args());
			if (block == null) return noTarget(data);
			
			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data);
			EventUtil.call(event);
			if (event.isCancelled()) return noTarget(data);
			block = event.getTargetLocation().getBlock();
			
			if (!canTransmute(block)) return noTarget(data);

			block.setType(transmuteType);
			playSpellEffects(data.caster(), block.getLocation().add(0.5, 0.5, 0.5), data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		Block block = data.location().getBlock();
		if (canTransmute(block)) {
			block.setType(transmuteType);
			playSpellEffects(data.caster(), block.getLocation().add(0.5, 0.5, 0.5), data);
			return true;
		}

		Vector v = data.location().getDirection();
		block = data.location().clone().add(v).getBlock();
		if (canTransmute(block)) {
			block.setType(transmuteType);
			playSpellEffects(data.caster(), block.getLocation().add(0.5, 0.5, 0.5), data);
			return true;
		}
		return false;
	}

	private boolean canTransmute(Block block) {
		for (Material m : blockTypes) {
			if (m.equals(block.getType())) return true;
		}
		return false;
	}

}
