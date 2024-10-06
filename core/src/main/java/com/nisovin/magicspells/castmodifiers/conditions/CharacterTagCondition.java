package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;

import net.sneakycharactermanager.paper.handlers.character.Character;

@DependsOn(plugin = "SneakyCharacterManager")
public class CharacterTagCondition extends Condition {

	private String key;
	private String value;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty())
			return false;

		String[] split = var.split(";");

		if (!split[0].matches("^[a-zA-Z0-9_]+$")) {
			MagicSpells.error("Modifier 'charactertag " + split[0]
					+ "' has invalid formatting. Character tags may only include alphanumerical characters and underscores.");
			return false;
		}

		key = var;

		if (split.length > 1)
			value = split[1];
		else
			value = "true";

		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkTag((Player) caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkTag((Player) target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkTag(Player player) {
		Character character = Character.get(player);

		if (character != null) {
			return character.tagValue(key).equals(value);
		}

		return false;
	}

}
