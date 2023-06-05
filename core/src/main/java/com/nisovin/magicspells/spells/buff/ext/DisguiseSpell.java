package com.nisovin.magicspells.spells.buff.ext;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.*;
import me.libraryaddict.disguise.utilities.parser.DisguiseParser;

// NOTE: LIBSDISGUISES IS REQUIRED FOR THIS
public class DisguiseSpell extends BuffSpell {

	private final Set<UUID> entities;

	private ConfigData<Disguise> disguiseData;

	private EntityData entityData;

	private String playerName;
	private String skinName;

	private boolean burning;
	private boolean glowing;

	public DisguiseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();

		playerName = getConfigString("player-name", "");
		skinName = getConfigString("skin-name", playerName);

		burning = getConfigBoolean("burning", false);
		glowing = getConfigBoolean("glowing", false);

		if (isConfigSection("disguise")) {
			ConfigurationSection disguiseSection = getConfigSection("disguise");
			if (disguiseSection != null) entityData = new EntityData(disguiseSection);

			MagicSpells.error("DisguiseSpell '" + internalName + "' is using the legacy 'disguise' section, which is planned for removal. Please switch to a 'disguise' string.");
			return;
		}

		String disguiseString = getConfigString("disguise", null);
		if (disguiseString == null) {
			MagicSpells.error("DisguiseSpell '" + internalName + "' has an invalid or no 'disguise' defined.");
			disguiseData = null;
			return;
		}

		ConfigData<String> supplier = ConfigDataUtil.getString(disguiseString);
		if (supplier.isConstant()) {
			try {
				Disguise disguise = DisguiseParser.parseDisguise(disguiseString);
				disguiseData = (caster, target, power, args) -> disguise;
			} catch (Throwable t) {
				MagicSpells.error("DisguiseSpell '" + internalName + "' has an invalid 'disguise' defined.");
				DebugHandler.debug(t);
				disguiseData = null;
			}

			return;
		}

		disguiseData = (caster, target, power, args) -> {
			try {
				return DisguiseParser.parseDisguise(supplier.get(caster, target, power, args));
			} catch (Throwable ignored) {
				return null;
			}
		};
	}

	@Override
	public void initialize() {
		super.initialize();

		if (disguiseData != null) return;

		if (entityData == null || entityData.getEntityType() == null)
			MagicSpells.error("DisguiseSpell '" + internalName + "' has an invalid disguise defined!");
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		// STRING
		if (disguiseData != null) {
			Disguise disguise = disguiseData.get(entity, null, power, args);
			if (disguise == null) return false;

			DisguiseAPI.disguiseEntity(entity, disguise);
			entities.add(entity.getUniqueId());

			return true;
		}

		if (entityData == null) return false;

		DisguiseType disguiseType = DisguiseType.getType(entityData.getEntityType().get(entity, null, power, args));

		Disguise disguise;
		if (disguiseType.isPlayer()) disguise = new PlayerDisguise(playerName, skinName);
		else if (disguiseType.isMob()) disguise = new MobDisguise(disguiseType);
		else if (disguiseType.isMisc()) disguise = new MiscDisguise(disguiseType);
		else return false;

		FlagWatcher watcher = disguise.getWatcher();
		watcher.setBurning(burning);
		watcher.setGlowing(glowing);

		if (watcher instanceof AgeableWatcher ageableWatcher)
			ageableWatcher.setBaby(entityData.getBaby().get(entity, null, power, args));

		if (watcher instanceof AbstractHorseWatcher abstractHorseWatcher) {
			abstractHorseWatcher.setSaddled(entityData.getSaddled().get(entity, null, power, args));

			if (abstractHorseWatcher instanceof ChestedHorseWatcher chestedHorseWatcher)
				chestedHorseWatcher.setCarryingChest(entityData.getChested().get(entity, null, power, args));

			if (abstractHorseWatcher instanceof HorseWatcher horseWatcher) {
				horseWatcher.setColor(entityData.getHorseColor().get(entity, null, power, args));
				horseWatcher.setStyle(entityData.getHorseStyle().get(entity, null, power, args));
			}

			if (abstractHorseWatcher instanceof LlamaWatcher llamaWatcher)
				llamaWatcher.setColor(entityData.getLlamaColor().get(entity, null, power, args));
		}

		if (watcher instanceof TameableWatcher tameableWatcher) {
			tameableWatcher.setTamed(entityData.getTamed().get(entity, null, power, args));

			if (tameableWatcher instanceof ParrotWatcher parrotWatcher)
				parrotWatcher.setVariant(entityData.getParrotVariant().get(entity, null, power, args));

			if (tameableWatcher instanceof WolfWatcher wolfWatcher) {
				DyeColor color = entityData.getColor().get(entity, null, power, args);
				if (color != null) wolfWatcher.setCollarColor(color);
			}
		}

		if (watcher instanceof CreeperWatcher creeperWatcher)
			creeperWatcher.setPowered(entityData.getPowered().get(entity, null, power, args));

		if (watcher instanceof DroppedItemWatcher droppedItemWatcher)
			droppedItemWatcher.setItemStack(new ItemStack(entityData.getDroppedItemStack().get(entity, null, power, args)));

		if (watcher instanceof EndermanWatcher endermanWatcher)
			endermanWatcher.setItemInMainHand(entityData.getCarriedBlockData().get(entity, null, power, args).getMaterial());

		if (watcher instanceof FallingBlockWatcher fallingBlockWatcher)
			fallingBlockWatcher.setBlockData(entityData.getFallingBlockData().get(entity, null, power, args));

		if (watcher instanceof PigWatcher pigWatcher)
			pigWatcher.setSaddled(entityData.getSaddled().get(entity, null, power, args));

		if (watcher instanceof PufferFishWatcher pufferFishWatcher)
			pufferFishWatcher.setPuffState(entityData.getSize().get(entity, null, power, args));

		if (watcher instanceof SheepWatcher sheepWatcher) {
			DyeColor color = entityData.getColor().get(entity, null, power, args);
			if (color != null) sheepWatcher.setColor(color);

			sheepWatcher.setSheared(entityData.getSheared().get(entity, null, power, args));
		}

		if (watcher instanceof SlimeWatcher slimeWatcher)
			slimeWatcher.setSize(entityData.getSize().get(entity, null, power, args));

		if (watcher instanceof TropicalFishWatcher tropicalFishWatcher) {
			tropicalFishWatcher.setBodyColor(entityData.getColor().get(entity, null, power, args));
			tropicalFishWatcher.setPatternColor(entityData.getTropicalFishPatternColor().get(entity, null, power, args));
			tropicalFishWatcher.setPattern(entityData.getTropicalFishPattern().get(entity, null, power, args));
		}

		if (watcher instanceof VillagerWatcher villagerWatcher) {
			Villager.Profession profession = entityData.getProfession().get(entity, null, power, args);
			if (profession != null) villagerWatcher.setProfession(profession);
		}

		DisguiseAPI.disguiseEntity(entity, disguise);
		entities.add(entity.getUniqueId());

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		DisguiseAPI.undisguiseToAll(entity);
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			DisguiseAPI.undisguiseToAll(entity);
		}
		entities.clear();
	}

	public Set<UUID> getEntities() {
		return entities;
	}

	public EntityData getEntityData() {
		return entityData;
	}

	public void setEntityData(EntityData entityData) {
		this.entityData = entityData;
	}

}
