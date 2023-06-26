package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class CreatureTargetSpell extends InstantSpell {

	private String targetSpellName;
	private Subspell targetSpell;

	public CreatureTargetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetSpellName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		targetSpell = new Subspell(targetSpellName);
		if (!targetSpell.process()) {
			targetSpell = null;
			if (!targetSpellName.isEmpty()) MagicSpells.error("CreatureTargetSpell '" + internalName + "' has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			castSpells(caster, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void castSpells(LivingEntity caster, float power, String[] args) {
		if (!(caster instanceof Creature creature)) return;

		LivingEntity target = creature.getTarget();
		if (target == null || !target.isValid()) return;

		playSpellEffects(caster, target, power, args);

		if (targetSpell != null) targetSpell.subcast(caster, caster.getLocation(), target, power, args);
	}

}
