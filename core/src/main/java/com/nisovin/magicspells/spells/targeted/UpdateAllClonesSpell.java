package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;

public class UpdateAllClonesSpell extends TargetedSpell {

    public UpdateAllClonesSpell(MagicConfig config, String spellName) {
        super(config, spellName);
    }

    @Override
    public PostCastAction castSpell(SpellCastState state, SpellData data) {
        MagicSpells.getVolatileCodeHandler().updateAllFalsePlayers();
        return PostCastAction.HANDLE_NORMALLY;
    }
}
