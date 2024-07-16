package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaChangeReason;


public class ManaReagent extends Reagent {
    private int mana;

    public ManaReagent(int mana) {
        this.mana = mana;
    }

    public int get() {
        return mana;
    }

    public void add(int mana) {
        this.mana += mana;
    }

    public void set(int mana) {
        this.mana = mana;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (mana > 0 && (MagicSpells.getManaHandler() == null || !MagicSpells.getManaHandler().hasMana(player, mana))) return false;
            else return true;
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (mana != 0) MagicSpells.getManaHandler().addMana(player, -mana, ManaChangeReason.SPELL_COST);
        }
    }

    @Override
    public void multiply(float multiplier) {
        mana = Math.round(mana * multiplier);
    }

    @Override
    public ManaReagent clone() {
        return new ManaReagent(mana);
    }

    @Override
    public String toString() {
        return "mana=" + mana;
    }
}