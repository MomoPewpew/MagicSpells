package com.nisovin.magicspells.castmodifiers.conditions;

import de.cubbossa.pathapi.navigation.NavigationHandler;
import de.cubbossa.pathfinder.BukkitPathFinder;
import de.cubbossa.pathfinder.module.BukkitNavigationHandler;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;
import com.nisovin.magicspells.castmodifiers.Condition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@DependsOn(plugin = "Pathfinder")
public class PathfinderPathfindingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return pathfinding(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return pathfinding(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean pathfinding(LivingEntity entity) {
		if (entity instanceof Player player) {
			@Nullable NavigationHandler.SearchInfo<Player> path = BukkitNavigationHandler.getInstance().getActivePath(BukkitPathFinder.wrap(player));
			return path != null;
		}

		return false;
	}

}
