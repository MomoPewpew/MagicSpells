package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CloneSpell extends InstantSpell {

	private List<Integer> cloneIDs;

    private final boolean sleeping;
    private final boolean permanent;
    private final boolean cloneEquipment;

	private int duration;

    public CloneSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        cloneIDs = new ArrayList<>();

        this.sleeping = getConfigBoolean("sleeping", false);
        this.permanent = getConfigBoolean("permanent", false);
        this.cloneEquipment = getConfigBoolean("clone-equipment", true);

		duration = getConfigInt("duration", 0);
    }

	@Override
	public void turnOff() {
		for (int cloneID : cloneIDs) {
			MagicSpells.getVolatileCodeHandler().removeFalsePlayer(cloneID);
		}
		cloneIDs.clear();
	}

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        if(!(caster instanceof Player player)) return PostCastAction.NO_MESSAGES;

        if(state == SpellCastState.NORMAL) {
            int cloneID = MagicSpells.getVolatileCodeHandler().createFalsePlayer(player, sleeping, cloneEquipment);

            if (!permanent) {
            	cloneIDs.add(cloneID);

            	if (duration > 0) {
        			MagicSpells.scheduleDelayedTask(() -> {
        				MagicSpells.getVolatileCodeHandler().removeFalsePlayer(cloneID);
        				cloneIDs.remove(cloneID);
        			}, duration);
            	}
            }
        }
        return PostCastAction.HANDLE_NORMALLY;
    }
}
