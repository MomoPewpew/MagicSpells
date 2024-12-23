package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class EnderchestSpell extends InstantSpell implements TargetedEntitySpell {

	public EnderchestSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			String[] args = data.args();
			if (args != null && args.length == 1 && player.hasPermission("magicspells.advanced." + internalName)) {
				Player target = PlayerNameUtils.getPlayer(args[0]);
				if (target == null) {
					player.sendMessage("Invalid player target");
					return PostCastAction.ALREADY_HANDLED;
				}
				player.openInventory(target.getEnderChest());
			} else player.openInventory(player.getEnderChest());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		
		if (!(data.caster() instanceof Player playerCaster) || !(data.target() instanceof Player playerTarget)) return false;
		playerCaster.openInventory(playerTarget.getEnderChest());
		
		return true;
	}

}
