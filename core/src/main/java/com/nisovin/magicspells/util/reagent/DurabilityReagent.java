package com.nisovin.magicspells.util.reagent;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.Damageable;

public class DurabilityReagent extends Reagent {
    private int durability;

    public DurabilityReagent(int durability) {
        this.durability = durability;
    }

    public int get() {
        return durability;
    }

    public void add(int durability) {
        this.durability += durability;
    }

    public void set(int durability) {
        this.durability = durability;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (durability > 0) {
            // Durability cost is charged from the main hand item
            EntityEquipment equipment = livingEntity.getEquipment();
            if (equipment == null) return false;
            ItemStack inHand = equipment.getItemInMainHand();
            if (!(inHand.getItemMeta() instanceof Damageable damageable)) return false;
            if (damageable.getDamage() >= inHand.getType().getMaxDurability()) return false;
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (durability != 0) {
            EntityEquipment eq = livingEntity.getEquipment();

            if (eq != null) {
                ItemStack item =  eq.getItemInMainHand();
                ItemMeta meta = item.getItemMeta();

                int maxDurability = item.getType().getMaxDurability();
                if (maxDurability > 0 && meta instanceof Damageable damageable) {
                    int damage = damageable.getDamage() + durability;
                    damage = AccurateMath.max(AccurateMath.min(damage, maxDurability), 0);

                    damageable.setDamage(damage);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        durability = Math.round(durability * multiplier);
    }

    @Override
    public DurabilityReagent clone() {
        return new DurabilityReagent(durability);
    }

    @Override
    public String toString() {
        return "durability=" + durability;
    }
}
