package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;

import net.sneakycharactermanager.paper.handlers.character.LoadCharacterEvent;

@DependsOn(plugin = "SneakyCharacterManager")
public class LoadCharacterListener extends PassiveListener {
    
    Boolean firstLoad;

    @Override
    public void initialize(String var) {
        if (var != null) {
            if (var.equalsIgnoreCase("true")) {
                firstLoad = true;
                return;
            } else if (var.equalsIgnoreCase("false")) {
                firstLoad = false;
                return;
            } else {
                MagicSpells.error("Passive trigger 'loadcharacter " + var + "' has a TriggerVar but it isn't true/false. It will be ignored.");
            }
        }
        firstLoad = null;
    }
    
    @OverridePriority
	@EventHandler
	public void onCharacterLoad(LoadCharacterEvent event) {
        if (firstLoad != null && firstLoad != event.getFirstLoad()) return;
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!hasSpell(event.getPlayer()) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
    }

}
