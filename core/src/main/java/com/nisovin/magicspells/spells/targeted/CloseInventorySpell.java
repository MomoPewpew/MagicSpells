package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CloseInventorySpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> delay;

	public CloseInventorySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		delay = getConfigDataInt("delay", 0);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);
			Player target = targetInfo.target();

			close(data.builder().target(target).power(targetInfo.getPower()).build());
			sendMessages(data.caster(), target, data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player) || !validTargetList.canTarget(data.caster(), data.target())) return false;
		close(data);
		return true;
	}

	private void close(SpellData data) {
		LivingEntity caster = data.caster();
		Player target = (Player) data.target();

		int delay = this.delay.get(data);

		if (delay > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
				target.closeInventory();

				if (caster != null) playSpellEffects(data);
				else playSpellEffects(EffectPosition.TARGET, data.caster(), data);
			}, delay);
		}
		else {
			target.closeInventory();

			if (caster != null) playSpellEffects(data);
			else playSpellEffects(EffectPosition.TARGET, target, data);
		}
	}

}
