package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class GeyserSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<BlockData> geyserType;

	private ConfigData<Double> damage;
	private ConfigData<Double> velocity;

	private ConfigData<Integer> geyserHeight;
	private ConfigData<Integer> animationSpeed;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean powerAffectsDamage;
	private boolean avoidDamageModification;

	public GeyserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigDataDouble("damage", 0);
		velocity = getConfigDataDouble("velocity", 10);

		geyserHeight = getConfigDataInt("geyser-height", 4);
		animationSpeed = getConfigDataInt("animation-speed", 2);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsDamage = getConfigBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);

		geyserType = getConfigDataBlockData("geyser-type", Material.WATER.createBlockData());
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data);
			if (target.noTarget()) return noTarget(data, target);

			data = data.builder().target(target.getTarget()).power(target.getPower()).build();
			boolean ok = geyser(data);
			if (!ok) return noTarget(data);

			playSpellEffects(data);
			sendMessages(data.caster(), target.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean geyser(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity  target = data.target();
		float power = data.power();
		double damage = this.damage.get(data);
		if (powerAffectsDamage) damage *= power;

		if (caster != null && checkPlugins && damage > 0) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, damage, this);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) damage = event.getDamage();
		}

		if (damage > 0) {
			if (ignoreArmor) {
				double health = target.getHealth() - damage;
				if (health < 0) health = 0;
				target.setHealth(health);
				if (caster != null) MagicSpells.getVolatileCodeHandler().playHurtAnimation(target, LocationUtil.getRotatedLocation(caster.getLocation(), target.getLocation()).getYaw());
				else MagicSpells.getVolatileCodeHandler().playHurtAnimation(target, target.getLocation().getYaw());
			} else {
				if (caster != null) target.damage(damage, caster);
				else target.damage(damage);
			}
		}

		double velocity = this.velocity.get(data) / 10;
		if (velocity > 0) target.setVelocity(new Vector(0, velocity * power, 0));

		int geyserHeight = this.geyserHeight.get(data);
		if (geyserHeight > 0) {
			List<Entity> allNearby = target.getNearbyEntities(50, 50, 50);
			allNearby.add(target);

			List<Player> playersNearby = new ArrayList<>();
			for (Entity e : allNearby) {
				if (!(e instanceof Player)) continue;
				playersNearby.add((Player) e);
			}

			int animationSpeed = this.animationSpeed.get(data);

			BlockData blockType = this.geyserType.get(data);
			new GeyserAnimation(blockType, target.getLocation(), playersNearby, animationSpeed, geyserHeight);
		}

		return true;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		geyser(data);
		playSpellEffects(data);
		return true;
	}

	private static class GeyserAnimation extends SpellAnimation {

		private final BlockData blockData;
		private final List<Player> nearby;
		private final int geyserHeight;
		private final Location start;

		private GeyserAnimation(BlockData blockData, Location start, List<Player> nearby, int animationSpeed, int geyserHeight) {
			super(0, animationSpeed, true, false);

			this.blockData = blockData;
			this.start = start;
			this.nearby = nearby;
			this.geyserHeight = geyserHeight;
		}

		@Override
		protected void onTick(int tick) {
			if (blockData == null) {
				stop();
				return;
			}

			if (tick > geyserHeight << 1) {
				stop();
				return;
			}

			if (tick < geyserHeight) {
				Block block = start.clone().add(0, tick, 0).getBlock();
				if (!BlockUtils.isAir(block.getType())) return;
				for (Player p : nearby) p.sendBlockChange(block.getLocation(), blockData);
				return;
			}

			int n = geyserHeight - (tick - geyserHeight) - 1;
			Block block = start.clone().add(0, n, 0).getBlock();
			for (Player p : nearby) p.sendBlockChange(block.getLocation(), block.getBlockData());
		}

	}

}
