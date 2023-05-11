package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTeleportEvent;

import java.util.Collection;
import java.util.Objects;

public class CloneMoveListener extends PassiveListener {
    @Override
    public void initialize(String var) {

    }


    @OverridePriority
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event){
        if(event.getEntity() instanceof Display displayEntity){
            if(MagicSpells.getVolatileCodeHandler().isRelatedToFalsePlayer(displayEntity)){
                /*
                    Magic Spells REQUIRES a LivingEntity to cast spells..
                    Since a DisplayEntity is NOT considered a Living entity.. I needed to work around this.
                    My Solution: Just find any nearby entites either where they left or where they end up
                    there HAS to be one somewhere!
                */
                Collection<LivingEntity> nearbyEntities = displayEntity.getLocation().getNearbyLivingEntities(4);
                if(nearbyEntities.size() == 0)
                    nearbyEntities = Objects.requireNonNull(event.getTo()).getNearbyLivingEntities(4);
                if(nearbyEntities.size() > 0){
                    passiveSpell.activate(nearbyEntities.stream().findFirst().get(), null, event.getTo());
                    //Just pull any entity and cast the spell using them?
                }

            }
        }

    }
}
