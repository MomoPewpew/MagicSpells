package com.nisovin.magicspells.util.managers;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

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

    public List<Change> getByInternalName(String internalName) {
        List<Change> result = new ArrayList<>();
        for (AlteredBlock alteredBlock : alteredBlocks.values()) {
            for (Change change : alteredBlock.changes) {
                if (change.internalName().equals(internalName)) {
                    result.add(change);
                    break;
                }
            }
        }
        return result;
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
                    // Copy sign text - using getSide() for modern API compatibility
                    var fromSide = fromSign.getSide(org.bukkit.block.sign.Side.FRONT);
                    var toSide = toSign.getSide(org.bukkit.block.sign.Side.FRONT);
                    for (int i = 0; i < 4; i++) {
                        toSide.line(i, fromSide.line(i));
                    }
                    // Also copy back side
                    var fromBackSide = fromSign.getSide(org.bukkit.block.sign.Side.BACK);
                    var toBackSide = toSign.getSide(org.bukkit.block.sign.Side.BACK);
                    for (int i = 0; i < 4; i++) {
                        toBackSide.line(i, fromBackSide.line(i));
                    }
                }
                case org.bukkit.block.Container fromContainer when to instanceof org.bukkit.block.Container toContainer -> {
                    // Copy container contents
                    toContainer.getInventory().setContents(fromContainer.getInventory().getContents());
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
                    // For other block state types, we could use reflection or NBT copying
                    // but for now, we'll just log that we couldn't copy the state
                    // MagicSpells.log("Could not copy block state data for type: " + from.getClass().getSimpleName());
                }
            }
        }
    }
}