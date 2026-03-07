package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItemUpdater;

public class ItemTagSpell extends InstantSpell implements Listener {

    private final Map<MagicItemData, String> mapping = new HashMap<>();
    private final NamespacedKey nameKey = new NamespacedKey(MagicSpells.getInstance(), "magicitem");

    public ItemTagSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        ConfigurationSection mappingSection = getConfigSection("mapping");
        if (mappingSection != null) {
            for (String internalName : mappingSection.getKeys(false)) {
                ConfigurationSection itemSection = mappingSection.getConfigurationSection(internalName);
                if (itemSection == null)
                    continue;

                MagicItem magicItem = MagicItems.getMagicItemFromSection(itemSection);
                if (magicItem == null)
                    continue;

                mapping.put(magicItem.getMagicItemData(), internalName);
            }
        }
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        if (caster instanceof Player player) {
            tagInventory(player.getInventory());
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    /**
     * Event listener that checks any inventory being opened for matches.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        tagInventory(event.getInventory());
    }

    /**
     * Scans an inventory for items matching the configured mapping.
     * If a match is found without the correct 'magicitem' tag, it tags and updates
     * the item.
     */
    private void tagInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir())
                continue;

            MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
            if (itemData == null)
                continue;

            for (Map.Entry<MagicItemData, String> entry : mapping.entrySet()) {
                MagicItemData targetData = entry.getKey();
                String internalName = entry.getValue();

                if (targetData.matches(itemData)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null)
                        continue;

                    String currentName = meta.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                    if (internalName.equals(currentName))
                        continue;

                    // Update PDC
                    meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, internalName);
                    item.setItemMeta(meta);

                    // Trigger update
                    MagicItem targetMagicItem = MagicItems.getMagicItemByInternalName(internalName);
                    if (targetMagicItem != null) {
                        contents[i] = MagicItemUpdater.updateItem(item, targetMagicItem);
                        changed = true;
                    }
                    break;
                }
            }
        }

        if (changed) {
            inventory.setContents(contents);
        }
    }

}
