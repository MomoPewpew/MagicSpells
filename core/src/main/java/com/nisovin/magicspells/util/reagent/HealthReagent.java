package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.Util;
import org.bukkit.attribute.Attribute;

public class HealthReagent extends Reagent {
    private double health;

    public HealthReagent(double health) {
        this.health = health;
    }

    public double get() {
        return health;
    }

    public void add(double health) {
        this.health += health;
    }

    public void set(double health) {
        this.health = health;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        double totalHealth = livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        return totalHealth > health;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (health != 0) {
            double absorption = livingEntity.getAbsorptionAmount();
            double healthToRemove = health;

            if (absorption > 0) {
                if (absorption >= healthToRemove) {
                    livingEntity.setAbsorptionAmount(absorption - healthToRemove);
                    healthToRemove = 0;
                } else {
                    healthToRemove -= absorption;
                    livingEntity.setAbsorptionAmount(0);
                }
            }

            if (healthToRemove > 0) {
                double currentHealth = livingEntity.getHealth();
                double newHealth = currentHealth - healthToRemove;
                if (newHealth < 0) newHealth = 0;
                livingEntity.setHealth(newHealth);
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        health = health * multiplier;
    }

    @Override
    public HealthReagent clone() {
        return new HealthReagent(health);
    }

    @Override
    public String toString() {
        return "health=" + health;
    }
}
