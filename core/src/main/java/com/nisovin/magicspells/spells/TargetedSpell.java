package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.ValidTargetChecker;

public abstract class TargetedSpell extends InstantSpell {

	protected boolean targetSelf;
	protected boolean alwaysActivate;
	protected boolean playFizzleSound;
	
	protected String spellNameOnFail;
	protected Subspell spellOnFail;

	protected String strNoTarget;
	protected String strCastTarget;

	public TargetedSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetSelf = getConfigBoolean("target-self", false);
		alwaysActivate = getConfigBoolean("always-activate", false);
		playFizzleSound = getConfigBoolean("play-fizzle-sound", false);

		spellNameOnFail = getConfigString("spell-on-fail", "");

		strNoTarget = getConfigString("str-no-target", "");
		strCastTarget = getConfigString("str-cast-target", "");

	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (spellNameOnFail.isEmpty()) return;

		spellOnFail = new Subspell(spellNameOnFail);
		if (!spellOnFail.process()) {
			spellOnFail = null;
			MagicSpells.error("Spell '" + internalName + "' has an invalid spell-on-fail defined!");
		}
	}

	public void sendMessages(LivingEntity caster, LivingEntity target) {
		sendMessages(caster, target, null);
	}
	
	public void sendMessages(LivingEntity caster, LivingEntity target, String[] args) {
		String casterName = getTargetName(caster);
		String targetName = getTargetName(target);

		sendMessage(strCastSelf, caster, caster, target, args, "%a", casterName, "%t", targetName);
		sendMessage(strCastTarget, target, caster, target, args, "%a", casterName, "%t", targetName);
		sendMessageNear(caster, target, strCastOthers, args, "%a", casterName, "%t", targetName);
	}

	protected String getTargetName(LivingEntity target) {
		if (target instanceof Player) return target.getName();
		String name = MagicSpells.getEntityNames().get(target.getType());
		if (name != null) return name;
		return "unknown";
	}
	
	/**
	 * Checks whether two locations are within a certain distance from each other.
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @param range The maximum distance
	 * @return true if the distance is less than the range, false otherwise
	 */
	protected boolean inRange(Location loc1, Location loc2, int range) {
		return loc1.distanceSquared(loc2) < range * range;
	}
	
	/**
	 * Plays the fizzle sound if it is enabled for this spell.
	 */
	protected void fizzle(LivingEntity livingEntity) {
		if (!playFizzleSound || !(livingEntity instanceof Player player)) return;
		player.playEffect(livingEntity.getLocation(), Effect.EXTINGUISH, null);
	}

	@Override
	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data, boolean forceTargetPlayers, ValidTargetChecker checker) {
		if (targetSelf || validTargetList.canTargetSelf()) {
			SpellTargetEvent event = new SpellTargetEvent(this, data);
			return new TargetInfo<>(event.callEvent() ? event.getTarget() : null, event.getSpellData(), event.isCastCancelled());
		}

		return super.getTargetedEntity(data, forceTargetPlayers, checker);
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param data SpellData of spell
	 * @return the appropriate PostCastAction value
	 */
	protected PostCastAction noTarget(SpellData data) {
		return noTarget(data, strNoTarget, null);
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param data SpellData of spell
	 * @param message the message to send
	 * @return the appropriate PostCastAction value
	 */
	protected PostCastAction noTarget(SpellData data, String message) {
		return noTarget(data, message, null);
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param data SpellData of spell
	 * @param info targeting info
	 * @return the appropriate PostCastAction value
	 */
	protected PostCastAction noTarget(SpellData data, TargetInfo<?> info) {
		return noTarget(data, strNoTarget, info);
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param data SpellData of spell
	 * @param message the message to send
	 * @param info targeting info
	 * @return the appropriate PostCastAction value
	 */
	protected PostCastAction noTarget(SpellData data, String message, TargetInfo<?> info) {
		if (info != null && info.cancelled()) return PostCastAction.ALREADY_HANDLED;
		fizzle(data.caster());
		sendMessage(message, data.caster(), data.args());
		if (spellOnFail != null) spellOnFail.subcast(data.builder().power(info == null ? data.power() : info.getPower()).build());
		return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
	}

}
