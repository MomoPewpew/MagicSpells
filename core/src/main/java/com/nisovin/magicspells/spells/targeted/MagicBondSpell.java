package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class MagicBondSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<LivingEntity, LivingEntity> bondTarget;

	private ConfigData<Integer> duration;

	private String strDurationEnd;

	private SpellFilter filter;

	public MagicBondSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 200);
		strDurationEnd = getConfigString("str-duration", "");
		filter = getConfigSpellFilter();

		bondTarget = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			bond(data.builder()
				.target(target.target())
				.power(target.getPower())
				.build());
			sendMessages(data.caster(), target.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		bond(data);
		return true;
	}

	private void bond(SpellData data) {
		bondTarget.put(data.caster(), data.target());
		playSpellEffects(data.caster(), data.target(), data);
		SpellMonitor monitorBond = new SpellMonitor(data);
		MagicSpells.registerEvents(monitorBond);

		MagicSpells.scheduleDelayedTask(() -> {
			if (!strDurationEnd.isEmpty()) {
				if (data.caster() instanceof Player) MagicSpells.sendMessage((Player) data.caster(), strDurationEnd);
				if (data.target() instanceof Player) MagicSpells.sendMessage((Player) data.target(), strDurationEnd);
			}
			bondTarget.remove(data.caster());

			HandlerList.unregisterAll(monitorBond);
		}, duration.get(data));
	}

	private class SpellMonitor implements Listener {

		private final SpellData data;

		private SpellMonitor(SpellData data) {
			this.data = data;
		}

		@EventHandler
		public void onPlayerLeave(PlayerQuitEvent e) {
			if (bondTarget.containsKey(e.getPlayer()) || bondTarget.containsValue(e.getPlayer())) {
				bondTarget.remove(data.caster());
			}
		}

		@EventHandler
		public void onPlayerSpellCast(SpellCastEvent e) {
			Spell spell = e.getSpell();
			if (e.getCaster() != data.caster() || spell instanceof MagicBondSpell) return;
			if (spell.onCooldown(data.caster())) return;
			if (!bondTarget.containsKey(data.caster()) && !bondTarget.containsValue(data.target())) return;
			if (data.target().isDead()) return;
			if (!filter.check(spell)) return;

			spell.cast(data.builder().caster(data.target()).build());
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (!getClass().getName().equals(other.getClass().getName())) return false;
			SpellMonitor otherMonitor = (SpellMonitor)other;
			return data.equals(otherMonitor.data);
		}

	}

}
