package com.nisovin.magicspells.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.BlockPlaceEvent;

public class MagicSpellsBlockPlaceEvent extends BlockPlaceEvent implements IMagicSpellsCompatEvent, IDippGenBypass {

	private boolean bypassDippGen = false;

	public MagicSpellsBlockPlaceEvent(Block placedBlock, BlockState replacedBlockState, Block placedAgainst, ItemStack itemInHand, Player thePlayer, boolean canBuild, boolean bypassDippGen) {
		super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, EquipmentSlot.HAND);
		this.bypassDippGen = bypassDippGen;
	}

	public MagicSpellsBlockPlaceEvent(Block placedBlock, BlockState replacedBlockState, Block placedAgainst, ItemStack itemInHand, Player thePlayer, boolean canBuild, EquipmentSlot equipmentSlot, boolean bypassDippGen) {
		super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, equipmentSlot);
		this.bypassDippGen = bypassDippGen;
	}

	@Override
	public boolean getBypassDippGen() {
		return this.bypassDippGen;
	}

	@Override
	public void setBypassDippGenField(boolean bypass) {
		this.bypassDippGen = bypass;
	}

}
