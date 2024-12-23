package com.nisovin.magicspells.spells.targeted;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.SpellData;

//This spell currently support the shearing of sheep at the moment.
//Future tweaks for the shearing of other mobs will be added.

public class ShearSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker SHEEP = entity -> entity instanceof Sheep;
	private static final DyeColor[] colors = DyeColor.values();

	private DyeColor dye;

	private String requestedColor;

	private ConfigData<Integer> minWool;
	private ConfigData<Integer> maxWool;

	private ConfigData<Double> dropOffset;

	private boolean forceWoolColor;
	private boolean randomWoolColor;
	private boolean configuredCorrectly;

	public ShearSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		requestedColor = getConfigString("wool-color", "");

		minWool = getConfigDataInt("min-wool-drop", 1);
		maxWool = getConfigDataInt("max-wool-drop", 3);

		dropOffset = getConfigDataDouble("drop-offset", 1);

		forceWoolColor = getConfigBoolean("force-wool-color", false);
		randomWoolColor = getConfigBoolean("random-wool-color", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		configuredCorrectly = parseSpell();
		if (!configuredCorrectly) MagicSpells.error("ShearSpell " + internalName + " was configured incorrectly!");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data, SHEEP);
			if (target.noTarget()) return noTarget(data, target);

			data = data.builder().power(target.getPower()).build();
			boolean done = shear(data);
			if (!done) return noTarget(data);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		return data.target() instanceof Sheep sheep && validTargetList.canTarget(data.caster(), data.target()) && shear(data);
	}

	private boolean parseSpell() {
		if (forceWoolColor && requestedColor != null) {
			try {
				dye = DyeColor.valueOf(requestedColor.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid wool color defined. Will use sheep's color instead.");
				requestedColor = null;
				return false;
			}
		}
		return true;
	}

	private boolean shear(SpellData data) {
		if (!configuredCorrectly || !(data.target() instanceof Sheep sheep) || sheep.isSheared() || !sheep.isAdult()) return false;

		DyeColor color = null;
		if (forceWoolColor) {
			if (randomWoolColor) color = colors[Util.getRandomInt(colors.length)];
			else if (dye != null) color = dye;
		} else color = sheep.getColor();
		if (color == null) color = DyeColor.WHITE;
		Material woolColor = Material.getMaterial(color.name() + "_WOOL");
		if (woolColor == null) woolColor = Material.WHITE_WOOL;

		int maxWool = this.maxWool.get(data);
		int minWool = this.minWool.get(data);
		int count;
		if (maxWool != 0) count = random.nextInt((maxWool - minWool) + 1) + minWool;
		else count = random.nextInt(minWool + 1);

		sheep.setSheared(true);
		sheep.getWorld().dropItemNaturally(sheep.getLocation().add(0, dropOffset.get(data), 0), new ItemStack(woolColor, count));

		if (data.caster() != null) playSpellEffects(data.caster(), sheep, data);
		else playSpellEffects(EffectPosition.TARGET, sheep, data);

		return true;
	}

}
