package com.nisovin.magicspells.castmodifiers;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public interface IModifier {

	boolean apply(SpellCastEvent event);
	boolean apply(ManaChangeEvent event);
	boolean apply(SpellTargetEvent event);
	boolean apply(SpellTargetLocationEvent event);
	boolean apply(MagicSpellsGenericPlayerEvent event);

	ModifierResult apply(LivingEntity caster, SpellData data);
	ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data);
	ModifierResult apply(LivingEntity caster, Location target, SpellData data);

	boolean checkCaster(SpellData data);
	boolean checkTarget(SpellData data);
	boolean checkLocation(SpellData data);

}
