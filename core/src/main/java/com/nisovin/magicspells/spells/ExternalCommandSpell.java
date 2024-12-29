package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.conversations.Prompt;
import org.bukkit.command.CommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MessageBlocker;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ExternalCommandSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private static MessageBlocker messageBlocker;

	private ConfigData<List<String>> commandToExecuteData;
	private ConfigData<List<String>> commandToExecuteLaterData;

	private List<String> commandToBlock;
	private List<String> commandToExecute;
	private List<String> commandToExecuteLater;
	private List<String> temporaryPermissions;

	private int commandDelay;

	private boolean temporaryOp;
	private boolean blockChatOutput;
	private boolean requirePlayerTarget;
	private boolean doVariableReplacement;
	private boolean executeAsTargetInstead;
	private boolean executeOnConsoleInstead;
	private boolean useTargetVariablesInstead;
	private String strBlockedOutput;
	private String strCantUseCommand;

	private Prompt convoPrompt;
	private ConversationFactory convoFac;

	public ExternalCommandSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		commandToBlock = getConfigStringList("command-to-block", null);
		commandToExecuteData = getConfigDataStringList("command-to-execute", null);
		commandToExecuteLaterData = getConfigDataStringList("command-to-execute-later", null);
		temporaryPermissions = getConfigStringList("temporary-permissions", null);

		commandDelay = getConfigInt("command-delay", 0);

		temporaryOp = getConfigBoolean("temporary-op", false);
		blockChatOutput = getConfigBoolean("block-chat-output", false);
		requirePlayerTarget = getConfigBoolean("require-player-target", false);
		doVariableReplacement = getConfigBoolean("do-variable-replacement", false);
		executeAsTargetInstead = getConfigBoolean("execute-as-target-instead", false);
		executeOnConsoleInstead = getConfigBoolean("execute-on-console-instead", false);
		useTargetVariablesInstead = getConfigBoolean("use-target-variables-instead", false);

		strNoTarget = getConfigString("str-no-target", "No target found.");
		strBlockedOutput = getConfigString("str-blocked-output", "");
		strCantUseCommand = getConfigString("str-cant-use-command", "&4You don't have permission to do that.");

		if (requirePlayerTarget) validTargetList = new ValidTargetList(true, false);
		
		if (blockChatOutput) {
			if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
				if (messageBlocker == null) messageBlocker = new MessageBlocker();
			} else {
				convoPrompt = new StringPrompt() {
					
					@Override
					public String getPromptText(ConversationContext context) {
						return strBlockedOutput;
					}
					
					@Override
					public Prompt acceptInput(ConversationContext context, String input) {
						return Prompt.END_OF_CONVERSATION;
					}
					
				};
				convoFac = new ConversationFactory(MagicSpells.plugin)
					.withModality(true)
					.withFirstPrompt(convoPrompt)
					.withTimeout(1);
			}
		}
	}

	@Override
	public void turnOff() {
		if (messageBlocker == null) return;
		messageBlocker.turnOff();
		messageBlocker = null;
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			if (requirePlayerTarget) {
				TargetInfo<Player> targetInfo = getTargetedPlayer(data);
				if (targetInfo.noTarget()) return noTarget(data, targetInfo);

				data = data.builder().target(targetInfo.target()).power(targetInfo.getPower()).build();
			}

			process(data);

			sendMessages(data);

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void process(SpellData data) {
		// Get actual sender
		CommandSender sender = data.caster();
		CommandSender actualSender;
		if (executeAsTargetInstead) actualSender = data.target();
		else if (executeOnConsoleInstead) actualSender = Bukkit.getConsoleSender();
		else actualSender = data.caster();
		if (actualSender == null) return;
		
		data = data.builder().caster(actualSender instanceof Player ? (Player) actualSender : null).build();

		commandToExecute = commandToExecuteData.get(data);
		commandToExecuteLater = commandToExecuteLaterData.get(data);

		// Grant permissions and op
		boolean opped = false;
		if (actualSender instanceof Player) {
			if (temporaryPermissions != null) {
				for (String perm : temporaryPermissions) {
					if (actualSender.hasPermission(perm)) continue;
					actualSender.addAttachment(MagicSpells.plugin, perm.trim(), true, 5);
				}
			}
			if (temporaryOp && !actualSender.isOp()) {
				opped = true;
				actualSender.setOp(true);
			}
		}
		
		// Perform commands
		try {
			if (commandToExecute != null && !commandToExecute.isEmpty()) {

				Conversation convo = null;
				if (sender instanceof Player player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer(player);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation(player);
						convo.begin();
					}
				}
				
				int delay = 0;
				LivingEntity varOwner, varTarget;
				if (useTargetVariablesInstead) {
					varOwner = data.target();
					varTarget = sender instanceof Player player ? player : null;
				} else {
					varOwner = sender instanceof Player player ? player : null;
					varTarget = data.target();
				}

				for (String comm : commandToExecute) {
					if (comm == null || comm.isEmpty()) continue;
					if (doVariableReplacement) comm = MagicSpells.doReplacements(comm, varOwner, varTarget, data.args());
					if (data.args() != null && data.args().length > 0) {
						for (int i = 0; i < data.args().length; i++) {
							comm = comm.replace("%" + (i + 1), data.args()[i]);
						}
					}
					if (sender != null) comm = comm.replace("%a", sender.getName());
					if (data.target() != null) comm = comm.replace("%t", data.target().getName());
					if (comm.startsWith("DELAY ")) {
						String[] split = comm.split(" ");
						delay += Integer.parseInt(split[1]);
					} else if (delay > 0) {
						final CommandSender s = actualSender;
						final String c = comm;
						MagicSpells.scheduleDelayedTask(() -> Bukkit.dispatchCommand(s, c), delay);
					} else {
						Bukkit.dispatchCommand(actualSender, comm);
					}
				}
				if (blockChatOutput && messageBlocker != null && sender instanceof Player player) messageBlocker.removePlayer(player);
				else if (convo != null) convo.abandon();
			}
		} catch (Exception e) {
			// Catch all exceptions to make sure we don't leave someone opped
			e.printStackTrace();
		}
		
		// Deop
		if (opped) actualSender.setOp(false);
		
		// Effects
		if (sender instanceof Player player) {
			if (data.target() != null) playSpellEffects(player, data.target(), data);
			else playSpellEffects(EffectPosition.CASTER, player, data);
		} else if (sender instanceof BlockCommandSender commandBlock) {
			playSpellEffects(EffectPosition.CASTER, commandBlock.getBlock().getLocation(), data);
		}
		// Add delayed command
		if (commandToExecuteLater != null && !commandToExecuteLater.isEmpty() && !commandToExecuteLater.get(0).isEmpty()) {
			MagicSpells.scheduleDelayedTask(new DelayedCommand(data), commandDelay);
		}
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;

		if (requirePlayerTarget && data.target() instanceof Player player) {
			process(data);
			return true;
		}

		return false;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (!requirePlayerTarget) {
			process(new SpellData(null));
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.getPlayer().isOp()) return;
		if (commandToBlock == null) return;
		if (commandToBlock.isEmpty()) return;
		String msg = event.getMessage();
		for (String comm : commandToBlock) {
			comm = comm.trim();
			if (comm.isEmpty()) continue;
			if (!msg.startsWith("/" + commandToBlock)) continue;

			event.setCancelled(true);
			sendMessage(strCantUseCommand, event.getPlayer(), new String[0]);
			return;
		}
	}
	
	public boolean requiresPlayerTarget() {
		return requirePlayerTarget;
	}
	
	private class DelayedCommand implements Runnable {

		private final SpellData data;

		private DelayedCommand(SpellData data) {
			this.data = data;
		}
		
		@Override
		public void run() {
			CommandSender sender = data.caster();
			// Get actual sender
			CommandSender actualSender;
			if (executeAsTargetInstead) actualSender = data.target();
			else if (executeOnConsoleInstead) actualSender = Bukkit.getConsoleSender();
			else actualSender = sender;
			if (actualSender == null) return;
			
			// Grant permissions
			boolean opped = false;
			if (actualSender instanceof Player) {
				if (temporaryPermissions != null) {
					for (String perm : temporaryPermissions) {
						if (actualSender.hasPermission(perm)) continue;
						actualSender.addAttachment(MagicSpells.plugin, perm, true, 5);
					}
				}
				if (temporaryOp && !actualSender.isOp()) {
					opped = true;
					actualSender.setOp(true);
				}
			}
			
			// Run commands
			try {
				Conversation convo = null;
				if (sender instanceof Player player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer(player);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation(player);
						convo.begin();
					}
				}

				LivingEntity varOwner, varTarget;
				if (useTargetVariablesInstead) {
					varOwner = data.target();
					varTarget = sender instanceof Player player ? player : null;
				} else {
					varOwner = sender instanceof Player player ? player : null;
					varTarget = data.target();
				}

				String[] args = data.args();

				for (String comm : commandToExecuteLater) {
					if (comm == null || comm.isEmpty()) continue;
					if (doVariableReplacement) comm = MagicSpells.doReplacements(comm, varOwner, varTarget, args);
					if (args != null && args.length > 0) {
						for (int i = 0; i < args.length; i++) {
							comm = comm.replace("%" + (i + 1), args[i]);
						}
					}
					if (sender != null) comm = comm.replace("%a", sender.getName());
					if (data.target() != null) comm = comm.replace("%t", data.target().getName());
					Bukkit.dispatchCommand(actualSender, comm);
				}
				if (blockChatOutput && messageBlocker != null && sender instanceof Player player) messageBlocker.removePlayer(player);
				else if (convo != null) convo.abandon();
			} catch (Exception e) {
				// Catch exceptions to make sure we don't leave someone opped
				e.printStackTrace();
			}
			
			// Deop
			if (opped) actualSender.setOp(false);

			// Graphical effect
			if (sender == null) return;
			if (sender instanceof Player player) playSpellEffects(EffectPosition.DISABLED, player, data);
			else if (sender instanceof BlockCommandSender commandBlock) playSpellEffects(EffectPosition.DISABLED, commandBlock.getBlock().getLocation(), data);
		}
		
	}

}
