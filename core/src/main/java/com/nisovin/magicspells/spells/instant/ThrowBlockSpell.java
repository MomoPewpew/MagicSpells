package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class ThrowBlockSpell extends InstantSpell implements TargetedLocationSpell {

	public static List<ThrownBlock> thrownBlocks;
	private Map<Entity, ThrownBlock> fallingBlocks;

	private Material material;

	private ConfigData<Integer> tntFuse;
	private ConfigData<Integer> duration;

	private ConfigData<Float> yOffset;
	private ConfigData<Float> velocity;
	private ConfigData<Float> rotationOffset;
	private ConfigData<Float> verticalAdjustment;

	private boolean dropItem;
	private boolean stickyBlocks;
	private boolean checkPlugins;
	private boolean preventBlocks;
	private boolean callTargetEvent;
	private boolean ensureSpellCast;
	private boolean powerAffectsDamage;
	private boolean projectileHasGravity;
	private boolean applySpellPowerToVelocity;

	private final String spellOnLandName;

	private Subspell spellOnLand;

	public ThrowBlockSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String materialName = getConfigString("block-type", "stone");
		if (materialName.toLowerCase().startsWith("primedtnt:")) {
			String[] split = materialName.split(":", 2);
			material = null;

			try {
				int fuse = Integer.parseInt(split[1]);
				tntFuse = data -> fuse;
			} catch (NumberFormatException e) {
				tntFuse = FunctionData.build(split[1], Double::intValue, 0);
				if (tntFuse == null)
					MagicSpells
							.error("Invalid tnt fuse '" + split[1] + "' for ThrowBlockSpell '" + internalName + "'.");
			}
		} else {
			material = Util.getMaterial(materialName);
			tntFuse = null;
		}

		duration = getConfigDataInt("duration", 0);

		rotationOffset = getConfigDataFloat("rotation-offset", 0F);

		yOffset = getConfigDataFloat("y-offset", 0F);
		velocity = getConfigDataFloat("velocity", 1);
		verticalAdjustment = getConfigDataFloat("vertical-adjustment", 0.5F);

		dropItem = getConfigBoolean("drop-item", false);
		stickyBlocks = getConfigBoolean("sticky-blocks", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		preventBlocks = getConfigBoolean("prevent-blocks", false);
		callTargetEvent = getConfigBoolean("call-target-event", true);
		ensureSpellCast = getConfigBoolean("ensure-spell-cast", true);
		powerAffectsDamage = getConfigBoolean("power-affects-damage", true);
		projectileHasGravity = getConfigBoolean("gravity", true);
		applySpellPowerToVelocity = getConfigBoolean("apply-spell-power-to-velocity", false);

		spellOnLandName = getConfigString("spell-on-land", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		if (material == null || !material.isBlock() && tntFuse == null) {
			MagicSpells.error("ThrowBlockSpell '" + internalName + "' has an invalid block-type defined!");
		}

		spellOnLand = new Subspell(spellOnLandName);
		if (!spellOnLand.process()) {
			if (!spellOnLandName.isEmpty())
				MagicSpells.error("ThrowBlockSpell '" + internalName + "' has an invalid spell-on-land defined!");
			spellOnLand = null;
		}

		thrownBlocks = new ArrayList<>();
		fallingBlocks = new HashMap<>();
		if (material != null)
			registerEvents(new ThrowBlockListener(this));
		else if (tntFuse != null)
			registerEvents(new TntListener());
	}

	@Override
	public void turnOff() {
		if (fallingBlocks != null) {
			for (ThrownBlock thrownBlock : thrownBlocks) {
				thrownBlock.undo();
			}
			for (Entity block : fallingBlocks.keySet()) {
				if (block != null)
					block.remove();
			}
			fallingBlocks.clear();
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			Vector v = getVector(data);
			Location l = data.caster().getEyeLocation().add(v);
			l.add(0, yOffset.get(data), 0);
			ThrownBlock info = spawnFallingBlock(data, v);
			playSpellEffects(EffectPosition.CASTER, data.caster(), info.data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		Vector v = getVector(data);
        assert data.location() != null;
        ThrownBlock info = spawnFallingBlock(data.builder().location(data.location().clone().add(0, yOffset.get(data), 0)).build(), v);
		playSpellEffects(EffectPosition.CASTER, data.target(), info.data);
		return true;
	}

	private Vector getVector(SpellData data) {
		Vector v = data.location().getDirection();

		float verticalAdjustment = this.verticalAdjustment.get(data);
		if (verticalAdjustment != 0)
			v.setY(v.getY() + verticalAdjustment);

		float rotationOffset = this.rotationOffset.get(data);
		if (rotationOffset != 0)
			Util.rotateVector(v, rotationOffset);

		float velocity = this.velocity.get(data);
		if (applySpellPowerToVelocity)
			velocity *= data.power();

		return v.normalize().multiply(velocity);
	}

	private ThrownBlock spawnFallingBlock(SpellData data, Vector velocity) {
		Entity entity = null;
		ThrownBlock info = new ThrownBlock(material.createBlockData(), data);

		Location location = data.location();
		LivingEntity caster = data.caster();

		if (material != null) {
			FallingBlock block = location.getWorld().spawn(location, FallingBlock.class);
			block.setBlockData(material.createBlockData());

			block.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, block, info.data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(),
					block.getLocation(), caster, block, info.data);
			block.setVelocity(velocity);
			block.setDropItem(dropItem);
			if (ensureSpellCast || stickyBlocks)
				new ThrowBlockMonitor(block, info);
			entity = block;

			int duration = this.duration.get(data);
			if (duration > 0) {
				thrownBlocks.add(info);
				MagicSpells.scheduleDelayedTask(() -> {
					if (fallingBlocks.containsKey(block) && block.isValid()) {
						block.remove();
						fallingBlocks.remove(block);
					}
					if (thrownBlocks.contains(info)) {
						info.undo();
						thrownBlocks.remove(info);
					}
				}, duration);
			}
		} else if (tntFuse != null) {
			TNTPrimed tnt = location.getWorld().spawn(location, TNTPrimed.class);
			tnt.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, tnt, info.data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(),
					tnt.getLocation(), caster, tnt, info.data);

			int tntFuse = this.tntFuse.get(data);
			tnt.setFuseTicks(tntFuse);
			tnt.setVelocity(velocity);
			entity = tnt;
		}

		if (entity == null)
			return info;

		if (fallingBlocks != null) {
			fallingBlocks.put(entity, info);
		}

		return info;
	}

	private class ThrowBlockMonitor implements Runnable {

		private FallingBlock block;
		private ThrownBlock info;
		private int task;
		private int counter = 0;

		private ThrowBlockMonitor(FallingBlock fallingBlock, ThrownBlock ThrownBlock) {
			block = fallingBlock;
			info = ThrownBlock;
			task = MagicSpells.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, 1);
		}

		@Override
		public void run() {
			if (stickyBlocks && !block.isDead()) {
				if (block.getVelocity().lengthSquared() < .01) {
					if (!preventBlocks) {
						Block b = block.getLocation().getBlock();
						if (b.getType() == Material.AIR)
							BlockUtils.setBlockFromFallingBlock(b, block, true);
					}
					if (!info.spellActivated && spellOnLand != null) {
						spellOnLand.subcast(info.data.builder().location(block.getLocation()).build());
						info.spellActivated = true;
					}
					block.remove();
				}
			}
			if (ensureSpellCast && block.isDead()) {
				if (!info.spellActivated && spellOnLand != null) {
					spellOnLand.subcast(info.data.builder().location(block.getLocation()).build());
					info.spellActivated = true;
				}
				MagicSpells.cancelTask(task);
			}
			if (counter++ > 1500)
				MagicSpells.cancelTask(task);
		}

	}

	private class ThrowBlockListener implements Listener {

		private ThrowBlockSpell thisSpell;

		private ThrowBlockListener(ThrowBlockSpell throwBlockSpell) {
			thisSpell = throwBlockSpell;
		}

		@EventHandler(ignoreCancelled = true)
		private void onDamage(EntityDamageByEntityEvent event) {
			ThrownBlock info;
			if (preventBlocks)
				info = fallingBlocks.get(event.getDamager());
			else
				info = fallingBlocks.remove(event.getDamager());
			if (info == null || !(event.getEntity() instanceof LivingEntity target))
				return;

			float power = info.data.power();
			if (callTargetEvent && info.data.caster() != null) {
				SpellTargetEvent evt = new SpellTargetEvent(thisSpell, info.data);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					event.setCancelled(true);
					return;
				}

				power = evt.getPower();
			}

			double damage = event.getDamage();
			if (powerAffectsDamage)
				damage *= power;

			if (checkPlugins && info.data.caster() != null) {
				MagicSpellsEntityDamageByEntityEvent evt = new MagicSpellsEntityDamageByEntityEvent(info.data.caster(), target,
						DamageCause.ENTITY_ATTACK, damage, ThrowBlockSpell.this);
				EventUtil.call(evt);
				if (evt.isCancelled()) {
					event.setCancelled(true);
					return;
				}
			}
			event.setDamage(damage);

			if (spellOnLand != null && !info.spellActivated) {
				spellOnLand.subcast(info.data.builder().location(target.getLocation()).power(power).build());
				info.spellActivated = true;
			}
		}

		@EventHandler(ignoreCancelled = true)
		private void onBlockLand(EntityChangeBlockEvent event) {
			ThrownBlock info = fallingBlocks.get(event.getEntity());
			boolean removed = fallingBlocks.keySet().remove(event.getEntity());

			if (removed) {
				event.getEntity().remove();
				event.setCancelled(true);
				if (!preventBlocks && event.getBlock().getType() == Material.AIR) {
					event.getBlock().setBlockData(event.getBlockData(), false);
					if (info != null)
						info.targetBlock = event.getBlock();
				}

				if (spellOnLand != null && info != null && !info.spellActivated) {
					spellOnLand.subcast(info.data.builder().location(event.getBlock().getLocation().add(0.5, 0.5, 0.5)).build());
					info.spellActivated = true;
				}
			}
		}

	}

	private class TntListener implements Listener {

		@EventHandler
		private void onExplode(EntityExplodeEvent event) {
			Entity entity = event.getEntity();
			ThrownBlock info = fallingBlocks.get(entity);
			if (info == null)
				return;
			if (preventBlocks) {
				event.blockList().clear();
				event.setYield(0F);
				event.setCancelled(true);
				event.getEntity().remove();
			}
			if (spellOnLand != null && !info.spellActivated) {
				spellOnLand.subcast(info.data);
				info.spellActivated = true;
			}
		}

	}

	public class ThrownBlock {

		private final SpellData data;

		private boolean spellActivated;

		public final BlockData blockData;
		public Block targetBlock;

		public ThrownBlock(BlockData blockData, SpellData data) {
			this.blockData = blockData;
			this.data = data;

			spellActivated = false;
		}

		public boolean undo() {
			if (targetBlock != null && targetBlock.getBlockData() != null
					&& targetBlock.getBlockData().equals(blockData)) {
				targetBlock.setType(Material.AIR, false);
				return true;
			}
			return false;
		}
	}

}
