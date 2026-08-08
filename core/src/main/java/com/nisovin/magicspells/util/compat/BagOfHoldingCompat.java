package com.nisovin.magicspells.util.compat;

import org.bukkit.entity.Player;

/**
 * Soft-dependency entry points for SneakyBagOfHolding reagent consumption.
 */
public final class BagOfHoldingCompat {

	private static final String PLUGIN_NAME = "SneakyBagOfHolding";

	private BagOfHoldingCompat() {
	}

	public static boolean isAvailable() {
		return CompatBasics.pluginEnabled(PLUGIN_NAME);
	}

	/**
	 * Stored count for {@code itemId}, or 0 if the plugin/API is unavailable or the item is unknown.
	 */
	public static int getStoredAmount(Player player, String itemId) {
		if (player == null || itemId == null || itemId.isEmpty() || !isAvailable()) return 0;
		try {
			return BagOfHoldingAccess.getStoredAmount(player, itemId);
		} catch (NoClassDefFoundError | Exception e) {
			return 0;
		}
	}

	/**
	 * Consumes exactly {@code amount} from bag storage (does not touch inventory).
	 *
	 * @return amount consumed (0 on failure)
	 */
	public static int consume(Player player, String itemId, int amount) {
		if (player == null || itemId == null || itemId.isEmpty() || amount <= 0 || !isAvailable()) return 0;
		try {
			return BagOfHoldingAccess.consume(player, itemId, amount);
		} catch (NoClassDefFoundError | Exception e) {
			return 0;
		}
	}

}
