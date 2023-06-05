package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.CastData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

public class WindwalkSpell extends BuffSpell {

	private final Map<UUID, FlyData> players;

	private final boolean enableMaxY;
	private final ConfigData<Integer> maxY;
	private final ConfigData<Integer> maxAltitude;

	private final ConfigData<Float> flySpeed;
	private final ConfigData<Float> launchSpeed;

	private final boolean alwaysFly;
	private boolean cancelOnLand;

	private HeightMonitor heightMonitor;

	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxY = getConfigDataInt("max-y", 260);
		maxAltitude = getConfigDataInt("max-altitude", 100);
		enableMaxY = getConfigBoolean("enable-max-y", true);

		flySpeed = getConfigDataFloat("fly-speed", 0.1F);
		launchSpeed = getConfigDataFloat("launch-speed", 1F);

		alwaysFly = getConfigBoolean("always-fly", false);
		cancelOnLand = getConfigBoolean("cancel-on-land", true);

		players = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (alwaysFly) registerEvents(new FlyDisableListener());
		if (alwaysFly || cancelOnLand) registerEvents(new SneakListener());
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player player)) return false;

		float launchSpeed = this.launchSpeed.get(entity, null, power, args);
		if (launchSpeed > 0) entity.setVelocity(new Vector(0, launchSpeed, 0));
		else entity.teleportAsync(entity.getLocation().add(0, 0.25, 0));

		FlyData flyData = new FlyData(new CastData(power, args), player.getAllowFlight(), player.getFlySpeed());
		players.put(entity.getUniqueId(), flyData);

		player.setAllowFlight(true);
		player.setFlying(true);
		player.setFlySpeed(flySpeed.get(entity, null, power, args));

		if (heightMonitor == null) heightMonitor = new HeightMonitor();

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Player pl = (Player) entity;

		FlyData flyData = players.remove(pl.getUniqueId());
		pl.setFlying(false);
		pl.setAllowFlight(flyData.wasFlyingAllowed());
		pl.setFlySpeed(flyData.oldFlySpeed());
		pl.setFallDistance(0);

		if (heightMonitor != null && players.isEmpty()) {
			heightMonitor.stop();
			heightMonitor = null;
		}
	}

	@Override
	protected void turnOff() {
		Player pl;
		for (UUID id : players.keySet()) {
			pl = Bukkit.getPlayer(id);
			if (pl == null) continue;
			if (!pl.isValid()) continue;
			if (pl.getGameMode() != GameMode.CREATIVE) pl.setAllowFlight(false);

			pl.setFlying(false);
			pl.setFlySpeed(0.1F);
			pl.setFallDistance(0);
		}

		players.clear();

		if (heightMonitor == null) return;
		heightMonitor.stop();
		heightMonitor = null;
	}

	public Map<UUID, FlyData> getPlayers() {
		return players;
	}

	public boolean shouldCancelOnLand() {
		return cancelOnLand;
	}

	public void setCancelOnLand(boolean cancelOnLand) {
		this.cancelOnLand = cancelOnLand;
	}

	public class FlyDisableListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;
			if (event.isFlying()) return;
			event.setCancelled(true);
		}

	}

	public class SneakListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;
			if (BlockUtils.isAir(player.getLocation().subtract(0, 1, 0).getBlock().getType())) return;
			if (alwaysFly) {
				player.teleportAsync(player.getLocation().add(0, 0.25, 0));
				player.setFlying(true);
			}
			else if (cancelOnLand) turnOff(player);
		}

	}

	private class HeightMonitor implements Runnable {

		private final int taskId;

		private HeightMonitor() {
			taskId = MagicSpells.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
		}

		@Override
		public void run() {
			Player pl;
			CastData data;

			int yDiff;
			int yLimit;
			int altitudeLimit;

			Location loc;
			Vector v;

			for (UUID id : players.keySet()) {
				pl = Bukkit.getPlayer(id);
				if (pl == null || !pl.isValid()) continue;

				addUseAndChargeCost(pl);

				data = players.get(id).castData();

				loc = pl.getLocation();
				v = pl.getVelocity();

				yLimit = maxY.get(pl, null, data.power(), data.args());
				if (enableMaxY) {
					yDiff = loc.getBlockY() - yLimit;
					if (yDiff > 0) {
						pl.setVelocity(v.setY(-yDiff * 1.5));
						continue;
					}
				}

				altitudeLimit = maxAltitude.get(pl, null, data.power(), data.args());
				if (altitudeLimit > 0) {
					yDiff = loc.getBlockY() - pl.getWorld().getHighestBlockYAt(loc) - altitudeLimit;
					if (yDiff > 0) pl.setVelocity(v.setY(-yDiff * 1.5));
				}

				pl.setFlySpeed(flySpeed.get(pl, null, data.power(), data.args()));
			}
		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

	private record FlyData(CastData castData, boolean wasFlyingAllowed, float oldFlySpeed) {}

}
