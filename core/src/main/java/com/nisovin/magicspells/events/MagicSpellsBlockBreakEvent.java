package com.nisovin.magicspells.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class MagicSpellsBlockBreakEvent extends BlockBreakEvent implements IMagicSpellsCompatEvent, IDippGenBypass {

	private boolean bypassDippGen = false;

	public MagicSpellsBlockBreakEvent(Block theBlock, Player player) {
		super(theBlock, player);
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
