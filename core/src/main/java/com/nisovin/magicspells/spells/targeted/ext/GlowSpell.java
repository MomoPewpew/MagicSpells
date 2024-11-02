package com.nisovin.magicspells.spells.targeted.ext;

import java.util.Set;
import java.util.UUID;
import java.util.Collection;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import ru.xezard.glow.data.glow.Glow;
import ru.xezard.glow.data.glow.Glow.GlowBuilder;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.buff.InvisibilitySpell;

/*
 * NOTE: ProtocolLib and XGlow are required for this spell class.
 * XGlow: https://github.com/Xezard/XGlow
 */
public class GlowSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Multimap<UUID, GlowData> glowing;

	private GlowBuilder glowBuilder;

	private ChatColor color;

	private final ConfigData<Integer> duration;

	private final boolean powerAffectsDuration;

	public GlowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		glowing = LinkedListMultimap.create();

		duration = getConfigDataInt("duration", 0);

		powerAffectsDuration = getConfigBoolean("power-affects-duration", true);

		String colorName = getConfigString("color", "");
		try {
			color = ChatColor.valueOf(colorName.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			color = ChatColor.WHITE;
			MagicSpells.log("GlowSpell '" + internalName + "' has an invalid color defined: '" + colorName + "'.");
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		glowBuilder = Glow.builder().color(color).name(MagicSpells.getInstance().getName());
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player caster) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data);

			LivingEntity target = info.target();
			data = data.builder().power(info.getPower()).build();

			glow(data);
			playSpellEffects(data);
			sendMessages(caster, target, data.args());

			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target()) || !(data.caster() instanceof Player player)) return false;
		glow(data);
		playSpellEffects(data);
		return true;
	}

	@Override
	public void turnOff() {
		glowing.values().forEach(glowData -> glowData.getGlow().destroy());
	}

	private void glow(SpellData data) {
		Player caster = (Player) data.caster();
		LivingEntity target = data.target();
		float power = data.power();

		int duration = this.duration.get(data);
		if (powerAffectsDuration) duration = Math.round(duration * power);

		Collection<GlowData> glows = glowing.get(caster.getUniqueId());
		for (GlowData glowData : glows) {
			// That entity is glowing for the caster
			if (!glowData.getGlow().hasHolder(target)) continue;

			// If casted by the same spell, extend duration, otherwise fail
			if (!glowData.getInternalName().equals(internalName)) return;

			MagicSpells.cancelTask(glowData.getTaskId());
			glowData.setTaskId(MagicSpells.scheduleDelayedTask(() -> {
				// Make the target hidden if it has an invisibility spell active
				if (target instanceof Player targetPlayer) {
					Set<BuffSpell> buffSpells = MagicSpells.getBuffManager().getActiveBuffs(target);
					if (buffSpells != null) {
						for (BuffSpell buffSpell : buffSpells) {
							if (!(buffSpell instanceof InvisibilitySpell)) continue;
							if (!caster.canSee(targetPlayer)) continue;

							caster.hidePlayer(MagicSpells.getInstance(), targetPlayer);
						}
					}
				}

				glowData.getGlow().destroy();
				glowing.remove(caster.getUniqueId(), glowData);
			}, duration));

			return;
		}

		GlowData glowData;

		Glow glow = glowBuilder.name(target.getUniqueId().toString() + caster.getUniqueId().toString() + internalName).build();
		glow.addHolders(target);
		glow.display(caster);

		glowData = new GlowData(glow, internalName);
		glowData.setTaskId(MagicSpells.scheduleDelayedTask(() -> {
			// Make the target hidden if it has an invisibility spell active
			if (target instanceof Player targetPlayer) {
				Set<BuffSpell> buffSpells = MagicSpells.getBuffManager().getActiveBuffs(targetPlayer);
				if (buffSpells != null) {
					for (BuffSpell buffSpell : buffSpells) {
						if (!(buffSpell instanceof InvisibilitySpell)) continue;
						if (!caster.canSee(targetPlayer)) continue;

						caster.hidePlayer(MagicSpells.getInstance(), targetPlayer);
					}
				}
			}

			glow.destroy();
			glowing.remove(caster.getUniqueId(), glowData);
		}, duration));

		// If target is a vanished player, make the caster see the target with vanish
		if (target instanceof Player targetPlayer) {
			Set<BuffSpell> buffSpells = MagicSpells.getBuffManager().getActiveBuffs(targetPlayer);
			if (buffSpells != null) {
				for (BuffSpell buffSpell : buffSpells) {
					if (!(buffSpell instanceof InvisibilitySpell)) continue;
					if (caster.canSee(targetPlayer)) continue;

					caster.showPlayer(MagicSpells.getInstance(), targetPlayer);
				}
			}
		}

		glowing.put(caster.getUniqueId(), glowData);
	}

	private static class GlowData {

		private Glow glow;
		private int taskId;
		private String internalName;

		private GlowData(Glow glow, String internalName) {
			this.glow = glow;
			this.internalName = internalName;
		}

		public Glow getGlow() {
			return glow;
		}

		public void setGlow(Glow glow) {
			this.glow = glow;
		}

		public int getTaskId() {
			return taskId;
		}

		public void setTaskId(int taskId) {
			this.taskId = taskId;
		}

		public String getInternalName() {
			return internalName;
		}

		public void setInternalName(String internalName) {
			this.internalName = internalName;
		}

	}

}
