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

	private String tag;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;

		if (!var.matches("^[a-zA-Z0-9_]+$")) {
			MagicSpells.error("Modifier 'charactertag " + var + "' has invalid formatting. Character tags may only include alphanumerical characters and underscores.");
            return false;
        }
		
		this.tag = var;
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
			return character.hasTag(tag);
		}

		return false;
	}

}
