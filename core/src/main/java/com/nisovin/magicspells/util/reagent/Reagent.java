package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.LivingEntity;

public abstract class Reagent {
    public abstract boolean has(LivingEntity livingEntity);
    public abstract void remove(LivingEntity livingEntity);
    public abstract void multiply(float multiplier);
    public abstract Reagent clone();
    public abstract String toString();
}
