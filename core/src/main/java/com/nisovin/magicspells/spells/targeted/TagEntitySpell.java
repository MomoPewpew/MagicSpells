package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.SpellData;

public class TagEntitySpell extends TargetedSpell implements TargetedEntitySpell {

	private final String operation;
	private final String tag;

	private final boolean doReplacements;

	public TagEntitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tag = getConfigString("tag", null);
		operation = getConfigString("operation", "add");

		doReplacements = MagicSpells.requireReplacement(tag);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data, info);

			data = data.builder().power(info.getPower()).build();
			tag(data);
			playSpellEffects(data.caster(), info.target(), data);
			sendMessages(data.caster(), info.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		tag(data);
		playSpellEffects(data.caster(), data.target(), data);
		return true;
	}

	private void tag(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		String varTag = doReplacements ? MagicSpells.doReplacements(tag, caster, target, data.args()) : tag;

		switch (operation) {
			case "add", "insert" -> target.addScoreboardTag(varTag);
			case "remove", "take" -> target.removeScoreboardTag(varTag);
			case "clear" -> {
				Set<String> tags = new HashSet<>(target.getScoreboardTags());
				tags.forEach(target::removeScoreboardTag);
			}
			default -> MagicSpells.error("TagEntitySpell '" + internalName + "' has an invalid operation defined!");
		}
	}

}
