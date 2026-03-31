package com.nisovin.magicspells.variables.variabletypes;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import com.nisovin.magicspells.util.BlockLocation;

public class Region<T> {

	private T value;
	private final Set<BlockLocation> blocks;

	public Region(T value) {
		this.value = value;
		this.blocks = new HashSet<>();
	}

	public Region(T value, Set<BlockLocation> blocks) {
		this.value = value;
		this.blocks = new HashSet<>(blocks);
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public Set<BlockLocation> getBlocks() {
		return Collections.unmodifiableSet(blocks);
	}

	public void addBlock(BlockLocation block) {
		blocks.add(block);
	}

	public void removeBlock(BlockLocation block) {
		blocks.remove(block);
	}

	public boolean isEmpty() {
		return blocks.isEmpty();
	}

	public int size() {
		return blocks.size();
	}

}
