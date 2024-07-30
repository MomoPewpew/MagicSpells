package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;

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
        if (health > 0 && livingEntity.getHealth() <= health) return false;
        else return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (health != 0) {
            double h = livingEntity.getHealth() - health;
            if (h < 0) h = 0;
            if (h > Util.getMaxHealth(livingEntity)) h = Util.getMaxHealth(livingEntity);
            livingEntity.setHealth(h);
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
