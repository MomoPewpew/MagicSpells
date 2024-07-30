package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;

public class HungerReagent extends Reagent {
    private int hunger;

    public HungerReagent(int hunger) {
        this.hunger = hunger;
    }

    public int get() {
        return hunger;
    }

    public void add(int hunger) {
        this.hunger += hunger;
    }

    public void set(int hunger) {
        this.hunger = hunger;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (hunger > 0 && player.getFoodLevel() < hunger) return false;
            else return true;
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (hunger != 0) {
                int f = player.getFoodLevel() - hunger;
                if (f < 0) f = 0;
                if (f > 20) f = 20;
                player.setFoodLevel(f);
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        hunger = Math.round(hunger * multiplier);
    }

    @Override
    public HungerReagent clone() {
        return new HungerReagent(hunger);
    }

    @Override
    public String toString() {
        return "hunger=" + hunger;
    }
}
