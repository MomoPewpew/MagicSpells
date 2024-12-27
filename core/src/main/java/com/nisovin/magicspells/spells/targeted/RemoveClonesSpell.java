package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.SpellData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
		}
	}

    @Override
    public PostCastAction castSpell(SpellCastState state, SpellData data) {
        if(!(data.caster() instanceof Player)) return PostCastAction.NO_MESSAGES;

        if(state == SpellCastState.NORMAL) {
				Location loc = null;
				if (pointBlank) loc = data.caster().getLocation();
				else {
					try {
						Block block = getTargetedBlock(data);
						if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation().add(0.5, 0, 0.5);
					}
					catch (IllegalStateException ignored) {}
				}

				if (loc == null) return noTarget(data);

				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data.builder().location(loc).build());
				EventUtil.call(event);
				if (event.isCancelled()) loc = null;
				else {
					loc = event.getTargetLocation();
					data = data.builder().power(event.getPower()).build();
				}

				if (loc == null) return noTarget(data);

				boolean done = removeClones(data.builder().location(loc).build());
				if (!done) return noTarget(data);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

	@Override
	public boolean castAtLocation(SpellData data) {
		return removeClones(data);
	}

    private boolean removeClones(SpellData data) {
		float radSq = radius.get(data);
		if (powerAffectsRadius) radSq *= data.power();

		radSq *= radSq;

		boolean succes = false;
		World locWorld = data.location().getWorld();

		if (!this.cloneSpells.isEmpty()) {
		    for (CloneSpell cloneSpell : cloneSpells) {
		        Iterator<Integer> iterator = cloneSpell.getTemporaryCloneMap().keySet().iterator();
		        while (iterator.hasNext()) {
		            Integer cloneID = iterator.next();
		            Location location = cloneSpell.getTemporaryCloneMap().get(cloneID);

					if (!location.getWorld().getName().equals(locWorld.getName())) continue;
			        if (location.distanceSquared(data.location()) < radSq) {
		        		MagicSpells.getVolatileCodeHandler().removeFalsePlayer(cloneID);
	    				CloneSpell.getCloneMap().remove(cloneID);
	    				iterator.remove();
		                succes = true;
		            }
		        }
		    }
		} else {
		    List<CloneSpell> cloneSpellsTemp = new ArrayList<CloneSpell>();

		    for (Spell spell : MagicSpells.spells()) {
		        if (spell instanceof CloneSpell) {
		            cloneSpellsTemp.add((CloneSpell) spell);
		        }
		    }

		    Iterator<Integer> iterator = CloneSpell.getCloneMap().keySet().iterator();
		    while (iterator.hasNext()) {
		        Integer cloneID = iterator.next();
		        Location location = CloneSpell.getCloneMap().get(cloneID);

				if (!location.getWorld().getName().equals(locWorld.getName())) continue;
		        if (location.distanceSquared(data.location()) < radSq) {
		            for (CloneSpell cloneSpell : cloneSpellsTemp) {
		                if (cloneSpell.getTemporaryCloneMap().containsKey(cloneID)) {
		                    cloneSpell.getTemporaryCloneMap().remove(cloneID);
		                    break;
		                }
		            }

	        		MagicSpells.getVolatileCodeHandler().removeFalsePlayer(cloneID);
	                iterator.remove();
                    succes = true;
		        }
		    }
		}

        return succes;
    }
}
