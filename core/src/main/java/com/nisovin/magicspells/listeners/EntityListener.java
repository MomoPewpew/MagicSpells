package com.nisovin.magicspells.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.nisovin.magicspells.spelleffects.effecttypes.EntityEffect;

public class EntityListener implements Listener {
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getScoreboardTags().contains(EntityEffect.ENTITY_TAG)) {
                String expirationTimeMillisTag = entity.getScoreboardTags().stream()
                    .filter(tag -> tag.startsWith(EntityEffect.EXPIRATION_TIME_MILLIS_TAG + ":"))
                    .findFirst()
                    .orElse(null);
                if (expirationTimeMillisTag == null) continue;
                try {
                    int expirationTimeMillis = Integer.parseInt(expirationTimeMillisTag.split(":")[1]);
                    if (System.currentTimeMillis() > expirationTimeMillis) {
                        entity.remove();
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
    }
}
