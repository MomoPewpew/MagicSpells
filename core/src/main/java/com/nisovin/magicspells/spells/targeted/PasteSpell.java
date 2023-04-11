package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileInputStream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;

public class PasteSpell extends TargetedSpell implements TargetedLocationSpell {

	private List<EditSession> sessions;
	private List<Builder> builders;

	private File file;
	private Clipboard clipboard;

	private ConfigData<Integer> yOffset;
	private ConfigData<Integer> undoDelay;
	private ConfigData<Integer> blocksPerInterval;

	private final int buildInterval;

	private boolean pasteAir;
	private boolean removePaste;
	private boolean pasteAtCaster;
	private boolean displayAnimation;
	private boolean playBlockBreakEffect;

	private BuilderTicker ticker;


	public PasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists()) folder.mkdir();
		String schematic = getConfigString("schematic", "none");
		file = new File(folder, schematic);
		if (!file.exists()) MagicSpells.error("PasteSpell " + spellName + " has non-existant schematic: " + schematic);

		yOffset = getConfigDataInt("y-offset", 0);
		undoDelay = getConfigDataInt("undo-delay", 0);
		blocksPerInterval = getConfigDataInt("blocks-per-interval", 10);

		buildInterval = getConfigInt("build-interval", 0);

		pasteAir = getConfigBoolean("paste-air", false);
		removePaste = getConfigBoolean("remove-paste", true);
		pasteAtCaster = getConfigBoolean("paste-at-caster", false);
		displayAnimation = getConfigBoolean("display-animation", true);
		playBlockBreakEffect = getConfigBoolean("play-block-break-effect", true);

		sessions = new ArrayList<EditSession>();
		builders = new ArrayList<Builder>();

		ticker = new BuilderTicker();
	}

	@Override
	public void initialize() {
		super.initialize();

		ClipboardFormat format = ClipboardFormats.findByFile(file);
		try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
			clipboard = reader.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (clipboard == null) MagicSpells.error("PasteSpell " + internalName + " has a wrong schematic!");
	}

	@Override
	public void turnOff() {
		for (EditSession session : sessions) {
			session.undo(session);
		}
		ticker.stop();
		sessions.clear();
		builders.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pasteAtCaster ? caster.getLocation().getBlock() : getTargetedBlock(caster, power, args);
			if (target == null) return noTarget(caster, args);
			Location loc = target.getLocation();
			boolean ok = castAtLocation(caster, loc, power, args);
			if (!ok) return noTarget(caster, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		boolean ok;

		if (buildInterval < 1) {
			ok = pasteInstant(caster, target, power, args);
		} else {
			ok = pasteOverTime(caster, target, power, args);
		}
		if (!ok) return false;
		if (caster != null) playSpellEffects(caster, target, power, args);
		else playSpellEffects(EffectPosition.TARGET, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

	private boolean pasteInstant(LivingEntity caster, Location target, float power, String[] args) {
		if (clipboard == null) return false;

		int yOffset = this.yOffset.get(caster, null, power, args);
		target.add(0, yOffset, 0);

		try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(target.getWorld()), -1)) {
			Operation operation = new ClipboardHolder(clipboard)
					.createPaste(editSession)
					.to(BlockVector3.at(target.getX(), target.getY(), target.getZ()))
					.ignoreAirBlocks(!pasteAir)
					.build();
			Operations.complete(operation);
			if (removePaste) sessions.add(editSession);

			int undoDelay = this.undoDelay.get(caster, null, power, args);
			if (undoDelay > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					editSession.undo(editSession);
					sessions.remove(editSession);
				}, undoDelay);
			}
		} catch (WorldEditException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private boolean pasteOverTime(LivingEntity caster, Location target, float power, String[] args) {
		try {
			builders.add(new Builder(caster, target, power, args));
			ticker.start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	class Builder {
        Map<BlockVector3, BlockData> blockDataMap = new HashMap<>();
		List<BlockVector3> handledVecs = new ArrayList<BlockVector3>();
		List<BuilderCrawler> builderCrawlers = new ArrayList<BuilderCrawler>();

        BlockVector3 origin;
        Location target;

    	int buildInterval;
    	int blocksPerInterval;

		public Builder(LivingEntity caster, Location target, float power, String[] args) {
			this.target = target;
			this.blocksPerInterval = PasteSpell.this.blocksPerInterval.get(caster, null, power, args);

			Clipboard cl = PasteSpell.this.clipboard;

	        this.origin = cl.getOrigin();

	        for (BlockVector3 pt : cl.getRegion()) {
	            BlockData blockData = BukkitAdapter.adapt(clipboard.getBlock(pt));

        		if (blockData.getMaterial() == Material.AIR) continue;

	            this.blockDataMap.put(pt, blockData);
	        }
		}

		public boolean build() {
			if (this.builderCrawlers.isEmpty()) {
				BlockVector3 vec = getNextBuilderCrawlerLocation();
				if (vec == null) return true;

				builderCrawlers.add(new BuilderCrawler(this, vec));
			}

			Iterator<BuilderCrawler> iterator = this.builderCrawlers.iterator();
			while (iterator.hasNext()) {
			    BuilderCrawler crawler = iterator.next();

			    crawler.build();

			    iterator.remove();
			}
			return false;
		}

		public BlockVector3 getNextBuilderCrawlerLocation() {
			double closestDistanceSq = 0;
			BlockVector3 closestVec = null;

	        for (BlockVector3 blockVec : blockDataMap.keySet()) {
	        	if (!handledVecs.contains(blockVec)) {
		        	double distanceX = blockVec.getX() - this.origin.getX();
		        	double distanceY = blockVec.getY() - this.origin.getY();
		        	double distanceZ = blockVec.getZ() - this.origin.getZ();
		        	double distanceSq = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ;

		        	if (closestVec == null || distanceSq < closestDistanceSq) {
		        		closestDistanceSq = distanceSq;
		        		closestVec = blockVec;
		        	}
        		}
			}
			return closestVec;
		}

		class BuilderCrawler {
			Builder builder;
			BlockVector3 vec;

			public BuilderCrawler(Builder builder, BlockVector3 vec) {
				this.builder = builder;
				this.vec = vec;
			}

			public void build() {
				Location location = this.builder.target.clone();
				location.add(this.vec.getX() - this.builder.origin.getX(), this.vec.getY() - this.builder.origin.getY(), this.vec.getZ() - this.builder.origin.getZ());
				MagicSpells.error("BuilderCrawler attempted to build at " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
				location.getBlock().setBlockData(this.builder.blockDataMap.get(this.vec));
				this.builder.handledVecs.add(this.vec);
			}
		}
	}

	private class BuilderTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicSpells.scheduleRepeatingTask(this, 0, buildInterval);
		}

		private void stop() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}

		@Override
		public void run() {
			for (Builder entry : new ArrayList<Builder>(builders)) {
				boolean remove = entry.build();
				if (remove) builders.remove(entry);
			}
			if (builders.isEmpty()) stop();
		}

	}
}
