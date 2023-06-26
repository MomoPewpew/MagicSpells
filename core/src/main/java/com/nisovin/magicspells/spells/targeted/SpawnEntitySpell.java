package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.ai.LookAtEntityGoal;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.managers.AttributeManager;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class SpawnEntitySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private List<LivingEntity> entities;
	private final Map<LivingEntity, EntityPulser> pulsers;

	private EntityData entityData;

	private ItemStack mainHandItem;
	private ItemStack offHandItem;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private final ConfigData<Float> mainHandItemDropChance;
	private final ConfigData<Float> offHandItemDropChance;
	private final ConfigData<Float> helmetDropChance;
	private final ConfigData<Float> chestplateDropChance;
	private final ConfigData<Float> leggingsDropChance;
	private final ConfigData<Float> bootsDropChance;
	private final ConfigData<Float> yOffset;

	private ConfigData<Integer> duration;
	private ConfigData<Integer> fireTicks;
	private ConfigData<Integer> targetInterval;
	private final int spellInterval;

	private ConfigData<Double> targetRange;
	private ConfigData<Double> targetPriorityRange;
	private ConfigData<Double> retargetRange;

	private final String location;
	private final String nameplateText;

	private boolean noAI;
	private boolean gravity;
	private boolean removeAI;
	private boolean removeMob;
	private boolean invulnerable;
	private boolean useCasterName;
	private boolean addLookAtPlayerAI;
	private boolean allowSpawnInMidair;
	private boolean nameplateFormatting;
	private boolean cancelAttack;
	private boolean synchroniseIntervalSpells;

	private Subspell attackSpell;
	private String attackSpellName;

	private Subspell intervalSpell;
	private String intervalSpellName;
	private Subspell spellOnSpawn;
	private String spellOnSpawnName;

	private List<PotionEffect> potionEffects;
	private Set<AttributeManager.AttributeInfo> attributes;

	private Random random = ThreadLocalRandom.current();

	private final EntityPulserTicker ticker;

	private Set<String> mountList;
	private boolean removeMountsOnAnyDeath;

	// DEBUG INFO: level 2, invalid potion effect on internalname spell data
	public SpawnEntitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new ArrayList<>();

		ConfigurationSection entitySection = getConfigSection("entity");
		if (entitySection != null) entityData = new EntityData(entitySection);

		// Equipment
		MagicItem magicMainHandItem = MagicItems.getMagicItemFromString(getConfigString("main-hand", ""));
		if (magicMainHandItem != null) {
			mainHandItem = magicMainHandItem.getItemStack();
			if (mainHandItem != null && mainHandItem.getType().isAir()) mainHandItem = null;
		}

		MagicItem magicOffHandItem = MagicItems.getMagicItemFromString(getConfigString("off-hand", ""));
		if (magicOffHandItem != null) {
			offHandItem = magicOffHandItem.getItemStack();
			if (offHandItem != null && offHandItem.getType().isAir()) offHandItem = null;
		}

		MagicItem magicHelmetItem = MagicItems.getMagicItemFromString(getConfigString("helmet", ""));
		if (magicHelmetItem != null) {
			helmet = magicHelmetItem.getItemStack();
			if (helmet != null && helmet.getType().isAir()) helmet = null;
		}

		MagicItem magicChestplateItem = MagicItems.getMagicItemFromString(getConfigString("chestplate", ""));
		if (magicChestplateItem != null) {
			chestplate = magicChestplateItem.getItemStack();
			if (chestplate != null && chestplate.getType().isAir()) chestplate = null;
		}

		MagicItem magicLeggingsItem = MagicItems.getMagicItemFromString(getConfigString("leggings", ""));
		if (magicLeggingsItem != null) {
			leggings = magicLeggingsItem.getItemStack();
			if (leggings != null && leggings.getType().isAir()) leggings = null;
		}

		MagicItem magicBootsItem = MagicItems.getMagicItemFromString(getConfigString("boots", ""));
		if (magicBootsItem != null) {
			boots = magicBootsItem.getItemStack();
			if (boots != null && boots.getType().isAir()) boots = null;
		}

		if (mainHandItem != null) mainHandItem.setAmount(1);
		if (offHandItem != null) offHandItem.setAmount(1);
		if (helmet != null) helmet.setAmount(1);
		if (chestplate != null) chestplate.setAmount(1);
		if (leggings != null) leggings.setAmount(1);
		if (boots != null) boots.setAmount(1);

		mainHandItemDropChance = getConfigDataFloat("main-hand-drop-chance", 0);
		offHandItemDropChance = getConfigDataFloat("off-hand-drop-chance", 0);
		helmetDropChance = getConfigDataFloat("helmet-drop-chance", 0);
		chestplateDropChance = getConfigDataFloat("chestplate-drop-chance", 0);
		leggingsDropChance = getConfigDataFloat("leggings-drop-chance", 0);
		bootsDropChance = getConfigDataFloat("boots-drop-chance", 0);
		yOffset = getConfigDataFloat("y-offset", 0.1F);

		duration = getConfigDataInt("duration", 0);
		fireTicks = getConfigDataInt("fire-ticks", 0);
		targetInterval = getConfigDataInt("target-interval", -1);
		spellInterval = getConfigInt("spell-interval", 20);

		targetRange = getConfigDataDouble("target-range", 20);
		targetPriorityRange = getConfigDataDouble("target-priority-range", 10);
		retargetRange = getConfigDataDouble("retarget-range", 50);

		location = getConfigString("location", "target");
		nameplateText = getConfigString("nameplate-text", null);

		noAI = getConfigBoolean("no-ai", false);
		gravity = getConfigBoolean("gravity", true);
		removeAI = getConfigBoolean("remove-ai", false);
		removeMob = getConfigBoolean("remove-mob", true);
		invulnerable = getConfigBoolean("invulnerable", false);
		useCasterName = getConfigBoolean("use-caster-name", false);
		addLookAtPlayerAI = getConfigBoolean("add-look-at-player-ai", false);
		allowSpawnInMidair = getConfigBoolean("allow-spawn-in-midair", false);
		cancelAttack = getConfigBoolean("cancel-attack", true);
		synchroniseIntervalSpells = getConfigBoolean("synchronise-interval-spells", false);

		attackSpellName = getConfigString("attack-spell", "");
		intervalSpellName = getConfigString("interval-spell", "");
		spellOnSpawnName = getConfigString("spell-on-spawn", null);

		// Attributes
		// - [AttributeName] [Number] [Operation]
		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) attributes = MagicSpells.getAttributeManager().getAttributes(attributeList);

		List<String> list = getConfigStringList("potion-effects", null);
		if (list != null && !list.isEmpty()) {
			potionEffects = new ArrayList<>();

			String[] split;
			for (String data : list) {
				split = data.split(" ");
				try {
					PotionEffectType type = Util.getPotionEffectType(split[0]);
					if (type == null) throw new Exception("");

					int duration = 600;
					if (split.length > 1) duration = Integer.parseInt(split[1]);

					int strength = 0;
					if (split.length > 2) strength = Integer.parseInt(split[2]);
					boolean ambient = split.length > 3 && (split[3].equalsIgnoreCase("ambient") || split[3].equalsIgnoreCase("true"));
					boolean hidden = split.length > 4 && (split[4].equalsIgnoreCase("hidden") || split[4].equalsIgnoreCase("true"));
					potionEffects.add(new PotionEffect(type, duration, strength, ambient, !hidden));
				} catch (Exception e) {
					MagicSpells.error("SpawnMonsterSpell '" + spellName + "' has an invalid potion effect defined: " + data);
				}
			}
		}

		mountList = getConfigKeys("mounts");
		removeMountsOnAnyDeath = getConfigBoolean("remove-mounts-if-any-die", true);

		pulsers = new HashMap<>();
		ticker = new EntityPulserTicker();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (entityData == null || entityData.getEntityType() == null) {
			MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid entity defined!");
			entityData = null;
		}

		if (spellOnSpawnName != null) {
			spellOnSpawn = new Subspell(spellOnSpawnName);

			if (!spellOnSpawn.process()) {
				MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid spell-on-spawn '" + spellOnSpawnName + "' defined!");
				spellOnSpawn = null;
			}

			spellOnSpawnName = null;
		}

		if (!attackSpellName.isEmpty()) {
			attackSpell = new Subspell(attackSpellName);

			if (!attackSpell.process()) {
				MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid attack-spell defined!");
				attackSpell = null;
			}
		}

		if (!intervalSpellName.isEmpty()) {
			intervalSpell = new Subspell(intervalSpellName);

			if (!intervalSpell.process()) {
				MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid interval-spell defined!");
				intervalSpell = null;
			}
		}

		attackSpellName = null;
	}

	@Override
	public void turnOff() {
		Iterator<LivingEntity> it = entities.iterator();
		while (it.hasNext()) {
			Entity entity = it.next();

			it.remove();
			entity.remove();
		}
		ticker.stop();
		entities.clear();
		pulsers.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			LivingEntity target = null;

			switch (location.toLowerCase()) {
				case "focus" -> {
					loc = getRandomLocationFrom(caster.getLocation(), 3);
					TargetInfo<LivingEntity> info = getTargetedEntity(caster, power, args);
					if (info.noTarget()) return noTarget(caster, args, info);
					target = info.target();
					power = info.power();
				}
				case "target" -> {
					Block block = getTargetedBlock(caster, power, args);
					if (block != null && !block.getType().isAir()) {
						if (BlockUtils.isPathable(block)) loc = block.getLocation();
						else if (BlockUtils.isPathable(block.getRelative(BlockFace.UP))) loc = block.getRelative(BlockFace.UP).getLocation();
					}
				}
				case "caster" -> loc = caster.getLocation();
				case "random" -> loc = getRandomLocationFrom(caster.getLocation(), getRange(power));
				case "casteroffset" -> {
					String[] split = location.split(":");
					float y = Float.parseFloat(split[1]);
					loc = caster.getLocation().add(0, y, 0);
					loc.setPitch(0);
				}
			}

			if (loc == null || !spawnMob(caster, caster.getLocation(), loc, target, power, args))
				return noTarget(caster, args);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return switch (location.toLowerCase()) {
			case "target" -> spawnMob(caster, caster.getLocation(), target, null, power, args);
			case "caster" -> spawnMob(caster, caster.getLocation(), caster.getLocation(), null, power, args);
			case "random" -> {
				Location loc = getRandomLocationFrom(target, getRange(power));
				yield loc != null && spawnMob(caster, caster.getLocation(), loc, null, power, args);
			}
			case "offset" -> {
				String[] split = location.split(":");
				float y = Float.parseFloat(split[1]);
				Location loc = target.clone().add(0, y, 0);
				loc.setPitch(0);

				yield spawnMob(caster, caster.getLocation(), loc, null, power, args);
			}
			default -> false;
		};
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return switch (location.toLowerCase()) {
			case "target", "caster" -> spawnMob(null, target, target, null, power, args);
			case "random" -> {
				Location loc = getRandomLocationFrom(target, getRange(power));
				yield loc != null && spawnMob(null, target, loc, null, power, args);
			}
			case "offset" -> {
				String[] split = location.split(":");
				float y = Float.parseFloat(split[1]);
				Location loc = target.clone().add(0, y, 0);
				loc.setPitch(0);

				yield spawnMob(null, target, loc, null, power, args);
			}
			default -> false;
		};
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		if (location.equals("focus")) spawnMob(caster, from, from, target, power, args);
		else castAtLocation(caster, from, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		if (location.equals("focus")) spawnMob(null, from, from, target, power, args);
		else castAtLocation(from, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	private Location getRandomLocationFrom(Location location, int range) {
		World world = location.getWorld();
		int x;
		int y;
		int z;
		int attempts = 0;
		Block block;
		Block block2;

		while (attempts < 10) {
			x = location.getBlockX() + random.nextInt(range << 1) - range;
			y = location.getBlockY() + 2;
			z = location.getBlockZ() + random.nextInt(range << 1) - range;

			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.WATER) return block.getLocation();
			if (BlockUtils.isPathable(block)) {
				if (allowSpawnInMidair) return block.getLocation();
				int c = 0;
				while (c < 5) {
					block2 = block.getRelative(BlockFace.DOWN);
					if (BlockUtils.isPathable(block2)) block = block2;
					else return block.getLocation();
					c++;
				}
			}

			attempts++;
		}
		return null;
	}

	private boolean spawnMob(LivingEntity caster, Location source, Location loc, LivingEntity target, float power, String[] args) {
		if (entityData == null) return false;
		SpellData data = new SpellData(caster, target, power, args);

		loc.add(0, yOffset.get(data), 0);

		LivingEntity entity = (LivingEntity) entityData.spawn(loc, data, mob -> prepMob(caster, target, mob, power, args));

		if(mountList != null && !mountList.isEmpty()) createMounts(caster, target, power, args, entity);

		int duration = this.duration.get(caster, target, power, args);
		if (attackSpell != null) {
			AttackMonitor monitor = new AttackMonitor(caster, entity, target, power, args);
			MagicSpells.registerEvents(monitor);

			MagicSpells.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration > 0 ? duration : 12000);
		}

		if (caster != null) playSpellEffects(caster, source, entity, power, args);
		else playSpellEffects(source, entity, power, args);
		if (entity == null) return false;

		entities.add(entity);

		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
				if(mountList != null && !mountList.isEmpty()){
					//Removing the mounts of the entity is removed
					Entity _riding = entity.getVehicle();
					while(_riding != null){
						Entity _prev = _riding;
						_riding = _riding.getVehicle();
						_prev.remove();
					}
				}
				entity.remove();
				entities.remove(entity);
			}, duration);
		}
		if (intervalSpell != null && spellInterval > 0) {
			ticker.start();
			pulsers.put(entity, new EntityPulser(caster, entity, power, args));
			if (duration > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					pulsers.remove(entity);
				}, duration);
			}
		}

		if (spellOnSpawn != null) {
			if (entity instanceof LivingEntity livingEntity) spellOnSpawn.subcast(caster, livingEntity, power, args);
			else spellOnSpawn.subcast(caster, entity.getLocation(), power, args);
		}

		int targetInterval = this.targetInterval.get(data);
		if (targetInterval > 0 && entity instanceof Mob mob) new Targeter(caster, mob, power, args, targetInterval);

		if (attackSpell != null) {
			AttackMonitor monitor = new AttackMonitor(caster, entity, target, power, args);
			MagicSpells.registerEvents(monitor);

			if (duration > 0) MagicSpells.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration);
		}

		if (caster != null) playSpellEffects(caster, source, entity, data);
		else playSpellEffects(source, entity, data);

		return true;
	}

	private void prepMob(LivingEntity caster, LivingEntity target, Entity entity, float power, String[] args) {
		SpellData data = new SpellData(caster, target, power, args);

		entity.setGravity(gravity);
		entity.setInvulnerable(invulnerable);

		int fireTicks = this.fireTicks.get(data);
		if (fireTicks > 0) entity.setFireTicks(fireTicks);

		if (useCasterName && caster != null) {
			if (caster instanceof Player player) entity.customName(player.displayName());
			else entity.customName(caster.name());
			entity.setCustomNameVisible(true);
		} else if (nameplateText != null) {
			entity.customName(Util.getMiniMessage(MagicSpells.doReplacements(nameplateText, caster, target, args)));
			entity.setCustomNameVisible(true);
		}

		if (entity instanceof Enderman enderman && mainHandItem != null) {
			ItemMeta meta = mainHandItem.getItemMeta();

			if (meta instanceof BlockDataMeta blockMeta)
				enderman.setCarriedBlock(blockMeta.getBlockData(mainHandItem.getType()));
		}

		if (entity instanceof LivingEntity livingEntity) {
			EntityEquipment equipment = livingEntity.getEquipment();
			if (equipment != null) {
				equipment.setItemInMainHand(mainHandItem);
				equipment.setItemInOffHand(offHandItem);
				equipment.setHelmet(helmet);
				equipment.setChestplate(chestplate);
				equipment.setLeggings(leggings);
				equipment.setBoots(boots);

				equipment.setItemInMainHandDropChance(mainHandItemDropChance.get(data) / 100);
				equipment.setItemInOffHandDropChance(offHandItemDropChance.get(data) / 100);
				equipment.setHelmetDropChance(helmetDropChance.get(data) / 100);
				equipment.setChestplateDropChance(chestplateDropChance.get(data) / 100);
				equipment.setLeggingsDropChance(leggingsDropChance.get(data) / 100);
				equipment.setBootsDropChance(bootsDropChance.get(data) / 100);
			}

			if (potionEffects != null) livingEntity.addPotionEffects(potionEffects);
			if (attributes != null) MagicSpells.getAttributeManager().addEntityAttributes(livingEntity, attributes);

			if (removeAI) {
				if (addLookAtPlayerAI && livingEntity instanceof Mob mob) {
					MobGoals mobGoals = Bukkit.getMobGoals();

					mobGoals.removeAllGoals(mob);
					mobGoals.addGoal(mob, 1, new LookAtEntityGoal(mob, HumanEntity.class, 10F, 1F));
				} else livingEntity.setAI(false);
			}

			if (noAI) livingEntity.setAI(false);
			if (livingEntity instanceof Mob mob) mob.setTarget(target);
		}
	}

	private void createMounts(LivingEntity caster, LivingEntity target, float power, String[] args, LivingEntity head){

		List<Entity> ents = new ArrayList<>();

		//All need to be unique names
		for(String sectionName : mountList){
			ConfigurationSection section = getConfigSection("mounts." + sectionName);
			EntityData mountData = new EntityData(section);
			Entity mount = mountData.spawn(head.getLocation(), e -> {
				{
					LivingEntity preSpawned = (LivingEntity) e;
					prepMob(caster, target, preSpawned, power, args);

					int fireTicks = this.fireTicks.get(caster, target, power, args);
					if (fireTicks > 0) preSpawned.setFireTicks(fireTicks);
					if (potionEffects != null) preSpawned.addPotionEffects(potionEffects);

					// Apply attributes
					if (attributes != null) MagicSpells.getAttributeManager().addEntityAttributes(preSpawned, attributes);

					if (removeAI) {
						if (addLookAtPlayerAI) {
							if (preSpawned instanceof Mob mob) {
								MobGoals mobGoals = Bukkit.getMobGoals();
								mobGoals.removeAllGoals(mob);
								mobGoals.addGoal(mob, 1, new LookAtEntityGoal(mob, HumanEntity.class, 10.0F, 1.0F));
							}
						} else {
							preSpawned.setAI(false);
						}
					}
					preSpawned.setAI(!noAI);
					preSpawned.setInvulnerable(invulnerable);

					if (target != null) MobUtil.setTarget(preSpawned, target);
				}
			});
			ents.add(mount);
		}

		for(int i = ents.size()-1; i > 0; i--){
			ents.get(i).addPassenger(ents.get(i-1));
		}
		ents.get(0).addPassenger(head);
	}

	@EventHandler
	private void onEntityDeath(EntityDeathEvent event) {
		entityDeath(event.getEntity());
	}

	@EventHandler
	private void onExplode(EntityExplodeEvent event) {
		if (event.getEntity() instanceof LivingEntity) entityDeath((LivingEntity) event.getEntity());
	}

	private void onEntityRemove(EntityRemoveFromWorldEvent event) {
		if (removeMob && event.getEntity() instanceof LivingEntity) entityDeath((LivingEntity) event.getEntity());
	}

	private void entityDeath(LivingEntity entity) {
		if (!entities.contains(entity)) {
			if(!removeMountsOnAnyDeath) return;
			List<Entity> forRemoval = new ArrayList<>();
			Entity _ent = entity;
			Entity _riding = entity.getVehicle();

			while(_ent.getPassengers().size() > 0){
				forRemoval.add(_ent);
				_ent = _ent.getPassengers().get(0);
			}
			while(_riding != null){
				forRemoval.add(_riding);
				_riding = _riding.getVehicle();
			}

			if(_ent instanceof LivingEntity && entities.contains(_ent)){
				for(Entity ent : forRemoval){
					ent.remove();
				}
				_ent.remove();
			}


			return;
		}
		if (removeMob) {
			if(mountList != null && !mountList.isEmpty()){
				//Removing the mounts of the entity is removed
				Entity _riding = entity.getVehicle();
				while(_riding != null){
					Entity _prev = _riding;
					_riding = _riding.getVehicle();
					_prev.remove();
				}
			}
			entities.remove(entity);
			if (pulsers.containsKey(entity)) pulsers.remove(entity);
		}
	}

	private class AttackMonitor implements Listener {

		private final LivingEntity caster;
		private final LivingEntity monster;
		private final String[] args;
		private final float power;

		private LivingEntity target;

		private AttackMonitor(LivingEntity caster, LivingEntity monster, LivingEntity target, float power, String[] args) {
			this.caster = caster;
			this.monster = monster;
			this.target = target;
			this.power = power;
			this.args = args;
		}

		@EventHandler(ignoreCancelled = true)
		private void onDamage(EntityDamageByEntityEvent event) {
			Entity damager = event.getDamager();
			if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity entity)
				damager = entity;

			if (damager != monster) return;

			if (attackSpell != null && event.getEntity() instanceof LivingEntity damaged) {
				attackSpell.subcast(caster, monster.getLocation(), damaged, power, args);
				event.setCancelled(cancelAttack);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		private void onTarget(EntityTargetEvent event) {
			if (event.getEntity() == monster) {
				if (!validTargetList.canTarget(caster, event.getTarget()) || (targetModifiers != null && !targetModifiers.check(monster, (LivingEntity) event.getTarget()))) event.setCancelled(true);
				else if (event.getTarget() == null) retarget(null);
				else if (target != null && event.getTarget() != target) event.setTarget(target);
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		private void onDeath(EntityDeathEvent event) {
			LivingEntity entity = event.getEntity();
			if (entity == monster) {
				HandlerList.unregisterAll(this);
				return;
			}

			if (entity != target) return;

			retarget(entity);
			if (target != null) MobUtil.setTarget(monster, target);
		}

		@EventHandler(ignoreCancelled = true)
		public void onRemove(EntityRemoveFromWorldEvent event) {
			Entity entity = event.getEntity();
			if (entity == monster) {
				HandlerList.unregisterAll(this);
				return;
			}

			if (entity != target) return;

			retarget(target);
			if (target != null) MobUtil.setTarget(monster, target);
		}

		private void retarget(LivingEntity ignore) {
			double range = retargetRange.get(caster, null, power, args);
			double rangeSquared = range * range;
			double distanceSquared;

			for (LivingEntity entity : monster.getLocation().getNearbyLivingEntities(range)) {
				if (!entity.isValid()) continue;
				if (entity.equals(ignore)) continue;
				if (!validTargetList.canTarget(caster, entity)) continue;
			}

			for (Entity e : monster.getNearbyEntities(range, range, range)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;
				if (e == ignore) continue;
				if (targetModifiers != null && !targetModifiers.check(monster, (LivingEntity) e)) continue;

				target = (LivingEntity) e;
				break;
			}
		}
	}

	private class Targeter implements Runnable {

		private final Mob mob;

		private final LivingEntity caster;
		private final String[] args;
		private final float power;
		private final int taskId;

		private Targeter(LivingEntity caster, Mob mob, float power, String[] args, int interval) {
			this.caster = caster;
			this.power = power;
			this.args = args;
			this.mob = mob;

			this.taskId = MagicSpells.scheduleRepeatingTask(this, 1, interval);
		}

		@Override
		public void run() {
			if (!mob.isValid()) {
				MagicSpells.cancelTask(taskId);
				return;
			}

			double targetRange = SpawnEntitySpell.this.targetRange.get(caster, null, power, args);
			List<Entity> list = mob.getNearbyEntities(targetRange, targetRange, targetRange);
			List<LivingEntity> targetable = new ArrayList<>();
			LivingEntity target = null;
			double nearestEntityDistance = 0;
			for (Entity e : list) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;
				if (targetModifiers != null && !targetModifiers.check(mob, (LivingEntity) e)) continue;

				targetable.add((LivingEntity) e);

				if (target == null || e.getLocation().distanceSquared(mob.getLocation()) < nearestEntityDistance) {
					target = (LivingEntity) e;
					nearestEntityDistance = e.getLocation().distanceSquared(mob.getLocation());
				}
			}

			if (targetable.isEmpty()) return;

			EntityTargetEvent.TargetReason reason = EntityTargetEvent.TargetReason.CLOSEST_ENTITY;

			if (nearestEntityDistance > Math.pow(SpawnEntitySpell.this.targetPriorityRange.get(caster, null, power, args), 2)) {
				target = targetable.get(random.nextInt(targetable.size()));
				reason = EntityTargetEvent.TargetReason.RANDOM_TARGET;
			}

			MobUtil.setTarget(mob, target, reason);
		}

	}

	private class EntityPulser {

		private final LivingEntity caster;
		private final LivingEntity entity;
		private final float power;
		private final int delay = random.nextInt(SpawnEntitySpell.this.spellInterval);

		private EntityPulser(LivingEntity caster, LivingEntity entity, float power, String[] args) {
			this.caster = caster;
			this.entity = entity;
			this.power = power;
		}

		private void pulse() {
			if (entity != null && entity.isValid() && pulsers.containsKey(entity) && entity.getWorld().isChunkLoaded(entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4)) {
				activate();
			} else {
				pulsers.remove(entity);
			}
		}

		private void activate() {
			LivingEntity target = null;
			if (entity instanceof Mob) target = ((Mob) entity).getTarget();

			if (intervalSpell.isTargetedEntityFromLocationSpell()) {
				if (target != null) intervalSpell.castAtEntityFromLocation(caster, entity.getLocation(), target, power);
			} else if (intervalSpell.isTargetedEntitySpell()) {
				if (target != null) intervalSpell.castAtEntity(caster, (LivingEntity) target, power);
			} else if (intervalSpell.isTargetedLocationSpell()) {
				Location location = entity.getLocation();
				if (target != null && !(intervalSpell.getSpell() instanceof InstantSpell)) {
					location = target.getLocation();
				}

				intervalSpell.castAtLocation(caster, location, power);
			} else {
				intervalSpell.cast(entity, power);
			}
		}
	}


	private class EntityPulserTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicSpells.scheduleRepeatingTask(this, 0, spellInterval);
		}

		private void stop() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}

		@Override
		public void run() {
			for (Map.Entry<LivingEntity, EntityPulser> entry : new HashMap<>(pulsers).entrySet()) {
				if (synchroniseIntervalSpells) {
					entry.getValue().pulse();
				} else {
					MagicSpells.scheduleDelayedTask(() -> entry.getValue().pulse(), entry.getValue().delay);
				}
			}
			if (pulsers.isEmpty()) stop();
		}

	}
}
