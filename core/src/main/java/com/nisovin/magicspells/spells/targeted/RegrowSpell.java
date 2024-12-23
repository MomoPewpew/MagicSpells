package com.nisovin.magicspells.spells.targeted;

import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.SpellData;

//This spell currently support the shearing of sheep at the moment.
//Future tweaks for the shearing of other mobs will be added.

public class RegrowSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker SHEEP = entity -> entity instanceof Sheep;

	private DyeColor dye;

	private String requestedColor;

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private boolean configuredCorrectly;

	public RegrowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		requestedColor = getConfigString("wool-color", "");

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);

	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseSpell();
		if (!configuredCorrectly) MagicSpells.error("RegrowSpell " + internalName + " was configured incorrectly!");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data, SHEEP);
			if (target.noTarget()) return noTarget(data, target);

			boolean done = grow(data);
			if (!done) return noTarget(data);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!(data.target() instanceof Sheep) || !validTargetList.canTarget(data.caster(), data.target())) return false;
		return grow(data);
	}

	private boolean grow(SpellData data) {
		if (!configuredCorrectly) return false;
		if (!(data.target() instanceof Sheep)) return false;
		if (!((Sheep) data.target()).isSheared()) return false;
		if (!((Sheep) data.target()).isAdult()) return false;

		//If we are forcing a specific random wool color, lets set its color to this.
		if (forceWoolColor && randomWoolColor) ((Sheep) data.target()).setColor(randomizeDyeColor());
		else if (forceWoolColor && dye != null) ((Sheep) data.target()).setColor(dye);

		((Sheep) data.target()).setSheared(false);

		playSpellEffects(EffectPosition.TARGET, data.target(), data);

		return true;
	}

	private DyeColor randomizeDyeColor() {
		DyeColor[] allDyes = DyeColor.values();
		int dyePosition = random.nextInt(allDyes.length);
		return allDyes[dyePosition];
	}

	private boolean parseSpell() {
		if (forceWoolColor && !requestedColor.isEmpty()) {
			try {
				dye = DyeColor.valueOf(requestedColor);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid wool color defined. Will use sheep's color instead.");
				return false;
			}
		}
		return true;
	}

}
