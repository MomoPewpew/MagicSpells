package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.LivingEntity;

public abstract class Reagent {
    public abstract boolean has(LivingEntity livingEntity);
    public abstract void remove(LivingEntity livingEntity);
    public abstract void multiply(float multiplier);
    public abstract Reagent clone();
    public abstract String toString();
    
    /**
     * Gets the current value of this reagent.
     * @return the value as a Number (can be Integer, Double, or Float depending on the reagent type)
     */
    public abstract Number get();
}
