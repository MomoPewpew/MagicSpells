package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CloneSpell extends InstantSpell {

    private final ConfigData<Boolean> isSleeping;

    public CloneSpell(MagicConfig config, String spellName) {
        super(config, spellName);
        Bukkit.getLogger().info("I am created! " + spellName);
        this.isSleeping = getConfigDataBoolean("is-sleeping", false);
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        if(!(caster instanceof Player player)) return PostCastAction.NO_MESSAGES;

        if(state == SpellCastState.NORMAL) createClone(player, power, args);
        return PostCastAction.HANDLE_NORMALLY;
    }


    private void createClone(Player player, float power, String[] args){
        MagicSpells.getVolatileCodeHandler().createFalsePlayer(player, isSleeping.get(player, null, power, args));
    }
}
