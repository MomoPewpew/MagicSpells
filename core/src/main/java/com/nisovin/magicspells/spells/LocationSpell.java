package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.ConfigData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;


public class LocationSpell extends TargetedSpell  {

	@ConfigData(field="location", dataType="String", defaultValue="world,0,0,0")
	MagicLocation location;

	@ConfigData(field="spell", dataType="String", defaultValue="")
	Subspell spell;

	public LocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String s = getConfigString("location", "world,0,0,0");
		try {
			String[] split = s.split(",");
			String world = split[0];
			double x = Double.parseDouble(split[1]);
			double y = Double.parseDouble(split[2]);
			double z = Double.parseDouble(split[3]);
			float yaw = 0;
			float pitch = 0;
			if (split.length > 4) yaw = Float.parseFloat(split[4]);
			if (split.length > 5) pitch = Float.parseFloat(split[5]);
			this.location = new MagicLocation(world, x, y, z, yaw, pitch);
		} catch (Exception e) {
			MagicSpells.error("Invalid location on LocationSpell '" + spellName + '\'');
		}
		this.spell = new Subspell(getConfigString("spell", ""));
	}

	@Override
	public void initialize() {
		super.initialize();
		if (this.spell != null) {
			boolean ok = this.spell.process();
			if (!ok || !this.spell.isTargetedLocationSpell()) {
				MagicSpells.error("Invalid spell on LocationSpell '" + this.name + '\'');
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = this.location.getLocation();
			if (loc == null) return noTarget(player);

			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, loc, power);
			EventUtil.call(event);
			if (event.isCancelled()) return noTarget(player);
			loc = event.getTargetLocation();
			power = event.getPower();

			this.spell.castAtLocation(player, loc, power);
			playSpellEffects(player, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
