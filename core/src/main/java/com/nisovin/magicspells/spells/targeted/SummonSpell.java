package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class SummonSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private Map<Player, Location> pendingSummons;
	private Map<Player, Long> pendingTimes;

	private int maxAcceptDelay;

	private boolean requireExactName;
	private boolean requireAcceptance;

	private String strUsage;
	private String acceptCommand;
	private String strSummonPending;
	private String strSummonExpired;
	private String strSummonAccepted;

	public SummonSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxAcceptDelay = getConfigInt("max-accept-delay", 90);

		requireExactName = getConfigBoolean("require-exact-name", false);
		requireAcceptance = getConfigBoolean("require-acceptance", true);

		strUsage = getConfigString("str-usage", "Usage: /cast summon <playername>, or /cast summon \nwhile looking at a sign with a player name on the first line.");
		acceptCommand = getConfigString("accept-command", "accept");
		strSummonPending = getConfigString("str-summon-pending", "You are being summoned! Type /accept to teleport.");
		strSummonExpired = getConfigString("str-summon-expired", "The summon has expired.");
		strSummonAccepted = getConfigString("str-summon-accepted", "You have been summoned.");

		if (requireAcceptance) {
			pendingSummons = new HashMap<>();
			pendingTimes = new HashMap<>();
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			// Get target name and landing location
			String targetName = "";
			
			if (data.args() != null && data.args().length > 0) {
				targetName = data.args()[0];
			} else {
				Block block = getTargetedBlock(data.caster(), 10, data.args());
				if (block != null && (block.getType().name().contains("SIGN"))) {
					Sign sign = (Sign) block.getState();
					targetName = Util.getStringFromComponent(sign.line(0));
				}
			}
			
			// Check usage
			if (targetName.isEmpty()) {
				// Fail -- show usage
				sendMessage(strUsage, data.caster(), data.args());
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Check location
			if (!BlockUtils.isSafeToStand(data.caster().getLocation().add(0, .25, 0).clone())) {
				sendMessage(strUsage, data.caster(), data.args());
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Get player
			Player target = null;
			if (requireExactName) {
				target = PlayerNameUtils.getPlayer(targetName);
				if (target != null && !target.getName().equalsIgnoreCase(targetName)) target = null;
			} else {
				List<Player> players = Bukkit.getServer().matchPlayer(targetName);
				if (players.size() == 1) {
					target = players.get(0);
				}
			}
			if (target == null) return noTarget(data);

			// Teleport player
			String displayName = Util.getStringFromComponent(player.displayName());
			if (requireAcceptance) {
				pendingSummons.put(target, data.caster().getLocation().add(0, .25, 0));
				pendingTimes.put(target, System.currentTimeMillis());
				sendMessage(strSummonPending, target, data.args(), "%a", displayName);
			} else {
				target.teleportAsync(data.caster().getLocation().add(0, .25, 0));
				sendMessage(strSummonAccepted, target, data.args(), "%a", displayName);
			}

			sendMessages(data.caster(), target, data.args());
			return PostCastAction.NO_MESSAGES;
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return data.target().teleport(data.caster());
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		data.target().teleportAsync(data.location());
		return true;
	}

	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!requireAcceptance) return;
		if (!event.getMessage().equalsIgnoreCase('/' + acceptCommand)) return;
		if (!pendingSummons.containsKey(event.getPlayer())) return;

		Player player = event.getPlayer();
		if (maxAcceptDelay > 0 && pendingTimes.get(player) + maxAcceptDelay * TimeUtil.MILLISECONDS_PER_SECOND < System.currentTimeMillis()) {
			sendMessage(strSummonExpired, player, new String[0]);
		} else {
			player.teleportAsync(pendingSummons.get(player));
			sendMessage(strSummonAccepted, player, new String[0]);
		}
		pendingSummons.remove(player);
		pendingTimes.remove(player);
		event.setCancelled(true);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (partial.contains(" ")) return null;
		return tabCompletePlayerName(sender, partial);
	}

}
