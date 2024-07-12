package com.nisovin.magicspells.util.reagent;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.ExperienceUtils;


public class ExperienceReagent extends Reagent {
    private int experience;

    public ExperienceReagent(int experience) {
        this.experience = experience;
    }

    public int get() {
        return experience;
    }

    public void add(int experience) {
        this.experience += experience;
    }

    public void set(int experience) {
        this.experience = experience;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (experience > 0 && !ExperienceUtils.hasExp(player, experience)) return false;
            else return true;
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (experience != 0) {
                ExperienceUtils.changeExp(player, -experience);
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        experience = Math.round(experience * multiplier);
    }

    @Override
    public ExperienceReagent clone() {
        return new ExperienceReagent(experience);
    }

    @Override
    public String toString() {
        return "experience=" + experience;
    }
}
