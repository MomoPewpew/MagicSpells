package com.nisovin.magicspells.variables.variabletypes;

import java.util.Map;
import java.util.HashMap;

import com.nisovin.magicspells.util.PlayerNameUtils;
import net.coreprotect.CoreProtect;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PlayerStringVariable extends PlayerVariable {

	private final Map<String, String> data;
	
	public PlayerStringVariable() {
		data = new HashMap<>();
	}
	
	@Override
	public void loadExtraData(ConfigurationSection section) {
		super.loadExtraData(section);
		defaultStringValue = section.getString("default-value", "");
	}
	
	@Override
	public String getStringValue(String player) {
		return data.getOrDefault(player, defaultStringValue);
	}
	
	@Override
	public void parseAndSet(String player, String textValue) {
		String current = data.get(player);

		data.put(player, textValue);
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (logInCoreprotect && current != null && Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) CoreProtect.getInstance().getAPI().logCommand(p, "/ms_var " + name + " =" + textValue);
	}
	
	@Override
	public void reset(String player) {
		data.remove(player);
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (logInCoreprotect && Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) CoreProtect.getInstance().getAPI().logCommand(p, "/ms_var " + name + " =" + defaultValue);
	}
	
}
