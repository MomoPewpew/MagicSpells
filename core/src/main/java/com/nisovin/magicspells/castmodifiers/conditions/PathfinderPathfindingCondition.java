package com.nisovin.magicspells.castmodifiers.conditions;

import de.cubbossa.pathfinder.misc.PathPlayer;
import de.cubbossa.pathfinder.navigation.NavigationModule;
import de.cubbossa.pathfinder.navigation.Navigation;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@DependsOn(plugin = "Pathfinder")
public class PathfinderPathfindingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return pathfinding(data.caster());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return pathfinding(data.target());
	}

	@Override
	public boolean checkLocation(SpellData data) {
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
