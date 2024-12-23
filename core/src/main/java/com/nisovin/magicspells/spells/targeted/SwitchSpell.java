package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.SpellData;

public class SwitchSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Integer> switchBack;
	
	public SwitchSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		switchBack = getConfigDataInt("switch-back", 0);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);
			
			data = data.builder().power(target.getPower()).build();
			switchPlaces(data);
			sendMessages(data.caster(), target.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		switchPlaces(data);
		return true;
	}

	private void switchPlaces(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location targetLoc = target.getLocation();
		Location casterLoc = caster.getLocation();
		caster.teleportAsync(targetLoc);
		target.teleportAsync(casterLoc);

		int switchBack = this.switchBack.get(data);
		if (switchBack <= 0) return;

		playSpellEffects(caster, target, data);

		MagicSpells.scheduleDelayedTask(() -> {
			if (caster.isDead() || target.isDead()) return;
			
			Location targetLoc1 = target.getLocation();
			Location casterLoc1 = caster.getLocation();
			caster.teleportAsync(targetLoc1);
			target.teleportAsync(casterLoc1);
		}, switchBack);
	}

}
