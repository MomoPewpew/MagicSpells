package com.nisovin.magicspells.castmodifiers.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;

public class LocationMatchesSchematicCondition extends Condition {

	private File file;
	private Clipboard clipboard;
	private boolean matchAir = false;

	@Override
	public boolean initialize(String var) {
		matchAir = var.endsWith(";true");

		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists())
			folder.mkdir();

		file = new File(folder, var.replace(";true", "").replace(";false", ""));
		if (!file.exists())
			return false;

		ClipboardFormat format = ClipboardFormats.findByFile(file);
		try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
			clipboard = reader.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return clipboard != null;
	}

	@Override
	public boolean checkCaster(SpellData data) {
		if (data.caster() == null) return false;
		return matchesSchematic(data.caster().getLocation());
	}

	@Override
	public boolean checkTarget(SpellData data) {
		if (data.target() == null) return false;
		return matchesSchematic(data.target().getLocation());
	}

	@Override
	public boolean checkLocation(SpellData data) {
		if (data.location() == null) return false;
		return matchesSchematic(data.location());
	}

	private boolean matchesSchematic(Location location) {
		BlockVector3 origin = this.clipboard.getOrigin();

		for (BlockVector3 pos : this.clipboard.getRegion()) {
			BlockVector3 pos_ = BlockVector3.at(pos.getX(), pos.getY(), pos.getZ());

			BlockData data = BukkitAdapter.adapt(this.clipboard.getBlock(pos_));

			Block bl = location.getBlock().getRelative(pos_.getX() - origin.getX(), pos_.getY() - origin.getY(),
					pos_.getZ() - origin.getZ());

			if (!matchAir && data.getMaterial().isAir())
				continue;

			if (!data.matches(bl.getBlockData())) {
				return false;
			}
		}

		return true;
	}

}
