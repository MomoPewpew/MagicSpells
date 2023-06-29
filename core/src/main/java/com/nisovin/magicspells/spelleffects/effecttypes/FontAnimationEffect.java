package com.nisovin.magicspells.spelleffects.effecttypes;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class FontAnimationEffect extends SpellEffect {

	private String fontName;

	private int ticksPerFrame;
	private int startFrame;
	private int totalFrames;

	private boolean fadeOut;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		fontName = config.getString("font-name", null);

		ticksPerFrame = config.getInt("ticks-per-frame", 1);
		startFrame = Math.max(Math.min(config.getInt("start-frame", 0), 999), 0);
		totalFrames = Math.max(Math.min(config.getInt("total-frames", 1), 1000 - startFrame), 1);

		fadeOut = config.getBoolean("fade-out", false);

		if ((startFrame + totalFrames) >= 1000) MagicSpells.error("A FontAnimationEffect is configured to go over the 1000 frame limit, so its total-frames have been adjusted.");
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if ((entity instanceof Player player)) new FontAnimation(player, fontName, ticksPerFrame, startFrame, totalFrames, fadeOut);

		return null;
	}

	private class FontAnimation implements Runnable {

		private Player target;

		private String fontName;

		private int ticksPerFrame;

		private boolean fadeOut;

		private ArrayList<String> list;

		private int iterations;
		private int fontAnimationTaskId;

		private FontAnimation(Player target, String fontName, int ticksPerFrame, int startFrame, int totalFrames, boolean fadeOut) {
			this.target = target;
			this.fontName = fontName;
			this.ticksPerFrame = ticksPerFrame;
			this.fadeOut = fadeOut;

			this.iterations = 0;
			this.list = new ArrayList<String>();

			for (int n = startFrame ; n < startFrame + totalFrames; n++) {
				String s = "\\uE";

				while ((s.length() + String.valueOf(n).length()) < 7) {
					s += "0";
				}

				s += n;

				list.add(s);
			}

			this.fontAnimationTaskId = MagicSpells.scheduleRepeatingTask(this, 0, this.ticksPerFrame);
		}

		@Override
		public void run() {
			if (this.iterations < this.list.size()) {
				Component message = Component.text(this.list.get(this.iterations++)).font(Key.key(this.fontName));
				this.target.sendActionBar(message);
			} else {
				if (!this.fadeOut) {
					Component message = Component.text("");
					this.target.sendActionBar(message);
				}
				MagicSpells.cancelTask(this.fontAnimationTaskId);
				return;
			}
		}
	}
}
