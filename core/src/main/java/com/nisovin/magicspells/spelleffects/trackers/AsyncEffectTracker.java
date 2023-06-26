package com.nisovin.magicspells.spelleffects.trackers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class AsyncEffectTracker implements Runnable {

	protected Entity entity;
	protected BuffSpell buffSpell;
	protected SpellEffect effect;
	protected SpellEffectActiveChecker checker;

	protected SpellData data;

	protected BukkitTask effectTask;

	public AsyncEffectTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		this.entity = entity;
		this.checker = checker;
		this.effect = effect;
		this.data = data;

		int interval = effect.getEffectInterval().get(data);
		effectTask = Bukkit.getScheduler().runTaskTimerAsynchronously(MagicSpells.getInstance(), this, 0, interval);
	}

	public Entity getEntity() {
		return entity;
	}

	public BuffSpell getBuffSpell() {
		return buffSpell;
	}

	public SpellEffect getEffect() {
		return effect;
	}

	public SpellEffectActiveChecker getChecker() {
		return checker;
	}

	public BukkitTask getEffectTrackerTask() {
		return effectTask;
	}

	public SpellData getData() {
		return data;
	}

	public void setBuffSpell(BuffSpell spell) {
		buffSpell = spell;
	}

	@Override
	public void run() {

	}

	public void stop() {
		effectTask.cancel();
		entity = null;
	}

	public void unregister() {
		if (buffSpell != null) buffSpell.getAsyncEffectTrackers().remove(this);
	}

}
