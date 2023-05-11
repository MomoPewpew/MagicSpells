package com.nisovin.magicspells.spells.passive;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Collection;
import java.util.Objects;

public class CloneKillListener extends PassiveListener {


    @Override
    public void initialize(String var) {

    }


    @OverridePriority
    @EventHandler
    public void onDeath(EntityRemoveFromWorldEvent event){
        if(event.getEntity() instanceof Display displayEntity && MagicSpells.getVolatileCodeHandler().isRelatedToFalsePlayer(displayEntity)){
            //Using larger radius for situations where this would actually be triggered :3
            Collection<LivingEntity> nearbyEntities = displayEntity.getLocation().getNearbyLivingEntities(20);
            Bukkit.getLogger().info("Found: " + nearbyEntities.size());
            if(nearbyEntities.size() > 0)   passiveSpell.activate(nearbyEntities.stream().findFirst().get());
        }
    }

}
