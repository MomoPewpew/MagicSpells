package com.nisovin.magicspells.spells;

import java.util.List;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PermissionSpell extends InstantSpell {

	private ConfigData<Integer> duration;
	
	private List<String> permissionNodes;
	
	public PermissionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		duration = getConfigDataInt("duration", 0);
		permissionNodes = getConfigStringList("permission-nodes", null);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && permissionNodes != null) {
			int duration = this.duration.get(data);
			if (duration <= 0) return PostCastAction.HANDLE_NORMALLY;

			for (String node : permissionNodes) {
                assert data.caster() != null;
                data.caster().addAttachment(MagicSpells.plugin, node, true, duration);
			}

			playSpellEffects(EffectPosition.CASTER, data.caster(), data.power(), data.args());
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

}
