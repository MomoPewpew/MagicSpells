package com.nisovin.magicspells.volatilecode;

import org.bukkit.plugin.Plugin;

public interface VolatileCodeHelper {

	void error(String message);

	int scheduleDelayedTask(Runnable task, long delay);

	Plugin getInstance();

}
