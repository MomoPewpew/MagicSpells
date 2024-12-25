package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;
public class OnTeamCondition extends Condition {

	private String teamName;
	
	@Override
	public boolean initialize(String var) {
		teamName = var;
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return onTeam(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return onTeam(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean onTeam(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(pl.getName());
		return team != null && team.getName().equals(teamName);
	}

}
