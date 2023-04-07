package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CloneSpell extends InstantSpell {

	String[] poseNames = {"CROAKING", "DIGGING", "DYING", "EMERGING", "FALL_FLYING", "LONG_JUMPING", "ROARING", "SITTING", "SLEEPING", "SNEAKING", "SNIFFING", "SPIN_ATTACK", "STANDING", "SWIMMING", "USING_TONGUE"};

	private List<Integer> cloneIDs;

    private final boolean permanent;
    private final boolean cloneEquipment;

    private String pose;

	private int duration;

    public CloneSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        cloneIDs = new ArrayList<>();

        permanent = getConfigBoolean("permanent", false);
        cloneEquipment = getConfigBoolean("clone-equipment", true);

        pose = getConfigString("pose", "").toUpperCase();
        if (!Arrays.asList(poseNames).contains(pose)) {
        	pose = "";
        }

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
            int cloneID = MagicSpells.getVolatileCodeHandler().createFalsePlayer(player, pose, cloneEquipment);

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
