package com.nisovin.magicspells.util.managers;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AlteredBlockManager {

    private final Map<Block, AlteredBlock> alteredBlocks = new HashMap<>();

    public void add(Change change) {
        AlteredBlock alteredBlock = alteredBlocks.computeIfAbsent(change.block(), b -> new AlteredBlock(change));
        alteredBlock.changes.add(change);
    }

    /**
     * Captures the current block, applies {@code newData}, and registers a reversible {@link Change}.
     */
    public Change apply(String internalName, Block block, BlockData newData, boolean applyPhysics) {
        BlockData fromData = block.getBlockData();
        BlockState fromState = block.getState();
        block.setBlockData(newData, applyPhysics);
        Change change = new Change(internalName, block, fromData, fromState);
        add(change);
        return change;
    }

    /**
     * Captures the current block, applies {@code material}'s default BlockData, and registers a reversible {@link Change}.
     */
    public Change apply(String internalName, Block block, Material material, boolean applyPhysics) {
        return apply(internalName, block, material.createBlockData(), applyPhysics);
    }

    /**
     * Registers a reversible {@link Change} for a block that has already been modified
     * (e.g. by WorldEdit). Does not write to the world.
     */
    public Change register(String internalName, Block block, BlockData fromData, BlockState fromState) {
        Change change = new Change(internalName, block, fromData, fromState);
        add(change);
        return change;
    }

    public List<Change> getByInternalName(String internalName) {
        List<Change> result = new ArrayList<>();
        for (AlteredBlock alteredBlock : alteredBlocks.values()) {
            for (Change change : alteredBlock.changes) {
                if (change.internalName().equals(internalName)) {
                    result.add(change);
                }
            }
        }
        return result;
    }

    public List<Change> getByBlock(Block block) {
        AlteredBlock alteredBlock = alteredBlocks.get(block);
        if (alteredBlock == null) return Collections.emptyList();
        return new ArrayList<>(alteredBlock.changes);
    }

    public List<Change> getByBlockAndInternalName(Block block, String internalName) {
        AlteredBlock alteredBlock = alteredBlocks.get(block);
        if (alteredBlock == null) return Collections.emptyList();

        List<Change> result = new ArrayList<>();
        for (Change change : alteredBlock.changes) {
            if (change.internalName().equals(internalName)) {
                result.add(change);
            }
        }
        return result;
    }

    /**
     * Undoes every change registered under {@code internalName} (LIFO per block).
     */
    public void undoAll(String internalName, boolean applyPhysics) {
        List<Change> changes = getByInternalName(internalName);
        for (int i = changes.size() - 1; i >= 0; i--) {
            changes.get(i).undo(applyPhysics);
        }
    }

    public class AlteredBlock {
        final BlockData originalBlockData;
        final BlockState originalBlockState;
        final List<Change> changes = new ArrayList<>();

        public AlteredBlock(Change change) {
            this.originalBlockData = change.fromData();
            this.originalBlockState = change.fromState();
        }
    }

    public record Change(String internalName, Block block, BlockData fromData, BlockState fromState) {
        public void undo(boolean applyPhysics) {
            AlteredBlockManager manager = MagicSpells.getAlteredBlockManager();
            AlteredBlock alteredBlock = manager.alteredBlocks.get(block);
            if (alteredBlock != null && alteredBlock.changes.contains(this)) {
                if (alteredBlock.changes.size() == 1) {
                    // Restore to original state
                    restoreBlockState(alteredBlock.originalBlockData, alteredBlock.originalBlockState, applyPhysics);
                    manager.alteredBlocks.remove(block);
                } else {
                    // Restore to previous change state
                    restoreBlockState(fromData, fromState, applyPhysics);
                    alteredBlock.changes.remove(this);
                }
            }
        }

        private void restoreBlockState(BlockData blockData, BlockState blockState, boolean applyPhysics) {
            // First set the block data (material, rotation, etc.)
            block.setBlockData(blockData, applyPhysics);
            
            // Then restore the block state data (sign text, chest contents, etc.)
            if (blockState != null) {
                BlockState currentState = block.getState();
                
                // Copy the state data from the cached state to the current state
                if (blockState.getClass().equals(currentState.getClass())) {
                    copyBlockStateData(blockState, currentState);
                    currentState.update(true, applyPhysics);
                }
            }
        }

        private void copyBlockStateData(BlockState from, BlockState to) {
            // Handle different block state types
            switch (from) {
                case org.bukkit.block.Sign fromSign when to instanceof org.bukkit.block.Sign toSign -> {
                    // Copy front side text and formatting
                    var fromSide = fromSign.getSide(Side.FRONT);
                    var toSide = toSign.getSide(Side.FRONT);
                    for (int i = 0; i < 4; i++) {
                        toSide.line(i, fromSide.line(i));
                    }
                    toSide.setGlowingText(fromSide.isGlowingText());
                    toSide.setColor(fromSide.getColor());
                    // Copy back side text and formatting
                    var fromBackSide = fromSign.getSide(Side.BACK);
                    var toBackSide = toSign.getSide(Side.BACK);
                    for (int i = 0; i < 4; i++) {
                        toBackSide.line(i, fromBackSide.line(i));
                    }
                    toBackSide.setGlowingText(fromBackSide.isGlowingText());
                    toBackSide.setColor(fromBackSide.getColor());
                    // Copy waxed state
                    toSign.setWaxed(fromSign.isWaxed());
                }
                case org.bukkit.block.Container fromContainer when to instanceof org.bukkit.block.Container toContainer -> {
                    // Copy container contents
                    toContainer.getInventory().setContents(fromContainer.getInventory().getContents());
                }
                case org.bukkit.block.Lectern fromLectern when to instanceof org.bukkit.block.Lectern toLectern -> {
                    // Copy lectern book and page
                    toLectern.getInventory().setContents(fromLectern.getInventory().getContents());
                    toLectern.setPage(fromLectern.getPage());
                }
                case org.bukkit.block.CreatureSpawner fromSpawner when to instanceof org.bukkit.block.CreatureSpawner toSpawner -> {
                    // Copy spawner data
                    toSpawner.setSpawnedType(fromSpawner.getSpawnedType());
                    toSpawner.setDelay(fromSpawner.getDelay());
                    toSpawner.setMinSpawnDelay(fromSpawner.getMinSpawnDelay());
                    toSpawner.setMaxSpawnDelay(fromSpawner.getMaxSpawnDelay());
                    toSpawner.setSpawnCount(fromSpawner.getSpawnCount());
                    toSpawner.setMaxNearbyEntities(fromSpawner.getMaxNearbyEntities());
                    toSpawner.setRequiredPlayerRange(fromSpawner.getRequiredPlayerRange());
                    toSpawner.setSpawnRange(fromSpawner.getSpawnRange());
                }
                case org.bukkit.block.Banner fromBanner when to instanceof org.bukkit.block.Banner toBanner -> {
                    // Copy banner patterns
                    toBanner.setPatterns(fromBanner.getPatterns());
                }
                default -> {

                }
            }
        }
    }
}
