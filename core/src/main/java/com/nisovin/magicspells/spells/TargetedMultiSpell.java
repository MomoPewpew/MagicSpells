package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.nisovin.magicspells.util.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private static final Pattern DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private List<Action> actions;
	private List<String> spellList;

	private ConfigData<Float> yOffset;

	private boolean pointBlank;
	private boolean stopOnFail;
	private boolean passTargeting;
	private boolean requireEntityTarget;
	private boolean castRandomSpellInstead;

	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		yOffset = getConfigDataFloat("y-offset", 0F);

		pointBlank = getConfigBoolean("point-blank", false);
		stopOnFail = getConfigBoolean("stop-on-fail", true);
		passTargeting = getConfigBoolean("pass-targeting", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (spellList == null) return;

		for (String s : spellList) {
			if (RegexUtil.matches(DELAY_PATTERN, s)) {
				int delay = Integer.parseInt(s.split(" ")[1]);
				actions.add(new Action(delay));
				continue;
			}

			Subspell spell = new Subspell(s);
			if (spell.process()) actions.add(new Action(spell));
			else MagicSpells.error("TargetedMultiSpell '" + internalName + "' has an invalid spell '" + s + "' defined!");
		}

		spellList = null;
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Location locTarget = null;
			LivingEntity entTarget = null;
			LivingEntity caster = data.caster();
			float power = data.power();
			String[] args = data.args();

			if (requireEntityTarget) {
				TargetInfo<LivingEntity> info = getTargetedEntity(data);
				if (info.noTarget()) return noTarget(data, info);

				entTarget = info.target();
				power = info.getPower();
			} else if (pointBlank) {
				locTarget = caster.getLocation();
			} else {
				Block b;
				try {
					b = getTargetedBlock(data);
					if (b != null && !BlockUtils.isAir(b.getType())) {
						locTarget = b.getLocation();
						locTarget.add(0.5, 0, 0.5);
					}
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
				}
			}
			if (locTarget == null && entTarget == null) return noTarget(data);
			if (locTarget != null) {
				locTarget.setY(locTarget.getY() + yOffset.get(data));
				locTarget.setDirection(caster.getLocation().getDirection());
			}

			boolean somethingWasDone = runSpells(data, null);
			if (!somethingWasDone) return noTarget(data);

			if (entTarget != null) {
				sendMessages(caster, entTarget, args);
				return PostCastAction.NO_MESSAGES;
			}
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
        assert data.location() != null;
        return runSpells(data.builder().location(data.location().clone().add(0, yOffset.get(data), 0)).build(), null);
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		return runSpells(data, null);
	}

	private boolean runSpells(SpellData data, Location center) {
		LivingEntity caster = data.caster();
		LivingEntity targetEnt = data.target();

		if (targetEnt != null && (caster == null ? !validTargetList.canTarget(targetEnt) : !validTargetList.canTarget(caster, targetEnt)))
			return false;

		boolean somethingWasDone = false;
		if (data.target() != null && center != null) data = data.builder().location(center).build();
		if (!castRandomSpellInstead) {
			int delay = 0;
			Subspell spell;
			List<DelayedSpell> delayedSpells = new ArrayList<>();
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
					continue;
				}

				if (action.isSpell()) {
					spell = action.getSpell();
					if (delay == 0) {
						boolean ok = castTargetedSpells(spell, data);
						if (ok) somethingWasDone = true;
						else if (stopOnFail) break;
						continue;
					}

					DelayedSpell ds = new DelayedSpell(spell, data, delayedSpells);
					delayedSpells.add(ds);
					MagicSpells.scheduleDelayedTask(ds, delay);
					somethingWasDone = true;
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) somethingWasDone = castTargetedSpells(action.getSpell(), data);
		}
		if (somethingWasDone) {
			if (caster != null) {
				if (targetEnt != null || data.location() != null) playSpellEffects(data);
			} else {
				if (targetEnt != null || data.location() != null) playSpellEffects(EffectPosition.TARGET, data.caster(), data);
			}
		}
		return somethingWasDone;
	}

	private boolean castTargetedSpells(Subspell spell, SpellData data) {
		if (data.target() != null) return spell.subcast(data, passTargeting);
		return spell.subcast(data);
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		return runSpells(data, null);
	}

	private static class Action {

		private final Subspell spell;
		private final int delay;

		private Action(Subspell spell) {
			this.spell = spell;
			delay = 0;
		}

		private Action(int delay) {
			this.delay = delay;
			spell = null;
		}

		public boolean isSpell() {
			return spell != null;
		}

		public Subspell getSpell() {
			return spell;
		}

		public boolean isDelay() {
			return delay > 0;
		}

		public int getDelay() {
			return delay;
		}

	}

	private class DelayedSpell implements Runnable {

		private final Subspell spell;
		private final SpellData data;

		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;

		private DelayedSpell(Subspell spell, SpellData data, List<DelayedSpell> delayedSpells) {
			this.spell = spell;
			this.data = data;
			this.delayedSpells = delayedSpells;

			cancelled = false;
		}

		public void cancel() {
			cancelled = true;
			delayedSpells = null;
		}

		public void cancelAll() {
			for (DelayedSpell ds : delayedSpells) {
				if (ds == this) continue;
				ds.cancel();
			}
			delayedSpells.clear();
			cancel();
		}

		@Override
		public void run() {
			if (cancelled) {
				delayedSpells = null;
				return;
			}

			if (data.caster() == null || data.caster().isValid()) {
				boolean ok = castTargetedSpells(spell, data);
				delayedSpells.remove(this);
				if (!ok && stopOnFail) cancelAll();
			} else cancelAll();

			delayedSpells = null;
		}

	}

}
