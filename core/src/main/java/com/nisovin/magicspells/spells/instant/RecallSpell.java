package com.nisovin.magicspells.spells.instant;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RecallSpell extends InstantSpell implements TargetedEntitySpell {

	private ConfigData<Double> maxRange;

	private boolean useBedLocation;
	private boolean allowCrossWorld;

	private String strNoMark;
	private String strTooFar;
	private String strOtherWorld;
	private String strRecallFailed;

	private MarkSpell markSpell;
	private final String markSpellName;

	public RecallSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxRange = getConfigDataDouble("max-range", 0);

		useBedLocation = getConfigBoolean("use-bed-location", false);
		allowCrossWorld = getConfigBoolean("allow-cross-world", true);

		strNoMark = getConfigString("str-no-mark", "You have no mark to recall to.");
		strTooFar = getConfigString("str-too-far", "You mark is too far away.");
		strOtherWorld = getConfigString("str-other-world", "Your mark is in another world.");
		strRecallFailed = getConfigString("str-recall-failed", "Could not recall.");
		markSpellName = getConfigString("mark-spell", "mark");
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell instanceof MarkSpell) markSpell = (MarkSpell) spell;
		else MagicSpells.error("RecallSpell '" + internalName + "' has an invalid mark-spell defined!");
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity caster = data.caster();
			float power = data.power();
			String[] args = data.args();

			Location markLocation = null;
			if (args != null && args.length == 1 && caster.hasPermission("magicspells.advanced." + internalName)) {
				Player target = PlayerNameUtils.getPlayer(args[0]);
				if (useBedLocation && target != null) markLocation = target.getBedSpawnLocation();
				else if (markSpell != null) {
					Location loc = markSpell.getEffectiveMark(target != null ? target.getName().toLowerCase() : args[0].toLowerCase());
					if (loc != null) markLocation = loc;
				}
			} else markLocation = getRecallLocation(caster);

			if (markLocation == null) {
				sendMessage(strNoMark, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Location from = caster.getLocation();

			if (!allowCrossWorld && !LocationUtil.isSameWorld(markLocation, from)) {
				sendMessage(strOtherWorld, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			double maxRange = this.maxRange.get(data);
			if (maxRange > 0 && markLocation.distanceSquared(from) > maxRange * maxRange) {
				sendMessage(strTooFar, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			boolean canTeleport = (!(caster instanceof Vehicle)) && !caster.isDead();
			if (!canTeleport) {
				MagicSpells.error("Recall teleport blocked for " + caster.getName());
				sendMessage(strRecallFailed, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			caster.teleportAsync(markLocation);

			playSpellEffects(EffectPosition.CASTER, from, data);
			playSpellEffects(EffectPosition.TARGET, markLocation, data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;

		Location mark = getRecallLocation(data.caster());
		if (mark == null) return false;

		data.target().teleportAsync(mark);
		return true;
	}

	private Location getRecallLocation(LivingEntity caster) {
		if (useBedLocation && caster instanceof Player) return ((Player) caster).getBedSpawnLocation();
		if (markSpell == null) return null;
		return markSpell.getEffectiveMark(caster);
	}

}
