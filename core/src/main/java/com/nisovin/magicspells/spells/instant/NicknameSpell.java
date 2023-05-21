package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class NicknameSpell extends InstantSpell {


    private HashMap<String, String> nameStorage = new HashMap<>();

    public NicknameSpell(MagicConfig config, String spellName) {
        super(config, spellName);
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {

        if(!(caster instanceof Player player)) return PostCastAction.NO_MESSAGES;

        if(args == null){
            player.sendMessage(Component.text("You have been UnNicked!"));
            if(nameStorage.containsKey(player.getUniqueId().toString()))
                MagicSpells.getVolatileCodeHandler().nicknamePlayer(player, nameStorage.get(player.getUniqueId().toString()));

        }else{
            StringBuilder builder = new StringBuilder();
            for(String namePart : args){
                builder.append(namePart).append(" ");
            }

            if(builder.length() > 16){
                caster.sendMessage(Component.text(ChatColor.RED + "You can only have a Max of 16 Characters!"));
                return PostCastAction.ALREADY_HANDLED;
            }

            if(nameStorage.containsKey(player.getUniqueId().toString()))
                MagicSpells.getVolatileCodeHandler().nicknamePlayer(player, builder.substring(0, builder.length()-1)); //If the name is in there, don't overwrite it!
            else
                nameStorage.put(player.getUniqueId().toString(), MagicSpells.getVolatileCodeHandler().nicknamePlayer(player, builder.substring(0, builder.length()-1)));
        }

        return PostCastAction.ALREADY_HANDLED;
    }


}
