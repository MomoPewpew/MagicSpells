package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class CastAtMarkSpell extends InstantSpell {

	private final String markSpellName;
	private final String spellToCastName;

	private String strNoMark;

	private MarkSpell markSpell;
	private Subspell spellToCast;

	private boolean initialized = false;

	public CastAtMarkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strNoMark = getConfigString("str-no-mark", "You do not have a mark specified");
		markSpellName = getConfigString("mark-spell", "");
		spellToCastName = getConfigString("spell", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (initialized) return;

		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (!(spell instanceof MarkSpell)) {
			MagicSpells.error("CastAtMarkSpell '" + internalName + "' has an invalid mark-spell defined!");
			return;
		}
		
		markSpell = (MarkSpell) spell;
		
		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			MagicSpells.error("CastAtMarkSpell '" + internalName + "' has an invalid spell defined!");
			return;
		}
		
		initialized = true;
	}

	@Override
	public void turnOff() {
		super.turnOff();

		markSpell = null;
		spellToCast = null;
		initialized = false;
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		LivingEntity caster = data.caster();

		if (!initialized) return PostCastAction.HANDLE_NORMALLY;
		if (state == SpellCastState.NORMAL) {
			Location effectiveMark = markSpell.getEffectiveMark(caster);
			if (effectiveMark == null) {
				sendMessage(caster, strNoMark);
				return PostCastAction.HANDLE_NORMALLY;
			}
			spellToCast.subcast(data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
