package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.SpellData;

public class MountSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Integer> duration;

	private boolean reverse;

	public MountSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 0);

		reverse = getConfigBoolean("reverse", false);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);

			mount(data.builder()
				.target(targetInfo.target())
				.power(targetInfo.getPower())
				.build());
			sendMessages(data.caster(), targetInfo.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		mount(data);
		return true;
	}

	private void mount(SpellData data) {
		if (data.caster() == null || data.target() == null) return;

		int duration = this.duration.get(data);

		if (reverse) {
			if (!data.caster().getPassengers().isEmpty()) data.caster().eject();
			if (data.caster().getVehicle() != null) data.caster().getVehicle().eject();
			if (data.target().getVehicle() != null) data.target().getVehicle().eject();

			data.caster().addPassenger(data.target());
			if (duration > 0) {
				final SpellData finalData = data;
				MagicSpells.scheduleDelayedTask(() -> finalData.caster().removePassenger(finalData.target()), duration);
			}
			sendMessages(data.caster(), data.target(), data.args());
			return;
		}

		if (data.caster().getVehicle() != null) {
			Entity veh = data.caster().getVehicle();
			veh.eject();
			List<Entity> passengers = data.caster().getPassengers();
			if (passengers.isEmpty()) return;

			data.caster().eject();
			for (Entity e : passengers) {
				veh.addPassenger(e);
				if (duration > 0) {
					MagicSpells.scheduleDelayedTask(() -> veh.removePassenger(e), duration);
				}
			}
			return;
		}

		for (Entity e : data.target().getPassengers()) {
			if (!(e instanceof LivingEntity)) continue;
			data = data.builder().target((LivingEntity) e).build();
			break;
		}

		data.caster().eject();
		data.target().addPassenger(data.caster());
		if (duration > 0) {
			final SpellData finalData = data;
			MagicSpells.scheduleDelayedTask(() -> finalData.target().removePassenger(finalData.caster()), duration);
		}

		playSpellEffects(data.caster(), data.target(), data);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Entity vehicle = player.getVehicle();
		List<Entity> passengers = player.getPassengers();
		if (!passengers.isEmpty()) player.eject();
		if (vehicle instanceof Player) vehicle.eject();
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Entity vehicle = player.getVehicle();
		List<Entity> passengers = player.getPassengers();
		if (!passengers.isEmpty()) player.eject();
		if (vehicle instanceof Player) vehicle.eject();
	}

}
