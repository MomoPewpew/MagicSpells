package com.nisovin.magicspells.util.compat;

import org.bukkit.entity.Player;

import com.sneakybagofholding.api.BagOfHoldingApi;
import com.sneakybagofholding.api.WithdrawResult;

/**
 * Direct references to SneakyBagOfHolding API. Only load/call via {@link BagOfHoldingCompat}
 * after confirming the plugin is enabled, so missing soft dependency does not break casts.
 */
final class BagOfHoldingAccess {

	private BagOfHoldingAccess() {
	}

	static int getStoredAmount(Player player, String itemId) {
		BagOfHoldingApi api = BagOfHoldingApi.get();
		if (api == null) return 0;
		return api.getStoredAmount(player, itemId);
	}

	/**
	 * Removes {@code amount} from bag storage without placing items in the inventory.
	 *
	 * @return amount actually consumed (0 or {@code amount})
	 */
	static int consume(Player player, String itemId, int amount) {
		if (amount <= 0) return 0;
		BagOfHoldingApi api = BagOfHoldingApi.get();
		if (api == null) return 0;
		WithdrawResult result = api.withdrawAsItemStack(player, itemId, amount);
		return result.isSuccess() ? amount : 0;
	}

}
