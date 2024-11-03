package com.nisovin.magicspells.spells.targeted;

import java.util.function.Function;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.data.DataLivingEntity;

public class DataSpell extends TargetedSpell implements TargetedEntitySpell {

	private String variableName;
	private Function<? super LivingEntity, String> dataElement;
	
	public DataSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		variableName = getConfigString("variable-name", "");

		dataElement = DataLivingEntity.getDataFunction(getConfigString("data-element", "uuid"));
	}
	
	@Override
	public void initialize() {
		if (dataElement == null) MagicSpells.error("DataSpell '" + internalName + "' has an invalid option defined for data-element!");
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		if (variableName.isEmpty() || MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("DataSpell '" + internalName + "' has an invalid variable-name defined!");
		}
	}
	
	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);
			LivingEntity target = targetInfo.target();

			playSpellEffects(data.builder().target(target).power(targetInfo.getPower()).build());
			String value = dataElement.apply(target);
			MagicSpells.getVariableManager().set(variableName, player, value);

			sendMessages(data.caster(), target, data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		if (!(caster instanceof Player) || !validTargetList.canTarget(caster, target)) return false;
		playSpellEffects(data);
		String value = dataElement.apply(target);
		MagicSpells.getVariableManager().set(variableName, (Player) caster, value);
		return true;
	}

}
