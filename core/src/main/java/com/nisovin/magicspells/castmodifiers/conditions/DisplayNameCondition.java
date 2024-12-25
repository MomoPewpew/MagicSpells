package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class DisplayNameCondition extends Condition {

    private String displayName;

    @Override
    public boolean initialize(String var) {
        if (var == null || var.isEmpty()) return false;
        displayName = Util.getStringFromComponent(Util.getMiniMessage(var));
        return true;
    }

    @Override
    public boolean checkCaster(SpellData data) {
        return checkName(data.caster());
    }

    @Override
    public boolean checkTarget(SpellData data) {
        return checkName(data.target());
    }

    @Override
    public boolean checkLocation(SpellData data) {
        return false;
    }

    private boolean checkName(LivingEntity target) {
        if (!(target instanceof Player pl)) return false;
        return Util.getStringFromComponent(pl.displayName()).equals(displayName);
    }

}
