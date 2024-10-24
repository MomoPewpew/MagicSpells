package com.nisovin.magicspells.spells.targeted;

import java.io.File;
import java.util.*;
import java.io.IOException;
import java.io.FileInputStream;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
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

	public class PasteSpellRecord {
		Map<String, List<Builder>> playerPastes;
		Map<String, List<EditSession>> playerSessions;

		public PasteSpellRecord(){
			this.playerPastes = new HashMap<>();
			this.playerSessions = new HashMap<>();
		}

		public void addPlayerPaste(String uuid, Builder builder){
			List<Builder> playerPastes = this.playerPastes.get(uuid);
			if(playerPastes == null)
				playerPastes = new ArrayList<>();
			playerPastes.add(builder);

			this.playerPastes.put(uuid, playerPastes);
		}

		public void addPlayerSession(String uuid, EditSession session){
			List<EditSession> playerPastes = this.playerSessions.get(uuid);
			if(playerPastes == null)
				playerPastes = new ArrayList<>();
			playerPastes.add(session);

			this.playerSessions.put(uuid, playerPastes);
		}

		public void removePlayerPaste(String uuid, Builder builder){
			List<Builder> playerPastes = this.playerPastes.get(uuid);
			if(playerPastes == null) return;
			playerPastes.remove(builder);
			this.playerPastes.put(uuid, playerPastes);
		}

		public void removePlayerSession(String uuid, EditSession session){
			List<EditSession> playerPastes = this.playerSessions.get(uuid);
			if(playerPastes == null) return;
			playerPastes.remove(session);
			this.playerSessions.put(uuid, playerPastes);
		}

		public void cleanAllBuilders(){
			playerPastes.forEach((uuid, builderList) -> {
				for(Builder builder : builderList){
					if(PasteSpell.this.removePaste && !builder.undone){
						builder.clipboard = builder.ogClipboard;
						builder.parseClipboard();
						if (builder.instantUndo || !builder.built) {
							builder.undoInstant();
						} else {
							builder.startBuilder();
						}
					}
				}
			});
			playerPastes.clear();
		}

		public void cleanPlayerBuilders(String uuid){
			List<Builder> playerBuilders = playerPastes.get(uuid);
			if(playerBuilders != null){
				for(Builder builder : playerBuilders){
					if(PasteSpell.this.removePaste && !builder.undone){
						builder.clipboard = builder.ogClipboard;
						builder.parseClipboard();
						if (builder.instantUndo || !builder.built) {
							builder.undoInstant();
						} else {
							builder.startBuilder();
						}
					}
				}
			}
			playerPastes.remove(uuid);
		}

		public void cleanAllSessions(){
			playerSessions.forEach((uuid, sessionList) -> {
				for(EditSession session : sessionList){
					session.undo(session);
				}
			});
			playerSessions.clear();
		}

		public void cleanPlayerSessions(String uuid){
			List<EditSession> sessionList = playerSessions.get(uuid);
			if(sessionList != null){
				for(EditSession session : sessionList){
					session.undo(session);
				}
			}
			playerPastes.remove(uuid);
		}
	}

	public static Map<String, PasteSpellRecord> spellRecords = new HashMap<>();

	private String spellName;
    private List<EditSession> sessions;
	private List<Builder> builders;
	private List<String> buildStartOffsetStrings;
	private List<String> dismantleStartOffsetStrings;
	private List<Vector3f> buildStartOffsets;
	private List<Vector3f> dismantleStartOffsets;

	private File file;
	private Clipboard clipboard;

	private ConfigData<Integer> yOffset;
	private ConfigData<Integer> undoDelay;
	private ConfigData<Integer> blocksPerCast;

	private final int buildInterval;
	private final int maxWorkingBlocks;
	private final int buildIntervalRandomness;

	private boolean pasteAir;
	private ConfigData<Boolean> onlyReplaceAir;
	private boolean removePaste;
	private boolean pasteAtCaster;
	private boolean displayAnimation;
	private boolean playBlockBreakEffect;
	private boolean dismantleFirst;
    private boolean instantUndo;


	public PasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellRecords.put(spellName, new PasteSpellRecord());

		this.spellName = spellName;

		File folder = new File(MagicSpells.plugin.getDataFolder(), "schematics");
		if (!folder.exists()) folder.mkdir();
		String schematic = getConfigString("schematic", "none");
		file = new File(folder, schematic);
		if (!file.exists()) MagicSpells.error("PasteSpell " + spellName + " has non-existant schematic: " + schematic);

		yOffset = getConfigDataInt("y-offset", 0);
		undoDelay = getConfigDataInt("undo-delay", 0);
		blocksPerCast = getConfigDataInt("blocks-per-cast", 0);

		buildInterval = getConfigInt("build-interval", 0);
		buildIntervalRandomness = Math.max(getConfigInt("build-interval-randomness", 7), 0) + 1;
		maxWorkingBlocks = getConfigInt("max-working-blocks", 1000);

		pasteAir = getConfigBoolean("paste-air", false);
		onlyReplaceAir = getConfigDataBoolean("only-replace-air", false);
		dismantleFirst = getConfigBoolean("dismantle-first", false);
		removePaste = getConfigInt("undo-delay", 0) > 0 ? true : getConfigBoolean("remove-paste", true);
		pasteAtCaster = getConfigBoolean("paste-at-caster", false);
		displayAnimation = getConfigBoolean("display-animation", true);
		playBlockBreakEffect = getConfigBoolean("play-block-break-effect", true);
        instantUndo = getConfigBoolean("instant-undo", false);

		buildStartOffsetStrings = getConfigStringList("build-start-offsets", null);
		dismantleStartOffsetStrings = getConfigStringList("dismantle-start-offsets", null);

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

		this.buildStartOffsets = new ArrayList<Vector3f>();
		if (this.buildStartOffsetStrings != null) {
			for (String s : this.buildStartOffsetStrings) {
				if (s.matches("^-?\\d+ -?\\d+ -?\\d+$")) {
				    String[] parts = s.split(" ");
				    int x = Integer.parseInt(parts[0]);
				    int y = Integer.parseInt(parts[1]);
				    int z = Integer.parseInt(parts[2]);
				    this.buildStartOffsets.add(new Vector3f(x, y, z));
				} else {
					MagicSpells.error("PasteSpell " + internalName + " has a wrong build-start-offset. The correct syntax is `- 1 2 3`");
				}
			}
		}

		this.dismantleStartOffsets = new ArrayList<Vector3f>();
		if (this.dismantleStartOffsetStrings != null) {
			for (String s : this.dismantleStartOffsetStrings) {
				if (s.matches("^-?\\d+ -?\\d+ -?\\d+$")) {
				    String[] parts = s.split(" ");
				    int x = Integer.parseInt(parts[0]);
				    int y = Integer.parseInt(parts[1]);
				    int z = Integer.parseInt(parts[2]);
				    this.dismantleStartOffsets.add(new Vector3f(x, y, z));
				} else {
					MagicSpells.error("PasteSpell " + internalName + " has a wrong dismantle-start-offset. The correct syntax is `- 1 2 3`");
				}
			}
		}
	}

	@Override
	public void turnOff() {
		for (EditSession session : sessions) {
			session.undo(session);
		}
		for (Builder builder : builders) {
			if (removePaste && !builder.undone) {
                builder.clipboard = builder.ogClipboard;
                builder.parseClipboard();
				builder.undoInstant();
			}
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

			int undoDelay = this.undoDelay.get(caster, null, power, args);

			if (removePaste) sessions.add(editSession);
			spellRecords.get(this.spellName).addPlayerSession(caster.getUniqueId().toString(), editSession);

			if (undoDelay > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					editSession.undo(editSession);
					sessions.remove(editSession);
					spellRecords.get(this.spellName).removePlayerSession(caster.getUniqueId().toString(), editSession);
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
			Builder builder = new Builder(caster, target, power, args);
			builders.add(builder);
			spellRecords.get(this.spellName).addPlayerPaste(caster.getUniqueId().toString(), builder);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void undoPastes(String spellName){
		PasteSpellRecord record = spellRecords.get(spellName);
		if(record == null) return;
		record.cleanAllBuilders();
		record.cleanAllSessions();
	}

	public static void undoPlayerPastes(String spellName, String uuid){
		PasteSpellRecord record = spellRecords.get(spellName);
		if(record == null) return;
		record.cleanPlayerBuilders(uuid);
		record.cleanPlayerSessions(uuid);
	}

	class Builder {
		private static List<BlockFace> CARDINAL_BLOCK_FACES = new ArrayList<BlockFace>(Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));

		private Location target;

	    private Clipboard clipboard;
	    private Clipboard ogClipboard;

		private int changedBlocks = 0;
		private int workingBlocks = 0;
	    private int workingAir = 0;
	    private int undoDelay;
		private int blocksPerCast;
        private boolean instantUndo;
        boolean built = false;
        boolean undone = false;

        private List<BlockVector3> blockVectors;
	    private List<BlockVector3> airVectors;
	    private List<BlockDisplay> blockDisplays;

	    boolean stop = false;
	    private LivingEntity caster;
		private boolean onlyReplaceAir;

		public Builder(LivingEntity caster, Location target, float power, String[] args) {
			this.target = target.clone();
			this.clipboard = PasteSpell.this.clipboard;
			this.caster = caster;
			this.onlyReplaceAir = PasteSpell.this.onlyReplaceAir.get(caster, null, power, args);

            this.undoDelay = PasteSpell.this.undoDelay.get(caster, null, power, args);
            this.blocksPerCast = PasteSpell.this.blocksPerCast.get(caster, null, power, args);
            this.instantUndo = PasteSpell.this.instantUndo;

			this.storeStartRegion();

			this.parseClipboard();

			startBuilder();
		}

		private void startBuilder(){

			BlockVector3 origin = this.clipboard.getOrigin();

			if (PasteSpell.this.dismantleFirst && PasteSpell.this.pasteAir) {
				for (BlockVector3 pos : this.blockVectors) {
					Block bl = this.target.getBlock().getRelative(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
					if (!bl.getBlockData().getMaterial().isAir()) {
						this.airVectors.add(pos);
					}
				}

				if (this.airVectors.size() > 0) this.firstWithdrawInit(origin);
				else if (this.blockVectors.size() > 0) this.firstBuildInit(origin);

			} else {
				if (this.blockVectors.size() > 0) this.firstBuildInit(origin);
				if (PasteSpell.this.pasteAir && this.airVectors.size() > 0) this.firstWithdrawInit(origin);
			}
		}

		private void storeStartRegion(){
			Region region = this.clipboard.getRegion();

			BlockVector3 minPos = region.getMinimumPoint();
			BlockVector3 maxPos = region.getMaximumPoint();
			BlockVector3 origin = this.clipboard.getOrigin();

			Block minBlock = this.target.getBlock().getRelative(minPos.getX() - origin.getX(), minPos.getY() - origin.getY(), minPos.getZ() - origin.getZ());
			Block maxBlock = this.target.getBlock().getRelative(maxPos.getX() - origin.getX(), maxPos.getY() - origin.getY(), maxPos.getZ() - origin.getZ());

			CuboidRegion cuboidRegion = new CuboidRegion(BukkitAdapter.adapt(this.target.getWorld()),
					BlockVector3.at(minBlock.getX(), minBlock.getY(), minBlock.getZ()),
					BlockVector3.at(maxBlock.getX(), maxBlock.getY(), maxBlock.getZ()));

			BlockArrayClipboard bAClipboard = new BlockArrayClipboard(cuboidRegion);
			EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(cuboidRegion.getWorld()).maxBlocks(-1).build();

			ForwardExtentCopy fec = new ForwardExtentCopy(session, cuboidRegion, bAClipboard, cuboidRegion.getMinimumPoint());
			try {
				Operations.complete(fec);
			} catch (WorldEditException e) {
				throw new RuntimeException(e);
			}
			bAClipboard.setOrigin(BlockVector3.at(this.target.getX(), this.target.getY(), this.target.getZ()));
			this.ogClipboard = bAClipboard;
		}

		private void firstBuildInit(BlockVector3 origin) {
			List<Vector3f> vecs = PasteSpell.this.buildStartOffsets;

			if (vecs.isEmpty()) {
				this.intialize(origin);
			} else {
				for (Vector3f vec : vecs) {
					this.intialize(BlockVector3.at(origin.getX() + vec.x, origin.getY() + vec.y, origin.getZ() + vec.z));
				}
			}
		}

		private void firstWithdrawInit(BlockVector3 origin) {
			List<Vector3f> vecs = PasteSpell.this.dismantleStartOffsets;

			if (vecs.isEmpty()) {
				this.intializeWithdraw(origin);
			} else {
				for (Vector3f vec : vecs) {
					this.intializeWithdraw(BlockVector3.at(origin.getX() + vec.x, origin.getY() + vec.y, origin.getZ() + vec.z));
				}
			}
		}

		private void parseClipboard() {
		    this.blockVectors = new ArrayList<BlockVector3>();
		    this.airVectors = new ArrayList<BlockVector3>();
		    this.blockDisplays = new ArrayList<BlockDisplay>();

	        BlockVector3 origin = this.clipboard.getOrigin();

	        for (BlockVector3 pos : this.clipboard.getRegion()) {
				BlockVector3 pos_ = BlockVector3.at(pos.getX(), pos.getY(), pos.getZ());

				BlockData data = BukkitAdapter.adapt(this.clipboard.getBlock(pos_));

				Block bl = this.target.getBlock().getRelative(pos_.getX() - origin.getX(), pos_.getY() - origin.getY(), pos_.getZ() - origin.getZ());

				if (this.onlyReplaceAir && !bl.getBlockData().getMaterial().isAir()) continue;

				if (!data.matches(bl.getBlockData())) {
					if (data.getMaterial().isAir()) {
						this.airVectors.add(pos_);
					} else {
						this.blockVectors.add(pos_);
					}
				}
	        }
		}

		private void intialize(BlockVector3 pos) {
	        int x = pos.getX() - this.clipboard.getOrigin().getX();
	        int y = pos.getY() - this.clipboard.getOrigin().getY();
	        int z = pos.getZ() - this.clipboard.getOrigin().getZ();

	        Location loc = this.target.clone().add(x, y, z);
			Block startingBlock = loc.getBlock();
			BlockData data = BukkitAdapter.adapt(this.clipboard.getBlock(pos));
			Block animatorBlock = null;
			BlockFace face = null;

			for (BlockFace f : CARDINAL_BLOCK_FACES) {
				Block b = startingBlock.getRelative(f);
				BlockData bd = b.getBlockData();
				if (bd.getMaterial().isSolid()) {
					animatorBlock = b;
					face = f;
					if (data.matches(bd)) break;
				}
			}

			if (animatorBlock == null) {
				if (this.stop || (this.blocksPerCast > 0 && this.changedBlocks >= this.blocksPerCast)) return;

				this.changedBlocks++;

				if (PasteSpell.this.playBlockBreakEffect) this.moveBlockEffects(startingBlock, data, 0, 0, 0, 0);
				startingBlock.setBlockData(data, false);

	            if (this.caster instanceof Player player) {
					BlockState previousState = startingBlock.getState();
					MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(startingBlock, previousState, startingBlock.getRelative(BlockFace.DOWN), player.getInventory().getItemInMainHand(), player, true);
					EventUtil.call(event);
	            }

		        this.placeBlock(startingBlock, pos.getX(), pos.getY(), pos.getZ());
			} else {
		        this.placeBlock(animatorBlock, pos.getX() + face.getModX(), pos.getY() + face.getModY(), pos.getZ() + face.getModZ());
			}
		}

		private void intializeWithdraw(BlockVector3 pos) {
	        int x = pos.getX() - this.clipboard.getOrigin().getX();
	        int y = pos.getY() - this.clipboard.getOrigin().getY();
	        int z = pos.getZ() - this.clipboard.getOrigin().getZ();

	        Location loc = this.target.clone().add(x, y, z);
			Block startingBlock = loc.getBlock();

			BlockFace face = null;

			for (BlockFace f : CARDINAL_BLOCK_FACES) {
				Block b = startingBlock.getRelative(f);
				BlockData bd = b.getBlockData();
				if (bd.getMaterial().isSolid()) {
					face = f;
					if (startingBlock.getBlockData().matches(bd)) break;
				}
			}

			this.withdrawBlock(startingBlock, pos.getX(), pos.getY(), pos.getZ(), face);
		}

		private void reInitialize() {
	        if (this.blockVectors.size() > 0) {
				for (int n = 0; n < Math.min(this.blockVectors.size(), 5); n++) {
	        		this.intialize(this.blockVectors.get(n));
				}
	        } else if (this.airVectors.isEmpty()) {
	        	this.finalise();
	        }
		}

		private void reInitializeWithdraw() {
	        if (this.airVectors.size() > 0) {
				for (int n = 0; n < Math.min(this.airVectors.size(), 5); n++) {
	        		this.intializeWithdraw(this.airVectors.get(n));
				}
			} else if (PasteSpell.this.dismantleFirst && !this.built) {
	        	this.parseClipboard();
    	        if (this.blockVectors.size() > 0) this.firstBuildInit(this.clipboard.getOrigin());
	        } else if (this.blockVectors.isEmpty()) {
	        	this.finalise();
	        }
		}

		private void finalise() {
			if (this.built) this.undone = true;
			this.built = true;

			this.blockDisplays = new ArrayList<BlockDisplay>();

			if (!this.undone && this.undoDelay > 0) {
				MagicSpells.scheduleDelayedTask(() ->{
					this.clipboard = this.ogClipboard;
					this.parseClipboard();
					if (this.instantUndo) {
						this.undoInstant();
					} else {
						this.startBuilder();
					}
				}, this.undoDelay);
			}
		}

		private void placeBlock(Block block, int x, int y, int z) {
			for (BlockFace face : CARDINAL_BLOCK_FACES) {
				if (this.stop || (this.blocksPerCast > 0 && this.changedBlocks >= this.blocksPerCast)) return;

				if ((this.workingBlocks + this.workingAir) > PasteSpell.this.maxWorkingBlocks) return;

				Block to = block.getRelative(face);
				BlockVector3 pos = BlockVector3.at(x + face.getModX(), y + face.getModY(), z + face.getModZ());
				if (!this.blockVectors.contains(pos)) continue;

				BlockData data = BukkitAdapter.adapt(this.clipboard.getBlock(pos));

		        this.blockVectors.remove(pos);

				int duration = new Random().nextInt(buildIntervalRandomness) + buildInterval;

				this.workingBlocks++;
				this.changedBlocks++;

				if (PasteSpell.this.playBlockBreakEffect) this.moveBlockEffects(block, data, face.getModX(), face.getModY(), face.getModZ(), duration);
				if (PasteSpell.this.displayAnimation) this.moveBlock(block, data, face.getModX(), face.getModY(), face.getModZ(), duration, true);

				MagicSpells.scheduleDelayedTask(() -> {
					if(this.stop) return;
					to.setBlockData(data, false);
					this.placeBlock(to, pos.getX(), pos.getY(), pos.getZ());
					this.workingBlocks--;

					if (this.workingBlocks < 1) {
						this.reInitialize();
					}
				}, duration);
	        }
			if (this.workingBlocks < 1) {
				this.reInitialize();
			}
	    }

		private void withdrawBlock(Block block, int x, int y, int z, BlockFace priorityFace) {
			if (this.stop || (this.blocksPerCast > 0 && this.changedBlocks >= this.blocksPerCast)) return;
			if ((this.workingBlocks + this.workingAir) > PasteSpell.this.maxWorkingBlocks) return;

			BlockVector3 currPos = BlockVector3.at(x, y, z);
			if (this.airVectors.contains(currPos)) {
				this.workingAir++;
				this.changedBlocks++;

				BlockData data = block.getBlockData();
				Block withdrawBlock = null;

				if (priorityFace != null) {
					withdrawBlock = block.getRelative(priorityFace);
					if (withdrawBlock.getBlockData().getMaterial().isAir()) {
						withdrawBlock = null;
					}
				}

				for (BlockFace face : CARDINAL_BLOCK_FACES) {
					this.airVectors.remove(currPos);

					Block to = block.getRelative(face);

					if (to.getBlockData().getMaterial().isAir()) continue;

					int duration = new Random().nextInt(buildIntervalRandomness) + buildInterval;

					if (withdrawBlock == null) {
						withdrawBlock = to;
					}

					if (to.getLocation().equals(withdrawBlock.getLocation())) {
						if (PasteSpell.this.playBlockBreakEffect) this.moveBlockEffects(block, data, face.getModX(), face.getModY(), face.getModZ(), duration);
						if (PasteSpell.this.displayAnimation) this.moveBlock(block, data, face.getModX(), face.getModY(), face.getModZ(), duration, false);

						MagicSpells.scheduleDelayedTask(() -> {
							this.workingAir--;

							if (this.workingAir < 1) {
								this.reInitializeWithdraw();
							}
						}, buildIntervalRandomness + buildInterval);

						MagicSpells.scheduleDelayedTask(() -> {
							this.withdrawBlock(to, x + face.getModX(), y + face.getModY(), z + face.getModZ(), face);
						}, duration);
					}
				}

				if (withdrawBlock == null) {
					if (PasteSpell.this.playBlockBreakEffect) this.moveBlockEffects(block, data, 0, 0, 0, 0);
					block.setType(Material.AIR);
					this.workingAir--;
				}
			}

			if (this.workingAir < 1) {
				this.reInitializeWithdraw();
			}
		}

		private void moveBlock(Block block, BlockData data, int x, int y, int z, int duration, boolean keepOld) {
	        BlockDisplay ent = (BlockDisplay)block.getWorld().spawnEntity(block.getLocation(), EntityType.BLOCK_DISPLAY);
	        Block b = block.getRelative(x, y, z);

	        if (!keepOld) {
	        	if (this.caster instanceof Player player) {
					MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(block, player);
					EventUtil.call(event);
					if (!event.isCancelled()) {
			        	block.setType(Material.AIR);
					};
				} else {
		        	block.setType(Material.AIR);
				}
	        }

	        ent.setBlock(data);

	        Block lightBlock = null;

			for (BlockFace face : CARDINAL_BLOCK_FACES) {
				lightBlock = block.getRelative(face);
	        	if (lightBlock.getBlockData().getMaterial().isAir()) break;
			}

	        ent.setBrightness(new Display.Brightness(lightBlock.getLightFromBlocks(), lightBlock.getLightFromSky()));
	        if (keepOld) ent.setTransformation(new Transformation(new Vector3f(0.005f), new AxisAngle4f(), new Vector3f(0.955f), new AxisAngle4f()));
	        else ent.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1), new AxisAngle4f()));

	        this.blockDisplays.add(ent);

			MagicSpells.scheduleDelayedTask(() -> {
	            ent.setInterpolationDelay(-1);
	            ent.setInterpolationDuration(duration);
	            ent.setTransformation(new Transformation(new Vector3f(x, y, z), new AxisAngle4f(), new Vector3f(1), new AxisAngle4f()));
			}, 2);

			if (keepOld) {
				MagicSpells.scheduleDelayedTask(() -> {
		            if(!this.stop){
						if (this.caster instanceof Player player) {
							BlockState previousState = b.getState();
							MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(b, previousState, block, player.getInventory().getItemInMainHand(), player, true);
							EventUtil.call(event);
							if (!event.isCancelled()) {
								b.setBlockData(data, false);
								b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5), data.getSoundGroup().getPlaceSound(), 0.2f, data.getSoundGroup().getPitch());
							}
						} else {
							b.setBlockData(data, false);
							b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5), data.getSoundGroup().getPlaceSound(), 0.2f, data.getSoundGroup().getPitch());
						}
					}
				}, duration + 2);
			}

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
	            block.getWorld().spawnParticle(Particle.BLOCK, loc, 5, 0.2, 0.2, 0.2, data);
	        }, i * 6);
	    }

        private boolean undoInstant() {
        	this.stop = true;
        	for (BlockDisplay ent : this.blockDisplays) {
        		if (ent != null && ent.isValid()) ent.remove();
        	}
            try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(target.getWorld()), -1)) {
                Operation operation = new ClipboardHolder(this.clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(target.getX(), target.getY(), target.getZ()))
                        .ignoreAirBlocks(!pasteAir)
                        .build();
                Operations.complete(operation);
            } catch (WorldEditException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }
	}
}
