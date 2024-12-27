package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import com.nisovin.magicspells.util.managers.AlteredBlockManager;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class DestroySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private final Random random = ThreadLocalRandom.current();

	private Set<Material> blockTypesToThrow;
	private Set<Material> blockTypesToRemove;
	private Map<FallingBlock, Long> fallingBlocks;

	private ConfigData<Integer> vertRadius;
	private ConfigData<Integer> horizRadius;
	private ConfigData<Integer> fallingBlockMaxHeight;
	private ConfigData<Integer> duration;

	private ConfigData<Double> velocity;

	private ConfigData<Float> fallingBlockDamage;
	private ConfigData<Float> throwChance;

	private boolean checkPlugins;
	private boolean preventLandingBlocks;
	private boolean resolveDamagePerBlock;
	private boolean resolveVelocityPerBlock;
	private boolean resolveMaxHeightPerBlock;
	private boolean powerAffectsRadius;
	private boolean affectsContainers;

	private VelocityType velocityType;

	public DestroySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fallingBlocks = new HashMap<>();

		vertRadius = getConfigDataInt("vert-radius", 3);
		horizRadius = getConfigDataInt("horiz-radius", 3);
		fallingBlockMaxHeight = getConfigDataInt("falling-block-max-height", 0);
		duration = getConfigDataInt("duration", 0);

		velocity = getConfigDataDouble("velocity", 0);

		fallingBlockDamage = getConfigDataFloat("falling-block-damage", 0);
		throwChance = getConfigDataFloat("throw-chance", 100F);

		checkPlugins = getConfigBoolean("check-plugins", true);
		preventLandingBlocks = getConfigBoolean("prevent-landing-blocks", false);
		resolveDamagePerBlock = getConfigBoolean("resolve-damage-per-block", false);
		resolveVelocityPerBlock = getConfigBoolean("resolve-velocity-per-block", false);
		resolveMaxHeightPerBlock = getConfigBoolean("resolve-max-height-per-block", false);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);
		affectsContainers = getConfigBoolean("affects-containers", false);

		String vType = getConfigString("velocity-type", "none");

		switch (vType) {
			case "up" -> velocityType = VelocityType.UP;
			case "random" -> velocityType = VelocityType.RANDOM;
			case "randomup", "random_up" -> velocityType = VelocityType.RANDOM_UP;
			case "down" -> velocityType = VelocityType.DOWN;
			case "toward" -> velocityType = VelocityType.TOWARD;
			case "away" -> velocityType = VelocityType.AWAY;
			default -> velocityType = VelocityType.NONE;
		}

		List<String> toThrow = getConfigStringList("block-types-to-throw", null);
		if (toThrow != null && !toThrow.isEmpty()) {
			blockTypesToThrow = EnumSet.noneOf(Material.class);
			for (String s : toThrow) {
				Material m = Util.getMaterial(s);
				if (m == null)
					continue;
				blockTypesToThrow.add(m);
			}
		}

		List<String> toRemove = getConfigStringList("block-types-to-remove", null);
		if (toRemove != null && !toRemove.isEmpty()) {
			blockTypesToRemove = EnumSet.noneOf(Material.class);
			for (String s : toRemove) {
				Material m = Util.getMaterial(s);
				if (m == null)
					continue;
				blockTypesToRemove.add(m);
			}
		}

		registerEvents(new FallingBlockListener());
		MagicSpells.scheduleRepeatingTask(() -> {
			if (fallingBlocks.isEmpty())
				return;
			fallingBlocks.keySet().removeIf(fallingBlock -> !fallingBlock.isValid());
		}, 600, 600);
	}

	@Override
	public void turnOff() {
		for (FallingBlock fb : fallingBlocks.keySet()) {
			fb.remove();
		}
		fallingBlocks.clear();

		for (AlteredBlockManager.Change change : MagicSpells.getAlteredBlockManager().getByInternalName(internalName)) {
			change.undo(false);
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity caster = data.caster();

			Block b = getTargetedBlock(data);
			if (b != null && !BlockUtils.isAir(b.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data);
				EventUtil.call(event);
				if (event.isCancelled())
					b = null;
				else
					b = event.getTargetLocation().getBlock();
			}
			if (b != null && !BlockUtils.isAir(b.getType())) {
				Location loc = b.getLocation().add(0.5, 0.5, 0.5);
				doIt(data.builder().location(caster.getLocation()).build(), data.target().getLocation());
				playSpellEffects(caster, loc, data);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		doIt(data, data.location());
		playSpellEffects(data);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		doIt(data, data.target().getLocation());
		playSpellEffects(data.caster(), data.location(), data.target(), data.builder().build());
		return true;
	}

	private void doIt(SpellData data, Location targetLocation) {
		LivingEntity caster = data.caster();
		Location source = data.location();
		float power = data.power();

		int centerX = targetLocation.getBlockX();
		int centerY = targetLocation.getBlockY();
		int centerZ = targetLocation.getBlockZ();

		List<Block> blocksToThrow = new ArrayList<>();
		List<Block> blocksToRemove = new ArrayList<>();

		int vertRadius = this.vertRadius.get(data);
		int horizRadius = this.horizRadius.get(data);

		if (powerAffectsRadius) {
			vertRadius = Math.round(vertRadius * power);
			horizRadius = Math.round(horizRadius * power);
		}

		float throwChance = this.throwChance.get(data) / 100;
		int duration = this.duration.get(data);

		for (int y = centerY - vertRadius; y <= centerY + vertRadius; y++) {
			for (int x = centerX - horizRadius; x <= centerX + horizRadius; x++) {
				for (int z = centerZ - horizRadius; z <= centerZ + horizRadius; z++) {
					Block b = targetLocation.getWorld().getBlockAt(x, y, z);
					if (b.getType() == Material.BEDROCK)
						continue;
					if (BlockUtils.isAir(b.getType()))
						continue;

					if (blockTypesToThrow != null) {
						if (blockTypesToThrow.contains(b.getType())) {
							if (throwChance < 1 && random.nextFloat() > throwChance) {
								blocksToRemove.add(b);
							} else {
								blocksToThrow.add(b);
							}
						} else if (blockTypesToRemove != null) {
							if (blockTypesToRemove.contains(b.getType())) {
								blocksToRemove.add(b);
							}
						} else if (!b.getType().isSolid())
							blocksToRemove.add(b);

						continue;
					}

					if (!affectsContainers && BlockUtils.isContainer(b)) continue;
					if (b.getType().isSolid()) {
						if (throwChance < 1 && random.nextFloat() > throwChance) {
							blocksToRemove.add(b);
						} else {
							blocksToThrow.add(b);
						}
					} else
						blocksToRemove.add(b);
				}
			}
		}

		for (Block b : blocksToRemove) {
			if (checkPlugins && caster instanceof Player) {
				MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(b, (Player) caster, bypassDippGen);
				EventUtil.call(event);
				if (event.isCancelled())
					continue;
			}

			if (duration > 0) {
				AlteredBlockManager.Change change = new AlteredBlockManager.Change(internalName, b, b.getBlockData());
				MagicSpells.getAlteredBlockManager().add(change);

				MagicSpells.scheduleDelayedTask(() -> change.undo(false), duration);
			}

			b.setType(Material.AIR, false);
		}

		double velocity = resolveVelocityPerBlock ? 0 : this.velocity.get(data);
		float fallingBlockDamage = resolveDamagePerBlock ? 0 : this.fallingBlockDamage.get(data);
		int fallingBlockHeight = resolveMaxHeightPerBlock ? 0
				: this.fallingBlockMaxHeight.get(data);

		for (Block b : blocksToThrow) {
			if (checkPlugins && caster instanceof Player) {
				MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(b, (Player) caster, bypassDippGen);
				EventUtil.call(event);
				if (event.isCancelled())
					continue;
			}

			BlockData blockData = b.getBlockData();

			if (duration > 0) {
				AlteredBlockManager.Change change = new AlteredBlockManager.Change(internalName, b, b.getBlockData());
				MagicSpells.getAlteredBlockManager().add(change);

				MagicSpells.scheduleDelayedTask(() -> change.undo(false), duration);
			}

			Location l = b.getLocation().clone().add(0.5, 0.5, 0.5);
			FallingBlock fb = b.getWorld().spawn(l, FallingBlock.class);
			fb.setBlockData(blockData);

			fallingBlocks.put(fb, duration > 0 ? System.currentTimeMillis() + (duration * 50L) : null);

			fb.setDropItem(false);
			playSpellEffects(EffectPosition.PROJECTILE, fb, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, source, fb.getLocation(), null, fb,
					data);

			Vector v;
			if (resolveVelocityPerBlock)
				velocity = this.velocity.get(data);
			if (velocityType == VelocityType.UP) {
				v = new Vector(0, velocity, 0);
				v.setY(v.getY() + ((Math.random() - 0.5) / 4));
			} else if (velocityType == VelocityType.RANDOM) {
				v = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
				v.normalize().multiply(velocity);
			} else if (velocityType == VelocityType.RANDOM_UP) {
				v = new Vector(Math.random() - 0.5, Math.random() / 2, Math.random() - 0.5);
				v.normalize().multiply(velocity);
				fb.setVelocity(v);
			} else if (velocityType == VelocityType.DOWN)
				v = new Vector(0, -velocity, 0);
			else if (velocityType == VelocityType.TOWARD)
				v = source.toVector().subtract(l.toVector()).normalize().multiply(velocity);
			else if (velocityType == VelocityType.AWAY)
				v = l.toVector().subtract(source.toVector()).normalize().multiply(velocity);
			else
				v = new Vector(0, (Math.random() - 0.5) / 4, 0);

			fb.setVelocity(v);

			if (resolveDamagePerBlock)
				fallingBlockDamage = this.fallingBlockDamage.get(data);
			if (fallingBlockDamage > 0) {
				if (resolveMaxHeightPerBlock)
					fallingBlockHeight = this.fallingBlockMaxHeight.get(data);
				MagicSpells.getVolatileCodeHandler().setFallingBlockHurtEntities(fb, fallingBlockDamage,
						fallingBlockHeight);
			}
			b.setType(Material.AIR, false);
		}

	}

	private class FallingBlockListener implements Listener {

		@EventHandler
		public void onBlockLand(EntityChangeBlockEvent event) {
			if (event.getEntity() instanceof FallingBlock fallingBlock) {
				if (fallingBlocks.containsKey(fallingBlock)) {
					Long endTime = fallingBlocks.get(fallingBlock);
					fallingBlocks.remove(fallingBlock);

					event.getEntity().remove();
					event.setCancelled(true);
					if (!preventLandingBlocks && event.getBlock().getType() == Material.AIR) {
						if (endTime != null) {
							long duration = (endTime - System.currentTimeMillis()) / 50;

							if (duration < 1) return;

							AlteredBlockManager.Change change = new AlteredBlockManager.Change(internalName, event.getBlock(), event.getBlock().getBlockData());
							MagicSpells.getAlteredBlockManager().add(change);

							MagicSpells.scheduleDelayedTask(() -> change.undo(false), duration);
						}
						event.getBlock().setBlockData(event.getBlockData(), false);
					}
				}
			}
		}
	}

	public enum VelocityType {

		NONE,
		UP,
		RANDOM,
		RANDOM_UP,
		DOWN,
		TOWARD,
		AWAY

	}

}
