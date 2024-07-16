package com.nisovin.magicspells.util.reagent;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.MoneyHandler;

public class MoneyReagent extends Reagent {
    private float money;

    public MoneyReagent(float money) {
        this.money = money;
    }

    public float get() {
        return money;
    }

    public void add(float money) {
        this.money += money;
    }

    public void set(float money) {
        this.money = money;
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (money > 0) {
                MoneyHandler handler = MagicSpells.getMoneyHandler();
                if (handler == null || !handler.hasMoney(player, money)) return false;
            }
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (money != 0) {
                MoneyHandler handler = MagicSpells.getMoneyHandler();
                if (handler != null) {
                    if (money > 0) handler.removeMoney(player, money);
                    else handler.addMoney(player, money);
                }
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        money *= multiplier;
    }

    @Override
    public MoneyReagent clone() {
        return new MoneyReagent(money);
    }

    @Override
    public String toString() {
        return "money=" + money;
    }
}
