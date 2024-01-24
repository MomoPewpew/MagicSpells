package com.nisovin.magicspells.spells.instant;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class GateSpell extends InstantSpell {

	private ConfigData<String> world;
	private ConfigData<String> coordinates;
	private String strGateFailed;

	public GateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		world = getConfigDataString("world", "CURRENT");
		coordinates = getConfigDataString("coordinates", "SPAWN");
		strGateFailed = getConfigString("str-gate-failed", "Unable to teleport.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {

		SpellData data = new SpellData(caster, power, args);

		String world = this.world.get(data);
		String coordinates = this.coordinates.get(data);

		if (state == SpellCastState.NORMAL) {
			World effectiveWorld;
			if (world.equals("CURRENT")) effectiveWorld = caster.getWorld();
			else if (world.equals("DEFAULT")) effectiveWorld = Bukkit.getServer().getWorlds().get(0);
			else effectiveWorld = Bukkit.getServer().getWorld(world);

			if (effectiveWorld == null) {
				MagicSpells.error("GateSpell '" + internalName + "' has a non existent world defined!");
				sendMessage(strGateFailed, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			// Get location
			Location location = null;
			switch (coordinates.toUpperCase()) {
				case "SPAWN" -> {
					location = effectiveWorld.getSpawnLocation();
					location = new Location(effectiveWorld, location.getX(), effectiveWorld.getHighestBlockYAt(location) + 1, location.getZ());
				}
				case "EXACTSPAWN" -> location = effectiveWorld.getSpawnLocation();
				case "CURRENT" -> {
					Location l = caster.getLocation();
					location = new Location(effectiveWorld, l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
				}
				default -> {
					String[] c = coordinates.split(",");
					if (c.length >= 3) {
						try {
							double x = Double.parseDouble(c[0]);
							double y = Double.parseDouble(c[1]);
							double z = Double.parseDouble(c[2]);
							float yaw = 0;
							float pitch = 0;
							if (c.length > 3) {
								yaw = Float.parseFloat(c[3]);
								pitch = Float.parseFloat(c[4]);
							}

							location = new Location(effectiveWorld, x, y, z, yaw, pitch);
						} catch (NumberFormatException ignored) {}
					}

					if (location == null) {
						MagicSpells.error("GateSpell '" + internalName + "' has invalid coordinates defined!");
						sendMessage(strGateFailed, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			MagicSpells.debug(3, "Gate location: " + location);

			Block b = location.getBlock();
			if (!BlockUtils.isPathable(b) || !BlockUtils.isPathable(b.getRelative(0, 1, 0))) {
				MagicSpells.error("GateSpell '" + internalName + "' has landing spot blocked!");
				sendMessage(strGateFailed, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Location from = caster.getLocation();
			Location to = b.getLocation();
			boolean canTeleport = (!(caster instanceof Vehicle)) && !caster.isDead();
			if (!canTeleport) {
				MagicSpells.error("GateSpell '" + internalName + "': teleport prevented!");
				sendMessage(strGateFailed, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			caster.teleportAsync(location);

			data = new SpellData(caster, power, args);
			playSpellEffects(EffectPosition.CASTER, from, data);
			playSpellEffects(EffectPosition.TARGET, to, data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public ConfigData<String> getWorld() {
		return world;
	}

	public void setWorld(ConfigData<String> world) {
		this.world = world;
	}

	public ConfigData<String> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(ConfigData<String> coordinates) {
		this.coordinates = coordinates;
	}

	public String getStrGateFailed() {
		return strGateFailed;
	}

	public void setStrGateFailed(String strGateFailed) {
		this.strGateFailed = strGateFailed;
	}

}
