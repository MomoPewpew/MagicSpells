package com.nisovin.magicspells.util.compat;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Soft-dependency entry points for SneakyBagOfHolding (reagents, conjure, etc.).
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

	/**
	 * Resolves {@code stack} to a bag item id, or null if unavailable / unregistered / not storable.
	 */
	public static String resolveItemId(ItemStack stack) {
		if (stack == null || !isAvailable()) return null;
		try {
			return BagOfHoldingAccess.resolveItemId(stack);
		} catch (NoClassDefFoundError | Exception e) {
			return null;
		}
	}

	public static boolean isAutopickupEnabled(Player player, String itemId) {
		if (player == null || itemId == null || itemId.isEmpty() || !isAvailable()) return false;
		try {
			return BagOfHoldingAccess.isAutopickupEnabled(player, itemId);
		} catch (NoClassDefFoundError | Exception e) {
			return false;
		}
	}

	public static int getRemainingCapacity(Player player, String itemId) {
		if (player == null || itemId == null || itemId.isEmpty() || !isAvailable()) return 0;
		try {
			return BagOfHoldingAccess.getRemainingCapacity(player, itemId);
		} catch (NoClassDefFoundError | Exception e) {
			return 0;
		}
	}

	/**
	 * Deposits {@code amount} directly into bag storage (does not remove from inventory).
	 *
	 * @return amount deposited
	 */
	public static int deposit(Player player, String itemId, int amount) {
		if (player == null || itemId == null || itemId.isEmpty() || amount <= 0 || !isAvailable()) return 0;
		try {
			return BagOfHoldingAccess.deposit(player, itemId, amount);
		} catch (NoClassDefFoundError | Exception e) {
			return 0;
		}
	}

	/**
	 * Mimics SneakyBagOfHolding ground-autopickup feedback (WITCH particles + configured pickup sound).
	 * Reads sound settings from the bag plugin config when available; no-ops if the plugin is missing.
	 */
	public static void playPickupFeedback(Player player) {
		if (player == null || !isAvailable()) return;
		try {
			player.spawnParticle(Particle.WITCH, player.getLocation().add(0, 0.5, 0), 5, 0.1, 0.1, 0.1, 0.1);

			String sound = "minecraft:entity.item.pickup";
			float volume = 0.2f;
			float minPitch = 0.9f;
			float maxPitch = 1.1f;

			Plugin plugin = CompatBasics.getPlugin(PLUGIN_NAME);
			if (plugin != null) {
				ConfigurationSection pickup = plugin.getConfig().getConfigurationSection("settings.sounds.pickup");
				if (pickup != null) {
					String configuredSound = pickup.getString("sound");
					if (configuredSound != null && !configuredSound.isEmpty()) sound = configuredSound;
					if (pickup.contains("volume")) volume = (float) pickup.getDouble("volume", volume);
					String pitchStr = pickup.getString("pitch");
					if (pitchStr != null && !pitchStr.isEmpty()) {
						if (pitchStr.contains("-")) {
							String[] parts = pitchStr.split("-", 2);
							try {
								minPitch = Float.parseFloat(parts[0].trim());
								maxPitch = parts.length > 1 ? Float.parseFloat(parts[1].trim()) : minPitch;
							} catch (NumberFormatException ignored) {
								// keep defaults
							}
						} else {
							try {
								minPitch = Float.parseFloat(pitchStr.trim());
								maxPitch = minPitch;
							} catch (NumberFormatException ignored) {
								// keep defaults
							}
						}
					} else if (pickup.contains("pitch")) {
						minPitch = (float) pickup.getDouble("pitch", 1.0);
						maxPitch = minPitch;
					}
				}
			}

			float pitch = minPitch >= maxPitch ? minPitch
					: ThreadLocalRandom.current().nextFloat() * (maxPitch - minPitch) + minPitch;
			player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
		} catch (NoClassDefFoundError | Exception ignored) {
			// soft dependency
		}
	}

}
