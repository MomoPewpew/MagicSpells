package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class TimeSpell extends InstantSpell implements TargetedLocationSpell {

	private ConfigData<Integer> timeToSet;

	private String strAnnounce;
		
	public TimeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		timeToSet = getConfigDataInt("time-to-set", 0);
		strAnnounce = getConfigString("str-announce", "The sun suddenly appears in the sky.");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			World world = data.caster().getWorld();
			setTime(data.caster(), world, data.power(), data.args());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		setTime(data.caster(), data.location().getWorld(), data.power(), data.args());
		return true;
	}

	private void setTime(LivingEntity caster, World world, float power, String[] args) {
		world.setTime(timeToSet.get(caster, null, power, args));
		for (Player p : world.getPlayers()) sendMessage(strAnnounce, p, args);
	}

}
