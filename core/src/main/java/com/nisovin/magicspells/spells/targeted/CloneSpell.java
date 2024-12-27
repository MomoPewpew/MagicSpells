package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CloneSpell extends TargetedSpell implements TargetedLocationSpell {

	private static Map<Integer, Location> cloneMap = new HashMap<>();
	private Map<Integer, Location> temporaryCloneMap;

	private ConfigData<Boolean> permanent;
	private ConfigData<Boolean> cloneEquipment;
	private boolean pointBlank;

    private ConfigData<String> pose;

	private int duration;

    public CloneSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        temporaryCloneMap = new HashMap<>();

        permanent = getConfigDataBoolean("permanent", false);
        cloneEquipment = getConfigDataBoolean("clone-equipment", true);
		pointBlank = getConfigBoolean("point-blank", true);

        pose = getConfigDataString("pose", "");

		duration = getConfigInt("duration", 0);
    }

	@Override
	public void turnOff() {
		for (int cloneID : temporaryCloneMap.keySet()) {
			CloneSpell.this.removeTemporaryClone(cloneID);
		}
		temporaryCloneMap.clear();
	}

    @Override
    public PostCastAction castSpell(SpellCastState state, SpellData data) {
		LivingEntity caster = data.caster();
		float power = data.power();
		String[] args = data.args();

        if(!(caster instanceof Player)) return PostCastAction.NO_MESSAGES;

        if(state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = caster.getLocation();
			else {
				try {
					Block block = getTargetedBlock(data);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation().add(0.5, 1, 0.5);
				}
				catch (IllegalStateException ignored) {}
			}

			if (loc == null) return noTarget(data);

			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data);
			EventUtil.call(event);
			if (event.isCancelled()) loc = null;
			else {
				loc = event.getTargetLocation();
				power = event.getPower();
			}

			if (loc == null) return noTarget(data.power(data.power()));

			boolean done = createFalsePlayer(data);
			if (!done) return noTarget(data);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

	@Override
	public boolean castAtLocation(SpellData data) {
		Location location = data.location().clone();
		location.setY(data.location().getY() + 1);
		return createFalsePlayer(data.builder().location(location).build());
	}

    private boolean createFalsePlayer(SpellData data) {
        final int cloneID = MagicSpells.getVolatileCodeHandler().createFalsePlayer((Player) data.caster(), data.location(), this.pose.get(data).toUpperCase(), this.cloneEquipment.get(data));

		Location loc = data.location();
    	cloneMap.put(cloneID, loc);

        if (!this.permanent.get(data)) {
        	this.temporaryCloneMap.put(cloneID, loc);
        	if (duration > 0) {
    			MagicSpells.scheduleDelayedTask(() -> {
    				CloneSpell.this.removeTemporaryClone(cloneID);
    			}, duration);
        	}
        }

        return true;
    }

	public static Map<Integer, Location> getCloneMap() {
		return cloneMap;
	}

	public Map<Integer, Location> getTemporaryCloneMap() {
		return this.temporaryCloneMap;
	}

	private void removeTemporaryClone(int cloneID) {
		MagicSpells.getVolatileCodeHandler().removeFalsePlayer(cloneID);
		cloneMap.remove(cloneID);
		this.temporaryCloneMap.remove(cloneID);
	}
}
