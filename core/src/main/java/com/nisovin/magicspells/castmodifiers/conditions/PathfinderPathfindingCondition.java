package com.nisovin.magicspells.castmodifiers.conditions;

import de.cubbossa.pathfinder.misc.PathPlayer;
import de.cubbossa.pathfinder.navigation.NavigationModule;
import de.cubbossa.pathfinder.navigation.Navigation;
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
			@Nullable Navigation<Object> path = NavigationModule.get().getActiveFindCommandPath(PathPlayer.wrap(player));
			return path != null;
		}

		return false;
	}

}
