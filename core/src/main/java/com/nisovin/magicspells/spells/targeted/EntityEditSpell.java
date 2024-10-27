package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.managers.AttributeManager;

public class EntityEditSpell extends TargetedSpell implements TargetedEntitySpell {

	private Set<AttributeManager.AttributeInfo> attributes;

	private boolean toggle;

	public EntityEditSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		// Attributes
		// - [AttributeName] [Number] [Operation]
		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) attributes = MagicSpells.getAttributeManager().getAttributes(attributeList);

		toggle = getConfigBoolean("toggle", false);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
			if (target.noTarget()) return noTarget(caster, args, target);

			applyAttributes(target.target());
			playSpellEffects(caster, target.target(), target.power(), args);
			sendMessages(caster, target.target(), args);

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		playSpellEffects(caster, target, power, args);
		applyAttributes(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		playSpellEffects(caster, target, power, null);
		applyAttributes(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		playSpellEffects(EffectPosition.TARGET, target, power, args);
		applyAttributes(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		playSpellEffects(EffectPosition.TARGET, target, power, null);
		applyAttributes(target);
		return true;
	}

	private void applyAttributes(LivingEntity entity) {
		if (attributes == null) return;
		if (toggle) {
			boolean apply = false;
			for (AttributeManager.AttributeInfo info : attributes) {
				apply = MagicSpells.getAttributeManager().hasEntityAttribute(entity, info);
			}
			if (!apply) MagicSpells.getAttributeManager().addEntityAttributes(entity, attributes);
			else MagicSpells.getAttributeManager().clearEntityAttributeModifiers(entity, attributes);
			return;
		}

		for (AttributeManager.AttributeInfo info : attributes) {
			if (MagicSpells.getAttributeManager().hasEntityAttribute(entity, info)) continue;
			MagicSpells.getAttributeManager().addEntityAttribute(entity, info);
		}
	}

}
