package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;

public class HasSpellCondition extends Condition {

    private Spell spell;

    @Override
    public boolean initialize(String var) {
        spell = MagicSpells.getSpellByInternalName(var);
        return spell != null;
    }

    @Override
    public boolean checkCaster(SpellData data) {
        if (data.caster() == null) return false;
        return hasSpell(data.caster());
    }

    @Override
    public boolean checkTarget(SpellData data) {
        if (data.target() == null) return false;
        return hasSpell(data.target());
    }

    @Override
    public boolean checkLocation(SpellData data) {
        return false;
    }

    private boolean hasSpell(LivingEntity target) {
        return target instanceof Player pl && MagicSpells.getSpellbook(pl).hasSpell(spell);
    }

}
