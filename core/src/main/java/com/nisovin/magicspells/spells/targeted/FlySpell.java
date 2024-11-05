package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FlySpell extends TargetedSpell implements TargetedEntitySpell {

	private final Set<UUID> wasAllowedFlight;

	private final boolean setFlying;
	private final TargetBooleanState targetBooleanState;

	public FlySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		wasAllowedFlight = new HashSet<>();

		setFlying = getConfigBoolean("set-flying", true);
		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);
			Player target = targetInfo.target();

			setFlyingState(target);
			playSpellEffects(data.caster(), target, targetInfo.getPower(), data.args());
			sendMessages(data.caster(), target, data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player) || !validTargetList.canTarget(data.caster(), data.target())) return false;
		setFlyingState(player);
		playSpellEffects(data);
		return true;
	}

	@Override
	protected void turnOff() {
		Player player;
		for (UUID uuid : wasAllowedFlight) {
			player = Bukkit.getPlayer(uuid);
			if (player == null) continue;
			if (player.getGameMode() == GameMode.CREATIVE) continue;
			if (player.getGameMode() == GameMode.SPECTATOR) continue;
			player.setAllowFlight(false);
		}
		wasAllowedFlight.clear();
	}

	private void setFlyingState(Player player) {
		boolean newState = targetBooleanState.getBooleanState(player.isFlying() || player.getAllowFlight());
		UUID uuid = player.getUniqueId();
		if (newState) {
			if (!player.getAllowFlight()) {
				player.setAllowFlight(true);
				wasAllowedFlight.add(uuid);
			}
			if (setFlying) player.teleportAsync(player.getLocation().add(0, 0.25, 0));
		}
		else {
			boolean wasAllowed = wasAllowedFlight.remove(uuid);
			if (wasAllowed) player.setAllowFlight(false);
		}
		if (setFlying) player.setFlying(newState);
	}

}
