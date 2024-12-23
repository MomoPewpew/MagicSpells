package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.SpellData;

public class SkinSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private String texture;
	private String signature;
	
	public SkinSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		texture = getConfigString("texture", null);
		signature = getConfigString("signature", null);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> info = getTargetedPlayer(data);
			if (info.noTarget()) return noTarget(data, info);

			data = data.builder().power(info.getPower()).build();
			Util.setSkin(info.target(), texture, signature);
			playSpellEffects(data.caster(), info.target(), data);
			sendMessages(data.caster(), info.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player) || !validTargetList.canTarget(data.caster(), data.target())) return false;
		Util.setSkin(player, texture, signature);
		playSpellEffects(data.caster(), data.target(), data);
		return true;
	}

}
