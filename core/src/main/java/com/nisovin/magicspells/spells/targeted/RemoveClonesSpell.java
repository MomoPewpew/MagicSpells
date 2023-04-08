package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.instant.MarkSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class RemoveClonesSpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Float> radius;

	private boolean pointBlank;
	private boolean powerAffectsRadius;

	private List<String> cloneSpellNames;
	private List<CloneSpell> cloneSpells;

    public RemoveClonesSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        radius = getConfigDataFloat("radius", 10F);

		pointBlank = getConfigBoolean("point-blank", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);

		cloneSpellNames = getConfigStringList("clone-spells", null);
		cloneSpells = new ArrayList<CloneSpell>();
    }

	@Override
	public void initialize() {
		super.initialize();

		if (cloneSpellNames != null) {
			for (String cloneSpellName : cloneSpellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(cloneSpellName);
				if (spell instanceof CloneSpell) {
					cloneSpells.add((CloneSpell) spell);
				} else {
					MagicSpells.error("RemoveClonesSpell '" + internalName + "' has an invalid spell defined in clone-spells!");
					return;
				}
			}
		} else {
			for (Spell spell : MagicSpells.spells()) {
				if (spell instanceof CloneSpell) {
					cloneSpells.add((CloneSpell) spell);
				}
			}
		}
	}

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        if(!(caster instanceof Player)) return PostCastAction.NO_MESSAGES;

        if(state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = caster.getLocation();
			else {
				try {
					Block block = getTargetedBlock(caster, power, args);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation().add(0.5, 0, 0.5);
				}
				catch (IllegalStateException ignored) {}
			}

			if (loc == null) return noTarget(caster, args);

			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, loc, power, args);
			EventUtil.call(event);
			if (event.isCancelled()) loc = null;
			else {
				loc = event.getTargetLocation();
				power = event.getPower();
			}

			if (loc == null) return noTarget(caster, args);

			boolean done = removeClones(caster, loc, power, args);
			if (!done) return noTarget(caster, args);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return removeClones(caster, target, power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return removeClones(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return removeClones(null, target, power, null);
	}

    private boolean removeClones(LivingEntity caster, Location loc, float power, String[] args) {
		float radSq = radius.get(caster, null, power, args);
		if (powerAffectsRadius) radSq *= power;

		radSq *= radSq;

		boolean succes = false;

		for (CloneSpell cloneSpell : cloneSpells) {
			for (Integer id : cloneSpell.getCloneMap().keySet()) {
				Location location = cloneSpell.getCloneMap().get(id);

				if (loc.distanceSquared(location) < radSq) {
    				MagicSpells.getVolatileCodeHandler().removeFalsePlayer(id);
    				succes = true;
				}
			}
		}

        return succes;
    }
}
