package com.nisovin.magicspells.util.managers;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.block.Block;
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
        final BlockData originalState;
        final List<Change> changes = new ArrayList<>();

        public AlteredBlock(Change change) {
            this.originalState = change.from();
        }
    }

    public record Change(String internalName, Block block, BlockData from) {
        public void undo(boolean applyPhysics) {
            AlteredBlockManager manager = MagicSpells.getAlteredBlockManager();
            AlteredBlock alteredBlock = manager.alteredBlocks.get(block);
            if (alteredBlock != null && alteredBlock.changes.contains(this)) {
                if (alteredBlock.changes.size() == 1) {
                    block.setBlockData(alteredBlock.originalState, applyPhysics);
                    manager.alteredBlocks.remove(block);
                } else {
                    block.setBlockData(from, applyPhysics);
                    alteredBlock.changes.remove(this);
                }
            }
        }
    }
}