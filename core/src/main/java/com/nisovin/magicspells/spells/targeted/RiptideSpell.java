package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.SpellData;

public class RiptideSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Integer> duration;

	public RiptideSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 40);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> info = getTargetedPlayer(data);
			if (info.noTarget()) return noTarget(data, info);

			Player target = info.target();
			data = data.builder().power(info.getPower()).build();

			MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(target, duration.get(data));
			playSpellEffects(data.caster(), target, data);
			sendMessages(data.caster(), target, data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player) || !validTargetList.canTarget(data.caster(), data.target())) return false;

		MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(player, duration.get(data));
		playSpellEffects(data.caster(), data.target(), data);

		return true;
	}

}
