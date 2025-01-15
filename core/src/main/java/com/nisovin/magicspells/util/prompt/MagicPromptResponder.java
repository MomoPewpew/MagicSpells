package com.nisovin.magicspells.util.prompt;

import org.bukkit.entity.Player;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;

public class MagicPromptResponder {

	String variableName;
	Subspell spellOnEnd;
	
	public MagicPromptResponder(ConfigurationSection section) {
		variableName = section.getString("variable-name", null);
		String spellName = section.getString("spell-on-end", null);
		if (spellName != null && !spellName.isEmpty()) {
			spellOnEnd = new Subspell(spellName);
			if (!spellOnEnd.process()) {
				spellOnEnd = null;
				MagicSpells.error("Invalid spell-on-end defined in prompt: " + spellName);
			}
		}
	}
	
	public Prompt acceptValidatedInput(ConversationContext paramConversationContext, String paramString) {
		String playerName = null;
		Conversable who = ConversationContextUtil.getConversable(paramConversationContext.getAllSessionData());
		if (who instanceof Player player) {
			playerName = player.getName();
			
			// Cast spell if configured
			if (spellOnEnd != null) {
				spellOnEnd.cast(player, 1.0F);
			}
		}

		// Try to save response to a variable.
		MagicSpells.getVariableManager().set(variableName, playerName, paramString);

		return Prompt.END_OF_CONVERSATION;
	}
	
}
