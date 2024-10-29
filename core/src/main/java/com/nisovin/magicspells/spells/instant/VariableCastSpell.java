package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class VariableCastSpell extends InstantSpell {

	private String variableName;
	private String strDoesntContainSpell;

	public VariableCastSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		variableName = getConfigString("variable-name", null);
		strDoesntContainSpell = getConfigString("str-doesnt-contain-spell", "You do not have a valid spell in memory");
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		if (MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("VariableCastSpell '" + internalName + "' has an invalid variable-name defined!");
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			if (variableName == null) return PostCastAction.HANDLE_NORMALLY;
			String value = MagicSpells.getVariableManager().getVariable(variableName).getStringValue(player);

			Spell toCast = MagicSpells.getSpellByInternalName(value);
			if (toCast == null) {
				sendMessage(player, strDoesntContainSpell);
				return PostCastAction.NO_MESSAGES;
			}
			toCast.cast(data.builder().build());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

}
