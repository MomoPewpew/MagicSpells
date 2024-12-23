package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Slime;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.SpellData;

public class SlimeSizeSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker SLIME = entity -> entity instanceof Slime;

	private VariableMod variableMod;

	private String size;

	private ConfigData<Integer> minSize;
	private ConfigData<Integer> maxSize;

	private static ValidTargetChecker isSlimeChecker = (LivingEntity entity) -> entity instanceof Slime;

	public SlimeSizeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		size = getConfigString("size", "=5");

		minSize = getConfigDataInt("min-size", 0);
		maxSize = getConfigDataInt("max-size", 20);
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		variableMod = new VariableMod(size);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data, SLIME);
			if (info.noTarget()) return noTarget(data, info);

			data = data.builder().power(info.getPower()).build();
			if (!setSize(data)) return noTarget(data);

			sendMessages(data.caster(), info.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return setSize(data);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return isSlimeChecker;
	}

	private boolean setSize(SpellData data) {
		if (!(data.target() instanceof Slime slime) || !(data.caster() instanceof Player player)) return false;

		int minSize = this.minSize.get(data);
		int maxSize = this.maxSize.get(data);

		if (minSize < 0) minSize = 0;
		if (maxSize < minSize) maxSize = minSize;

		double rawOutputValue = variableMod.getValue(player, null, slime.getSize(), data.power(), data.args());
		int finalSize = Util.clampValue(minSize, maxSize, (int) rawOutputValue);
		slime.setSize(finalSize);

		playSpellEffects(data.caster(), data.target(), data);

		return true;
	}

}
