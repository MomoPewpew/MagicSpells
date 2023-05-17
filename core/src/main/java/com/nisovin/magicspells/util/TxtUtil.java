package com.nisovin.magicspells.util;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;

public class TxtUtil {

	public static String getStringNumber(double number, int places) {
		if (places < 0) return number + "";
		if (places == 0) return (int) Math.round(number) + "";
		int x = (int) Math.pow(10, places);
		return ((double) Math.round(number * x) / x) + "";
	}

	public static String getStringNumber(String textNumber, int places) {
		String ret;
		try {
			ret = getStringNumber(Double.parseDouble(textNumber), places);
		} catch (NumberFormatException nfe) {
			ret = textNumber;
		}
		return ret;
	}

	public static String getProgressBar(double number, double max) {
		String bar = "[";

		double progress = Math.min(number / max, 1);

		long progressPer100 = Math.round(Math.floor(progress * 100));
		long progressPer10 = Math.round(Math.floor(progress * 10));

		for (int n = 0; n < progressPer10; n++) {
			bar += "\u25A0";
		}
		for (int n = 0; n < (10 - progressPer10); n++) {
			bar += "-";
		}

		bar += "]" + progressPer100 + "%";

		return bar;
	}

	public static String escapeJSON(String str) {
		return str.replaceAll("[\"\\\\]", "\\\\$0");
	}

	public static List<String> tabCompleteSpellName(CommandSender sender, String partial) {
		List<String> matches = new ArrayList<>();
		if (sender instanceof Player player) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (Spell spell : spellbook.getSpells()) {
				if (!spellbook.canTeach(spell)) continue;
				if (spell.getName().toLowerCase().startsWith(partial)) {
					matches.add(spell.getName());
					continue;
				}
				String[] aliases = spell.getAliases();
				if (aliases == null || aliases.length <= 0) continue;
				for (String alias : aliases) {
					if (alias.toLowerCase().startsWith(partial)) matches.add(alias);
				}
			}
		} else if (sender.isOp()) {
			for (Spell spell : MagicSpells.spells()) {
				if (spell.getName().toLowerCase().startsWith(partial)) {
					matches.add(spell.getName());
					continue;
				}
				String[] aliases = spell.getAliases();
				if (aliases == null || aliases.length <= 0) continue;
				for (String alias : aliases) {
					if (alias.toLowerCase().startsWith(partial)) matches.add(alias);
				}
			}
		}
		if (!matches.isEmpty()) return matches;
		return null;
	}

	public static String getPossessiveName(String name) {
		name = name.trim();
		return name + "'" + (name.endsWith("s") ? "" : "s");
	}

}
