package com.nisovin.magicspells.util.reagent;

import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.Util;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class ItemReagent extends Reagent {
    private Map<MagicItemData, Integer> itemMap;

    public ItemReagent() {
        this.itemMap = new HashMap<>();
    }

    public void add(MagicItemData magicItem, int quantity) {
        if (itemMap.containsKey(magicItem)) {
            int currentQuantity = itemMap.get(magicItem);
            itemMap.put(magicItem, currentQuantity + quantity);
        } else {
            itemMap.put(magicItem, quantity);
        }
    }

    public int get(MagicItemData magicItem) {
        return itemMap.getOrDefault(magicItem, 0);
    }

    public void set(MagicItemData magicItem, int quantity) {
        itemMap.put(magicItem, quantity);
    }

    public boolean isEmpty() {
        return itemMap.isEmpty();
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (!itemMap.isEmpty()) {
            if (livingEntity instanceof Player player) {
                Inventory inventory = player.getInventory();
                for (Map.Entry<MagicItemData, Integer> item : itemMap.entrySet()) {
                    if (item == null) continue;
                    if (InventoryUtil.inventoryContains(inventory, item)) continue;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (!itemMap.isEmpty()) {
            for (Map.Entry<MagicItemData, Integer> item : itemMap.entrySet()) {
                if (item == null) continue;
                if (livingEntity instanceof Player player) Util.removeFromInventory(player.getInventory(), item);
                else if (livingEntity.getEquipment() != null) Util.removeFromInventory(livingEntity.getEquipment(), item);
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        for (MagicItemData key : itemMap.keySet()) {
            int newQuantity = Math.round(itemMap.get(key) * multiplier);
            itemMap.put(key, newQuantity);
        }
    }

    @Override
    public ItemReagent clone() {
        ItemReagent cloned = new ItemReagent();
        for (MagicItemData key : itemMap.keySet()) {
            cloned.add(key, itemMap.get(key));
        }
        return cloned;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ItemReagent{");
        for (MagicItemData key : itemMap.keySet()) {
            sb.append(key.toString()).append("=").append(itemMap.get(key)).append(", ");
        }
        sb.delete(sb.length() - 2, sb.length()); // Remove last comma and space
        sb.append("}");
        return sb.toString();
    }
}
