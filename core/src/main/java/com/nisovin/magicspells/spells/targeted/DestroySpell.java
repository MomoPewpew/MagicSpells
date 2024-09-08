package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

	public static List<DestroyedBlock> destroyedBlocks;
	public Map<FallingBlock, DestroyedBlock> fallingDestroyedBlocks;

	private final Random random = ThreadLocalRandom.current();

	private Set<Material> blockTypesToThrow;
	private Set<Material> blockTypesToRemove;

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

	private VelocityType velocityType;

	public DestroySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		destroyedBlocks = new ArrayList<>();
		fallingDestroyedBlocks = new HashMap<>();

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
			if (fallingDestroyedBlocks.isEmpty())
				return;
			fallingDestroyedBlocks.keySet().removeIf(fallingBlock -> !fallingBlock.isValid());
		}, 600, 600);
	}

	@Override
	public void turnOff() {
		for (FallingBlock fb : fallingDestroyedBlocks.keySet()) {
			fb.remove();
		}

		fallingDestroyedBlocks.clear();

		for (DestroyedBlock b : destroyedBlocks) {
			b.undo(destroyedBlocks);

			destroyedBlocks.forEach(destroyedBlock -> {
				if (destroyedBlock.targetBlock != null && b.sourceBlock != null
						&& destroyedBlock.targetBlock.getLocation().equals(b.sourceBlock.getLocation()))
					destroyedBlock.targetBlock = null;
			});
		}
		destroyedBlocks.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block b = getTargetedBlock(caster, power, args);
			if (b != null && !BlockUtils.isAir(b.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, b.getLocation(), power,
						args);
				EventUtil.call(event);
				if (event.isCancelled())
					b = null;
				else
					b = event.getTargetLocation().getBlock();
			}
			if (b != null && !BlockUtils.isAir(b.getType())) {
				Location loc = b.getLocation().add(0.5, 0.5, 0.5);
				doIt(caster, null, caster.getLocation(), loc, power, args);
				playSpellEffects(caster, loc, power, args);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		doIt(caster, null, caster.getLocation(), target, power, args);
		playSpellEffects(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power,
			String[] args) {
		doIt(caster, target, from, target.getLocation(), power, args);
		playSpellEffects(caster, from, target, new SpellData(caster, target, power, args));
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		doIt(null, target, from, target.getLocation(), power, args);
		playSpellEffects(from, target, new SpellData(null, target, power, args));
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	private void doIt(LivingEntity caster, LivingEntity target, Location source, Location targetLocation, float power,
			String[] args) {
		int centerX = targetLocation.getBlockX();
		int centerY = targetLocation.getBlockY();
		int centerZ = targetLocation.getBlockZ();

		List<Block> blocksToThrow = new ArrayList<>();
		List<Block> blocksToRemove = new ArrayList<>();

		int vertRadius = this.vertRadius.get(caster, target, power, args);
		int horizRadius = this.horizRadius.get(caster, target, power, args);

		if (powerAffectsRadius) {
			vertRadius = Math.round(vertRadius * power);
			horizRadius = Math.round(horizRadius * power);
		}

		float throwChance = this.throwChance.get(caster, target, power, null) / 100;
		int duration = this.duration.get(caster, target, power, args);

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

		Map<Block, DestroyedBlock> destroyedBlocksByThis = new HashMap<>();

		for (Block b : blocksToRemove) {
			if (checkPlugins && caster instanceof Player) {
				MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(b, (Player) caster);
				EventUtil.call(event);
				if (event.isCancelled())
					continue;
			}

			DestroyedBlock db = new DestroyedBlock(internalName, b, b.getBlockData());

			destroyedBlocks.forEach(destroyedBlock -> {
				if (destroyedBlock.targetBlock != null
						&& destroyedBlock.targetBlock.getLocation().equals(b.getLocation())) {
					destroyedBlock.targetBlock = null;
					db.sourceBlock = null;
				}
			});

			if (duration > 0) {
				destroyedBlocks.add(db);
				destroyedBlocksByThis.put(b, db);

				MagicSpells.scheduleDelayedTask(() -> {
					if (destroyedBlocks.contains(db))
						db.undo(destroyedBlocks);

					destroyedBlocks.remove(b);

					destroyedBlocks.forEach(destroyedBlock -> {
						if (destroyedBlock.targetBlock != null
								&& destroyedBlock.targetBlock.getLocation().equals(b.getLocation()))
							destroyedBlock.targetBlock = null;
					});
				}, duration);
			}

			b.setType(Material.AIR, false);
		}

		double velocity = resolveVelocityPerBlock ? 0 : this.velocity.get(caster, target, power, args);
		float fallingBlockDamage = resolveDamagePerBlock ? 0 : this.fallingBlockDamage.get(caster, target, power, args);
		int fallingBlockHeight = resolveMaxHeightPerBlock ? 0
				: this.fallingBlockMaxHeight.get(caster, target, power, args);

		SpellData data = new SpellData(caster, target, power, args);
		for (Block b : blocksToThrow) {
			if (preventLandingBlocks && checkPlugins && caster instanceof Player) {
				MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(b, (Player) caster);
				EventUtil.call(event);
				if (event.isCancelled())
					continue;
			}

			BlockData blockData = b.getBlockData();

			DestroyedBlock db = new DestroyedBlock(internalName, b, blockData);

			destroyedBlocks.forEach(destroyedBlock -> {
				if (destroyedBlock.targetBlock != null
						&& destroyedBlock.targetBlock.getLocation().equals(b.getLocation())) {
					destroyedBlock.targetBlock = null;
					db.sourceBlock = null;
				}
			});

			if (duration > 0) {
				destroyedBlocks.add(db);
				destroyedBlocksByThis.put(b, db);

				MagicSpells.scheduleDelayedTask(() -> {
					if (destroyedBlocks.contains(db)) {
						if (db.undo(destroyedBlocks) && db.targetBlock != null)
							playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, db.targetBlock.getLocation(), power,
									args);

						destroyedBlocks.remove(b);

						destroyedBlocks.forEach(destroyedBlock -> {
							if (destroyedBlock.targetBlock != null
									&& destroyedBlock.targetBlock.getLocation().equals(b.getLocation()))
								destroyedBlock.targetBlock = null;
						});
					}
				}, duration);
			}

			Location l = b.getLocation().clone().add(0.5, 0.5, 0.5);
			FallingBlock fb = b.getWorld().spawn(l, FallingBlock.class);
			fb.setBlockData(blockData);

			fallingDestroyedBlocks.put(fb, destroyedBlocksByThis.get(b));

			fb.setDropItem(false);
			playSpellEffects(EffectPosition.PROJECTILE, fb, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, source, fb.getLocation(), null, fb,
					data);

			Vector v;
			if (resolveVelocityPerBlock)
				velocity = this.velocity.get(caster, target, power, args);
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
				fallingBlockDamage = this.fallingBlockDamage.get(caster, target, power, args);
			if (fallingBlockDamage > 0) {
				if (resolveMaxHeightPerBlock)
					fallingBlockHeight = this.fallingBlockMaxHeight.get(caster, target, power, args);
				MagicSpells.getVolatileCodeHandler().setFallingBlockHurtEntities(fb, fallingBlockDamage,
						fallingBlockHeight);
			}
			b.setType(Material.AIR, false);
		}

	}

	private class FallingBlockListener implements Listener {

		@EventHandler
		public void onBlockLand(EntityChangeBlockEvent event) {
			DestroyedBlock db = fallingDestroyedBlocks.get(event.getEntity());
			boolean removed = fallingDestroyedBlocks.keySet().remove(event.getEntity());

			if (removed) {
				event.setCancelled(true);
				if (!preventLandingBlocks && event.getBlock().getType() == Material.AIR) {
					event.getBlock().setBlockData(event.getBlockData(), false);
					if (db != null)
						db.targetBlock = event.getBlock();
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

	public class DestroyedBlock {

		public final String spellInternalName;
		public Block sourceBlock;
		public final BlockData blockData;
		public Block targetBlock;

		public DestroyedBlock(String spellInternalName, Block sourceBlock, BlockData blockData) {
			this.spellInternalName = spellInternalName;
			this.sourceBlock = sourceBlock;
			this.blockData = blockData;
		}

		public boolean undo(List<DestroyedBlock> destroyedBlocks) {
			List<DestroyedBlock> destroyedBlocksLandedOnSource = destroyedBlocks.stream()
					.filter(Objects::nonNull)
					.filter(destroyedBlock -> this.sourceBlock != null && destroyedBlock.targetBlock != null
							&& destroyedBlock.targetBlock.getLocation().equals(this.sourceBlock.getLocation()))
					.collect(Collectors.toList());

			if (sourceBlock != null
					&& ((sourceBlock.getBlockData() != null && (sourceBlock.getBlockData().getMaterial().isAir()
							|| sourceBlock.getBlockData().getMaterial().equals(Material.WATER)
							|| sourceBlock.getBlockData().getMaterial().equals(Material.LAVA)))
							|| !destroyedBlocksLandedOnSource.isEmpty())) {
				sourceBlock.setBlockData(blockData, false);

				destroyedBlocksLandedOnSource.forEach(destroyedBlock -> destroyedBlock.targetBlock = null);
			}

			if (targetBlock != null && targetBlock.getBlockData() != null
					&& targetBlock.getBlockData().equals(blockData)) {
				targetBlock.setType(Material.AIR, false);
				return true;
			}
			return false;
		}
	}

}
