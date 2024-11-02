package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class CarpetSpell extends TargetedSpell implements TargetedLocationSpell {

	private Map<Block, CarpetData> blocks;

	private Material material;
	private String materialName;

	private int touchCheckInterval;
	private ConfigData<Integer> radius;
	private ConfigData<Integer> duration;

	private boolean circle;
	private boolean removeOnTouch;
	private boolean powerAffectsRadius;

	private String spellOnTouchName;
	private Subspell spellOnTouch;

	private TouchChecker checker;
	
	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materialName = getConfigString("block", "white_carpet");
		material = Util.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicSpells.error("CarpetSpell '" + internalName + "' has an invalid block defined!");
			material = null;
		}

		radius = getConfigDataInt("radius", 1);
		duration = getConfigDataInt("duration", 0);
		touchCheckInterval = getConfigInt("touch-check-interval", 3);

		circle = getConfigBoolean("circle", false);
		removeOnTouch = getConfigBoolean("remove-on-touch", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);

		spellOnTouchName = getConfigString("spell-on-touch", "");

		blocks = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnTouch = new Subspell(spellOnTouchName);
		if (!spellOnTouch.process()) {
			if (!spellOnTouchName.isEmpty()) MagicSpells.error("CarpetSpell '" + internalName + "' has an invalid spell-on-touch defined!");
			spellOnTouch = null;
		}

		if (spellOnTouch != null) checker = new TouchChecker();
	}
	
	@Override
	public void turnOff() {
		super.turnOff();

		for (Block block : blocks.keySet()) {
			block.setType(Material.AIR);
		}
		blocks.clear();
		if (checker != null) checker.stop();
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL && data.caster() instanceof Player player) {
			Location loc = null;
			if (targetSelf) loc = player.getLocation();
			else {
				Block b = getTargetedBlock(player, data.power(), data.args());
				if (b != null && b.getType() != Material.AIR) loc = b.getLocation();
			}

			if (loc == null) return noTarget(data);

			layCarpet(data.builder().location(loc).build());
		}
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		if (!(data.caster() instanceof Player)) return false;
		if (targetSelf) layCarpet(data.builder().location(data.caster().getLocation()).build());
		else layCarpet(data);
		return true;
	}

	private void layCarpet(SpellData data) {
		LivingEntity player = data.caster();
		Location loc = data.location();
		float power = data.power();

		if (!loc.getBlock().getType().isOccluding()) {
			int c = 0;
			while (!loc.getBlock().getRelative(0, -1, 0).getType().isOccluding() && c <= 2) {
				loc.subtract(0, 1, 0);
				c++;
			}
		} else {
			int c = 0;
			while (loc.getBlock().getType().isOccluding() && c <= 2) {
				loc.add(0, 1, 0);
				c++;
			}
		}

		Block b;
		int y = loc.getBlockY();

		int rad = this.radius.get(data);
		if (powerAffectsRadius) rad = Math.round(rad * power);

		final List<Block> blockList = new ArrayList<>();
		for (int x = loc.getBlockX() - rad; x <= loc.getBlockX() + rad; x++) {
			for (int z = loc.getBlockZ() - rad; z <= loc.getBlockZ() + rad; z++) {
				b = loc.getWorld().getBlockAt(x, y, z);
				if (circle && loc.getBlock().getLocation().distanceSquared(b.getLocation()) > rad * rad) continue;

				if (b.getType().isOccluding()) b = b.getRelative(0, 1, 0);
				else if (!b.getRelative(0, -1, 0).getType().isOccluding()) b = b.getRelative(0, -1, 0);

				if (!BlockUtils.isAir(b.getType()) && !b.getRelative(0, -1, 0).getType().isSolid()) continue;

				b.setType(material, false);
				blockList.add(b);
				blocks.put(b, new CarpetData(data));
				playSpellEffects(EffectPosition.TARGET, b.getLocation().add(0.5, 0, 0.5), data);
			}
		}

		int duration = this.duration.get(data);
		if (duration > 0 && !blockList.isEmpty()) {
			MagicSpells.scheduleDelayedTask(() -> {
				for (Block b1 : blockList) {
					if (!material.equals(b1.getType())) continue;
					b1.setType(Material.AIR);
					if (blocks != null) blocks.remove(b1);
				}
			}, duration);
		}
		if (player != null) playSpellEffects(EffectPosition.CASTER, player, data);
	}

	private record CarpetData(SpellData data) {}
	
	private class TouchChecker implements Runnable {
		
		private int taskId;
		
		private TouchChecker() {
			taskId = MagicSpells.scheduleRepeatingTask(this, touchCheckInterval, touchCheckInterval);
		}
		
		@Override
		public void run() {
			if (blocks.isEmpty()) return;
			for (Player player : Bukkit.getOnlinePlayers()) {

				Block b = player.getLocation().getBlock();
				CarpetData data = blocks.get(b);

				if (data == null) continue;
				if (player.equals(data.data().caster())) continue;
				if (!material.equals(b.getType())) continue;

				if (removeOnTouch) {
					b.setType(Material.AIR);
					blocks.remove(b);
				}

				if (spellOnTouch != null) {
					SpellTargetEvent event = new SpellTargetEvent(CarpetSpell.this, data.data());
					if (!event.callEvent()) continue;

					spellOnTouch.subcast(data.data().builder().target(event.getTarget()).power(event.getPower()).build());
				}
			}
		}
		
		private void stop() {
			MagicSpells.cancelTask(taskId);
		}
		
	}

}
