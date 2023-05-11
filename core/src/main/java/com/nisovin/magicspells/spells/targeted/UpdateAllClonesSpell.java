package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class UpdateAllClonesSpell extends TargetedSpell {

    public UpdateAllClonesSpell(MagicConfig config, String spellName) {
        super(config, spellName);
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        MagicSpells.getVolatileCodeHandler().updateAllFalsePlayers();
        return PostCastAction.HANDLE_NORMALLY;
    }
}
