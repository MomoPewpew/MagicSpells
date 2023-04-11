package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileInputStream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
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

	private final int buildInterval;

	private boolean pasteAir;
	private boolean removePaste;
	private boolean pasteAtCaster;
	private boolean displayAnimation;
	private boolean playBlockBreakEffect;
	private boolean insideOut;


	public PasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists()) folder.mkdir();
		String schematic = getConfigString("schematic", "none");
		file = new File(folder, schematic);
		if (!file.exists()) MagicSpells.error("PasteSpell " + spellName + " has non-existant schematic: " + schematic);

		yOffset = getConfigDataInt("y-offset", 0);
		undoDelay = getConfigDataInt("undo-delay", 0);

		buildInterval = getConfigInt("build-interval", 0);

		pasteAir = getConfigBoolean("paste-air", false);
		removePaste = getConfigBoolean("remove-paste", true);
		pasteAtCaster = getConfigBoolean("paste-at-caster", false);
		displayAnimation = getConfigBoolean("display-animation", true);
		playBlockBreakEffect = getConfigBoolean("play-block-break-effect", true);
		insideOut = getConfigBoolean("inside-out", true);

		sessions = new ArrayList<EditSession>();
		builders = new ArrayList<Builder>();
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
		for (Builder builder : builders) {
			builder.stop = true;
		}
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
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	class Builder {
		List<BlockFace> CARDINAL_BLOCK_FACES = new ArrayList<BlockFace>(Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

	    final Block block;

	    Clipboard clipboard;

	    int workingBlocks = 0;
	    List<Block> handledBlocks = new ArrayList<>();

	    boolean stop = false;

		public Builder(LivingEntity caster, Location target, float power, String[] args) {
			this.block = target.getBlock();
			this.clipboard = PasteSpell.this.clipboard;

	        BlockVector3 origin = clipboard.getOrigin();

	        this.block.setBlockData(BukkitAdapter.adapt(clipboard.getBlock(origin)));
	        this.handledBlocks.add(this.block);

	        this.placeBlock(this.block, origin.getX(), origin.getY(), origin.getZ());
		}

		private void placeBlock(Block block, int x, int y, int z) {
			if (this.stop) return;
			Collections.shuffle(CARDINAL_BLOCK_FACES);
			for (BlockFace face : CARDINAL_BLOCK_FACES) {
				Block to = block.getRelative(face);
				if (handledBlocks.contains(to)) continue;

				BlockVector3 pos = BlockVector3.at(x + face.getModX(), y + face.getModY(), z + face.getModZ());
				BlockData data = BukkitAdapter.adapt(clipboard.getBlock(pos));

				if (data.getMaterial().isAir()) continue;

				this.handledBlocks.add(to);

				int duration = new Random().nextInt(8) + buildInterval;

				this.workingBlocks++;

				this.moveBlockEffects(block, data, face.getModX(), face.getModY(), face.getModZ(), duration);
				this.moveBlock(block, data, face.getModX(), face.getModY(), face.getModZ(), duration, true);

				MagicSpells.scheduleDelayedTask(() -> {
					to.setBlockData(data);
					this.placeBlock(to, pos.getX(), pos.getY(), pos.getZ());
				}, duration);

				this.workingBlocks--;
	        }
	    }

		private void moveBlock(Block block, BlockData data, int x, int y, int z, int duration, boolean keepOld) {
	        BlockDisplay ent = (BlockDisplay)block.getWorld().spawnEntity(block.getLocation(), EntityType.BLOCK_DISPLAY);
	        Block b = block.getRelative(x, y, z);
	        //var state = block.getState();

	        if (!keepOld) block.setType(Material.AIR);

	        ent.setBlock(data);
	        //ent.setBrightness(new Display.Brightness(block.getLightFromBlocks(), block.getLightFromSky()));
	        ent.setBrightness(new Display.Brightness(15, 15));
	        if (keepOld)
	        {
	            ent.setTransformation(new Transformation(new Vector3f(0.005f), new AxisAngle4f(), new Vector3f(0.955f), new AxisAngle4f()));
	            //var shape = getGeneralBlockShape(block.getBlockData());
	            /*if (data.getSoundGroup()..equals(block.getBlockData().getAsString()))
	                ent.setTransformation(new Transformation(new Vector3f(0.005f), new AxisAngle4f(), new Vector3f(0.955f), new AxisAngle4f()));
	            else
	                ent.setTransformation(new Transformation(new Vector3f(0.5f), new AxisAngle4f(), new Vector3f(), new AxisAngle4f()));*/
	        }
	        else ent.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1), new AxisAngle4f()));

			MagicSpells.scheduleDelayedTask(() -> {
	            ent.setInterpolationDelay(-1);
	            ent.setInterpolationDuration(duration);
	            ent.setTransformation(new Transformation(new Vector3f(x, y, z), new AxisAngle4f(), new Vector3f(1), new AxisAngle4f()));
			}, 2);

			MagicSpells.scheduleDelayedTask(() -> {
	            //b.setType(state.getType());
	            b.setBlockData(data);
	            //ent.remove();
	            b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5), data.getSoundGroup().getPlaceSound(), 0.2f, data.getSoundGroup().getPitch());
			}, duration + 2);

			MagicSpells.scheduleDelayedTask(() -> {
		            ent.remove();
			}, duration + 3);
	    }

	    private void moveBlockEffects(Block block, BlockData data, int x, int y, int z, int duration) {
	        Vector3f vec = new Vector3f(x, y, z);
	        vec.normalize();
	        vec = vec.sub(new Vector3f(0.1f));
	        Location loc = block.getLocation().add(0.5, 0.5, 0.5).add(vec.x, vec.y, vec.z);

	        for (int i = 0; i < duration / 6; i++) MagicSpells.scheduleDelayedTask(() ->
	        {
	            Block b = block.getRelative(x, y, z);
	            b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5), data.getSoundGroup().getBreakSound(), 0.1f, 0.1f);
	            block.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 5, 0.2, 0.2, 0.2, data);
	        }, i * 6);
	    }
	}
}
