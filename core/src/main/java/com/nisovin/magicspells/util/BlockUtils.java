package com.nisovin.magicspells.util;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Hangable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Bisected;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;

public class BlockUtils {

	public static List<Block> getNearbyBlocks(Location location, int radius, int height) {
		List<Block> blocks = new ArrayList<>();
		for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
			for (int y = location.getBlockY() - height; y <= location.getBlockY() + height; y++) {
				for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
					blocks.add(location.getWorld().getBlockAt(x, y, z));
				}
			}
		}
		return blocks;
	}

	public static boolean isTransparent(Spell spell, Block block) {
		return spell.getLosTransparentBlocks().contains(block.getType());
	}

	public static Block getTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			if (spell != null) return entity.getTargetBlock(spell.getLosTransparentBlocks(), range);
			return entity.getTargetBlock(MagicSpells.getTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}

	public static List<Block> getLastTwoTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			return entity.getLastTwoTargetBlocks(spell.getLosTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}

	public static void setTypeAndData(Block block, Material material, BlockData data, boolean physics) {
		block.setType(material);
		block.setBlockData(data, physics);
	}

	public static void setBlockFromFallingBlock(Block block, FallingBlock fallingBlock, boolean physics) {
		BlockData blockData = fallingBlock.getBlockData();
		block.setType(blockData.getMaterial());
		block.setBlockData(blockData, physics);
	}

	public static void setGrowthLevel(Block block, int level) {
		Ageable age = ((Ageable) block.getBlockData());
		age.setAge(level);
		block.setBlockData(age);
	}

	public static int getWaterLevel(Block block) {
		return ((Levelled) block.getBlockData()).getLevel();
	}

	public static int getGrowthLevel(Block block) {
		return ((Ageable) block.getBlockData()).getAge();
	}

	public static int getMaxGrowthLevel(Block block) {
		return ((Ageable) block.getBlockData()).getMaximumAge();
	}

	public static boolean isPlant(Block b) {
		return b.getBlockData() instanceof Ageable;
	}

	public static int getWaterLevel(BlockState blockState) {
		return ((Levelled) blockState.getBlockData()).getLevel();
	}

	public static boolean isChest(Block block) {
		return isChest(block.getType());
	}

	public static boolean isChest(Material m) {
		return switch (m) {
			case CHEST, TRAPPED_CHEST -> true;
			default -> false;
		};
	}

	public static boolean isLiquid(Material m) {
		return switch (m) {
			case WATER, LAVA -> true;
			default -> false;
		};

	}

	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}

	public static boolean isAir(ItemStack item) {
		return isAir(item.getType());
	}

	public static boolean isAir(Material m) {
		return m.isAir();
	}

	public static boolean isBed(Material m) {
		return m.name().contains("_BED");
	}

	public static boolean isDoor(Material m) {
		return m.name().contains("_DOOR");
	}

	public static boolean isButton(Material m) {
		return m.name().contains("_BUTTON");
	}

	public static boolean isPressurePlate(Material m) {
		return m.name().contains("_PRESSURE_PLATE");
	}

	public static boolean isTrapdoor(Material m) {
		return m.name().contains("_TRAPDOOR");
	}

	public static boolean isFenceGate(Material m) {
		return m.name().contains("_FENCE_GATE");
	}

	public static boolean isShulkerBox(Material m) {
		return m.name().contains("SHULKER_BOX");
	}

	public static boolean isWood(Material m) {
		return m.name().contains("_WOOD");
	}

	public static boolean isLog(Material m) {
		return m.name().contains("_LOG");
	}

	public static boolean isPathable(Material mat) {
		String name = mat.name();

		if (mat.isAir()) return true;
		if (isLiquid(mat)) return true;
		if (isPressurePlate(mat)) return true;
		if (isButton(mat)) return true;

		if (name.contains("SIGN")) return true;
		if (name.contains("CARPET")) return true;
		if (name.contains("TULIP")) return true;
		if (name.contains("SAPLING")) return true;
		if (name.contains("RAIL")) return true;
		if (name.contains("CORAL") && !name.endsWith("_BLOCK")) return true;

		return switch (mat) {
			case LIGHT, TALL_GRASS, LARGE_FERN, GRASS, DEAD_BUSH, FERN, SEAGRASS, TALL_SEAGRASS, LILY_PAD,
					DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET, OXEYE_DAISY, SUNFLOWER, LILAC, PEONY, ROSE_BUSH,
					BROWN_MUSHROOM, RED_MUSHROOM, TORCH, FIRE, REDSTONE_WIRE, WHEAT, LADDER, LEVER, REDSTONE_TORCH, SNOW,
					SUGAR_CANE, VINE, SCULK_VEIN, NETHER_WART -> true;
			default -> false;
		};
	}

	public static boolean isSafeToStand(Location location) {
		if (!isPathable(location.getBlock())) return false;
		if (!isPathable(location.add(0, 1, 0).getBlock())) return false;
		return !isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock());
	}

	public static void activatePowerable(Block block) {
		if (block.getBlockData() instanceof Powerable powerable) {
			powerable.setPowered(true);
			block.setBlockData(powerable, true);
		}

		if (block.getBlockData() instanceof AnaloguePowerable powerable) {
			powerable.setPower(powerable.getMaximumPower());
			block.setBlockData(powerable, true);
		}
	}

	public static void setBlockData(Block block, BlockData oldBlockData, BlockData newBlockData) {
		BlockData clone = newBlockData.clone();

		if (oldBlockData instanceof Directional && clone instanceof Directional) {
			if (((Directional) clone).getFaces().contains(((Directional) oldBlockData).getFacing())) {
				((Directional) clone).setFacing(((Directional) oldBlockData).getFacing());
			}
		}

		if (oldBlockData instanceof Stairs && clone instanceof Stairs) {
			((Stairs) clone).setShape(((Stairs) oldBlockData).getShape());
		}

		if (oldBlockData instanceof Slab && clone instanceof Slab) {
			((Slab) clone).setType(((Slab) oldBlockData).getType());
		}

		if (oldBlockData instanceof Hangable && clone instanceof Hangable) {
			((Hangable) clone).setHanging(((Hangable) oldBlockData).isHanging());
		}

		if (oldBlockData instanceof Orientable && clone instanceof Orientable) {
			if (((Orientable) clone).getAxes().contains(((Orientable) oldBlockData).getAxis())) {
				((Orientable) clone).setAxis(((Orientable) oldBlockData).getAxis());
			}
		}

		if (oldBlockData instanceof MultipleFacing && clone instanceof MultipleFacing) {
			for (BlockFace blockFace : ((MultipleFacing) oldBlockData).getFaces()) {
				if (((MultipleFacing) clone).getAllowedFaces().contains(blockFace)) {
					((MultipleFacing) clone).setFace(blockFace, true);
				}
			}
		}

		if (oldBlockData instanceof Bisected && clone instanceof Bisected) {
			((Bisected) clone).setHalf(((Bisected) oldBlockData).getHalf());
		}

		block.setBlockData(clone);
	}
}
