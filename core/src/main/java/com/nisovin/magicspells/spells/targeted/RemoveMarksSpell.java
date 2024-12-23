package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.instant.MarkSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.SpellData;

public class RemoveMarksSpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Float> radius;

	private boolean pointBlank;
	private boolean powerAffectsRadius;

	private MarkSpell markSpell;
	private String markSpellName;

	public RemoveMarksSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataFloat("radius", 10F);

		pointBlank = getConfigBoolean("point-blank", false);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);

		markSpellName = getConfigString("mark-spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell instanceof MarkSpell) {
			markSpell = (MarkSpell) spell;
			return;
		}

		MagicSpells.error("RemoveMarksSpell '" + internalName + "' has an invalid mark-spell defined!");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = data.caster().getLocation();
			else {
				Block b = getTargetedBlock(data.caster(), data.power());
				if (b != null && !BlockUtils.isAir(b.getType())) loc = b.getLocation();
			}
			if (loc == null) return noTarget(data);
			removeMarks(data.builder().location(loc).build());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		removeMarks(data);
		return true;
	}

	private void removeMarks(SpellData data) {
		float radSq = radius.get(data);
		if (powerAffectsRadius) radSq *= data.power();

		radSq *= radSq;

		Map<UUID, MagicLocation> marks = markSpell.getMarks();
		Iterator<UUID> iter = marks.keySet().iterator();
		World locWorld = data.location().getWorld();

		while (iter.hasNext()) {
			MagicLocation l = marks.get(iter.next());
			if (!l.getWorld().equals(locWorld.getName())) continue;
			if (l.getLocation().distanceSquared(data.location()) < radSq) iter.remove();
		}

		markSpell.setMarks(marks);
		if (data.caster() != null) playSpellEffects(data.caster(), data.location(), data);
		else playSpellEffects(EffectPosition.TARGET, data.location(), data);
	}

}
