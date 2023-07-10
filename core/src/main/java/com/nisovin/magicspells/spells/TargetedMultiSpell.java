package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location locTarget = null;
			LivingEntity entTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> info = getTargetedEntity(caster, power, args);
				if (info.noTarget()) return noTarget(caster, args, info);

				entTarget = info.target();
				power = info.power();
			} else if (pointBlank) {
				locTarget = caster.getLocation();
			} else {
				Block b;
				try {
					b = getTargetedBlock(caster, power, args);
					if (b != null && !BlockUtils.isAir(b.getType())) {
						locTarget = b.getLocation();
						locTarget.add(0.5, 0, 0.5);
					}
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
				}
			}
			if (locTarget == null && entTarget == null) return noTarget(caster, args);
			if (locTarget != null) {
				locTarget.setY(locTarget.getY() + yOffset.get(caster, null, power, args));
				locTarget.setDirection(caster.getLocation().getDirection());
			}

			boolean somethingWasDone = runSpells(caster, null, entTarget, locTarget, power, args);
			if (!somethingWasDone) return noTarget(caster, args);

			if (entTarget != null) {
				sendMessages(caster, entTarget, args);
				return PostCastAction.NO_MESSAGES;
			}
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return runSpells(caster, null, null, target.clone().add(0, yOffset.get(caster, null, power, args), 0), power, null);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return runSpells(caster, null, null, target.clone().add(0, yOffset.get(caster, null, power, null), 0), power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return runSpells(null, null, null, target.clone().add(0, yOffset.get(null, null, power, args), 0), power, null);
	}

	@Override
	public boolean castAtLocation(Location location, float power) {
		return runSpells(null, null, null, location.clone().add(0, yOffset.get(null, null, power, null), 0), power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		return runSpells(caster, null, target, null, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return runSpells(caster, null, target, null, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		return runSpells(null, null, target, null, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return runSpells(null, null, target, null, power, null);
	}

	private boolean runSpells(LivingEntity caster, Location center, LivingEntity targetEnt, Location targetLoc, float power, String[] args) {
		if (targetEnt != null && (caster == null ? !validTargetList.canTarget(targetEnt) : !validTargetList.canTarget(caster, targetEnt)))
			return false;

		boolean somethingWasDone = false;
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
						boolean ok = castTargetedSpells(spell, caster, center, targetEnt, targetLoc, power, args);
						if (ok) somethingWasDone = true;
						else if (stopOnFail) break;
						continue;
					}

					DelayedSpell ds = new DelayedSpell(spell, caster, center, targetEnt, targetLoc, power, args, delayedSpells);
					delayedSpells.add(ds);
					MagicSpells.scheduleDelayedTask(ds, delay);
					somethingWasDone = true;
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) somethingWasDone = castTargetedSpells(action.getSpell(), caster, center, targetEnt, targetLoc, power, args);
		}
		if (somethingWasDone) {
			if (caster != null) {
				if (targetEnt != null) playSpellEffects(caster, targetEnt, power, args);
				else if (targetLoc != null) playSpellEffects(caster, targetLoc, power, args);
			} else {
				if (targetEnt != null) playSpellEffects(EffectPosition.TARGET, targetEnt, power, args);
				else if (targetLoc != null) playSpellEffects(EffectPosition.TARGET, targetLoc, power, args);
			}
		}
		return somethingWasDone;
	}

	private boolean castTargetedSpells(Subspell spell, LivingEntity caster, Location center, LivingEntity targetEnt, Location targetLoc, float power, String[] args) {
		if (targetEnt != null && center != null) return spell.subcast(caster, center, targetEnt, power, args, passTargeting);
		if (targetEnt != null) return spell.subcast(caster, targetEnt, power, args, passTargeting);
		if (targetLoc != null) return spell.subcast(caster, targetLoc, power, args);
		return spell.subcast(caster, power, args);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		return runSpells(caster, from, target, null, power, args);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		return runSpells(null, from, target, null, power, args);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return runSpells(caster, from, target, null, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return runSpells(null, from, target, null, power, null);
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
		private final LivingEntity caster;
		private final Location center;
		private final LivingEntity targetEnt;
		private final Location targetLoc;
		private final String[] args;
		private final float power;

		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;

		private DelayedSpell(Subspell spell, LivingEntity caster, Location center, LivingEntity targetEnt, Location targetLoc, float power, String[] args, List<DelayedSpell> delayedSpells) {
			this.spell = spell;
			this.caster = caster;
			this.center = center;
			this.targetEnt = targetEnt;
			this.targetLoc = targetLoc;
			this.args = args;
			this.power = power;
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

			if (caster == null || caster.isValid()) {
				boolean ok = castTargetedSpells(spell, caster, center, targetEnt, targetLoc, power, args);
				delayedSpells.remove(this);
				if (!ok && stopOnFail) cancelAll();
			} else cancelAll();

			delayedSpells = null;
		}

	}

}
