package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class HasScoreboardTagCondition extends Condition {

    private boolean doReplacement;
    private String tag;

    @Override
    public boolean initialize(String var) {
        if (var == null || var.isEmpty()) return false;

        doReplacement = MagicSpells.requireReplacement(var);
        tag = var;

        return true;
    }

    @Override
    public boolean checkCaster(SpellData data) {
        if (data.caster() == null) return false;
        return checkTags(data.caster(), data.caster());
    }

    @Override
    public boolean checkTarget(SpellData data) {
        if (data.target() == null) return false;
        return checkTags(data.caster(), data.target());
    }

    @Override
    public boolean checkLocation(SpellData data) {
        return false;
    }

    private boolean checkTags(LivingEntity caster, LivingEntity target) {
        String localTag = doReplacement ? MagicSpells.doReplacements(tag, caster, target) : tag;
        return target.getScoreboardTags().contains(localTag);
    }

}
