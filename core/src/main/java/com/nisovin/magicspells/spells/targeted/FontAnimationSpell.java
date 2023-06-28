package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class FontAnimationSpell extends TargetedSpell implements TargetedEntitySpell {

	private String fontName;

	private int ticksPerFrame;
	private int startFrame;
	private int totalFrames;

	private boolean fadeOut;

	public FontAnimationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fontName = config.getString("font-name", null);

		ticksPerFrame = config.getInt("ticks-per-frame", 1);
		startFrame = Math.max(Math.min(config.getInt("start-frame", 0), 999), 0);
		totalFrames = Math.max(Math.min(config.getInt("total-frames", 1), 1000 - startFrame), 1);

		fadeOut = config.getBoolean("fade-out", false);
	}

	@Override
	public void initialize() {
		if ((startFrame + totalFrames) >= 1000) MagicSpells.error("FontAnimationSpell '" + internalName + "' is configured to go over the 1000 frame limit, so its total-frames have been adjusted.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power, args);
			if (targetInfo.noTarget()) return noTarget(caster, args, targetInfo);
			Player target = targetInfo.target();
			sendFontAnimation(target, fontName, ticksPerFrame, startFrame, totalFrames);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!(target instanceof Player player)) return false;
		sendFontAnimation(player, fontName, ticksPerFrame, startFrame, totalFrames);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		return castAtEntity(null, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private void sendFontAnimation(Player target, String fontName, int ticksPerFrame, int startFrame, int totalFrames) {
		Iterator<String> iterator = getUnicodeCharacters(startFrame, totalFrames).iterator();

		int n = 0;
		while (iterator.hasNext()) {
			MagicSpells.scheduleDelayedTask(() -> {
				Component message = Component.text(iterator.next()).font(Key.key(fontName));
				target.sendActionBar(message);
			}, ticksPerFrame * n++);
		}

		if (!fadeOut) {
			MagicSpells.scheduleDelayedTask(() -> {
				Component message = Component.text(" ");
				target.sendActionBar(message);
			}, ticksPerFrame * n++);
		}
	}

	private static ArrayList<String> getUnicodeCharacters(int startFrame, int totalFrames) {
		ArrayList<String> list = new ArrayList<String>();

		for (int n = startFrame ; n < startFrame + totalFrames; n++) {
			String s = "\\uE";

			while ((s.length() + String.valueOf(n).length()) < 7) {
				s += "0";
			}

			s += n;

			list.add(s);
		}

		return list;
	}
}
