package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FoodSpell extends InstantSpell {

	private ConfigData<Integer> food;

	private ConfigData<Float> saturation;
	private ConfigData<Float> maxSaturation;
	
	public FoodSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		food = getConfigDataInt("food", 4);
		saturation = getConfigDataFloat("saturation", 2.5F);
		maxSaturation = getConfigDataFloat("max-saturation", 0F);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			int f = Math.min(player.getFoodLevel() + food.get(data), 20);
			player.setFoodLevel(f);

			float saturation = this.saturation.get(data);
			float maxSaturation = this.maxSaturation.get(data);

			float s = Math.min(player.getSaturation() + saturation, maxSaturation);
			player.setSaturation(s);

			playSpellEffects(EffectPosition.CASTER, player, data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
