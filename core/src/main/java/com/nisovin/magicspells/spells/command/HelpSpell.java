package com.nisovin.magicspells.spells.command;

import java.util.List;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;

public class HelpSpell extends CommandSpell {
	
	private boolean requireKnownSpell;

	private String strUsage;
	private String strNoSpell;
	private String strDescLine;
	private String strCostLine;

	public HelpSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireKnownSpell = getConfigBoolean("require-known-spell", true);

		strUsage = getConfigString("str-usage", "Usage: /cast " + name + " <spell>");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strDescLine = getConfigString("str-desc-line", "%s - %d");
		strCostLine = getConfigString("str-cost-line", "Cost: %c");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			String[] args = data.args();
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Spell spell = MagicSpells.getSpellByInGameName(Util.arrayJoin(args, ' '));
			Spellbook spellbook = MagicSpells.getSpellbook(player);

			if (spell == null || requireKnownSpell && !spellbook.hasSpell(spell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			sendMessage(strDescLine, player, args, "%s", spell.getName(), "%d", spell.getDescription());
			if (spell.getCostStr() != null && !spell.getCostStr().isEmpty()) {
				sendMessage(strCostLine, player, args, "%c", spell.getCostStr());
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String [] args = Util.splitParams(partial);
		if (sender instanceof Player && args.length == 1) return tabCompleteSpellName(sender, partial);
		return null;
	}

}
