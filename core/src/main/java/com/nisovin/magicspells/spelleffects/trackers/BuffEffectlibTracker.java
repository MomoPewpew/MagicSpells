package com.nisovin.magicspells.spelleffects.trackers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class BuffEffectlibTracker extends AsyncEffectTracker implements Runnable {

	private final Effect effectlibEffect;

	private Location entityLoc;

	private Effect modifiedEffect;

	public BuffEffectlibTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		super(entity, checker, effect, data);

		effectlibEffect = effect.playEffectLib(entity.getLocation(), data);
		if (effectlibEffect != null) effectlibEffect.infinite();
	}

	@Override
	public void run() {
		if (!canRun()) {
			stop();
			return;
		}

		entityLoc = effect.applyOffsets(entity.getLocation(), data);

		effectlibEffect.setLocation(entityLoc);
		if (effectlibEffect instanceof ModifiedEffect) {
			modifiedEffect = ((ModifiedEffect) effectlibEffect).getInnerEffect();
			if (modifiedEffect != null) modifiedEffect.setLocation(entityLoc);
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (effectlibEffect != null) effectlibEffect.cancel();
	}

	public Effect getEffectlibEffect() {
		return effectlibEffect;
	}

	public boolean canRun() {
		if (entity == null) return false;
		if (!entity.isValid()) return false;
		if (!checker.isActive(entity)) return false;
		if (effect == null) return false;
		if (effectlibEffect == null) return false;
		return true;
	}

}
