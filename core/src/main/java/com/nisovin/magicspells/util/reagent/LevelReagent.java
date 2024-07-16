package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

public class LevelReagent extends Reagent {
    private int levels;

    public LevelReagent(int levels) {
        this.levels = levels;
    }

    public int get() {
        return levels;
    }

    public void add(int levels) {
        this.levels += levels;
    }

    public void set(int levels) {
        this.levels = levels;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (levels > 0 && player.getLevel() < levels) return false;
            else return true;
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (levels != 0) {
                int lvl = player.getLevel() - levels;
                if (lvl < 0) lvl = 0;
                player.setLevel(lvl);
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        levels = Math.round(levels * multiplier);
    }

    @Override
    public LevelReagent clone() {
        return new LevelReagent(levels);
    }

    @Override
    public String toString() {
        return "levels=" + levels;
    }
}

