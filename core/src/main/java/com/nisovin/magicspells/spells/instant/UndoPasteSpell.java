package com.nisovin.magicspells.spells.instant;

import com.comphenix.protocol.PacketType;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.targeted.PasteSpell;
import com.nisovin.magicspells.util.MagicConfig;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class UndoPasteSpell extends InstantSpell {


    private boolean ignoreOtherPlayers;
    private String pasteSpellName;

    public UndoPasteSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        ignoreOtherPlayers = getConfigBoolean("ignore-other-players", false);
        pasteSpellName = getConfigString("paste-name", "");
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {

        if(state == SpellCastState.NORMAL){

            if(ignoreOtherPlayers){
                PasteSpell.undoPlayerPastes(pasteSpellName, caster.getUniqueId().toString());
            }else{
                PasteSpell.undoPastes(pasteSpellName);
            }

        }

        return PostCastAction.HANDLE_NORMALLY;
    }
}
