package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Objective;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;
import com.nisovin.magicspells.util.SpellData;

public class ScoreboardDataSpell extends TargetedSpell implements TargetedEntitySpell {

	private String variableName;
	private String objectiveName;
	private Objective objective;

	public ScoreboardDataSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		variableName = getConfigString("variable-name", "");
		objectiveName = getConfigString("objective-name", "");
	}

	@Override
	public void initialize() {
		if (objectiveName == null) {
			MagicSpells.error("ScoreboardDataSpell '" + internalName + "' has an invalid objective name defined for objective-name!");
			return;
		}

		objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective == null) {
			MagicSpells.error("ScoreboardDataSpell '" + internalName + "' has an objective name defined for objective-name that could not be resolved as an existing objective!");
			objectiveName = null;
		}
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		if (variableName.isEmpty() || MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("ScoreboardDataSpell '" + internalName + "' has an invalid variable-name defined!");
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(data, info);

			data = data.builder().power(info.getPower()).build();
			setScore(player, info.target());
			playSpellEffects(player, info.target(), data);
			sendMessages(data.caster(), info.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!(data.caster() instanceof Player player) || !validTargetList.canTarget(data.caster(), data.target())) return false;

		setScore(player, data.target());
		playSpellEffects(data.caster(), data.target(), data);

		return true;
	}

	private void setScore(Player caster, LivingEntity target) {
		if (objective == null) return;

		Variable variable = MagicSpells.getVariableManager().getVariable(variableName);
		if (variable == null) return;

		int score = objective.getScoreFor(target).getScore();

		if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable)
			variable.parseAndSet(caster, String.valueOf(score));
		else variable.set(caster, score);
	}

}