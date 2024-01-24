package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

public class ItemSprayEffect extends SpellEffect {

	private ConfigData<Material> material;

	private ConfigData<Double> force;

	private ConfigData<Integer> amount;
	private ConfigData<Integer> duration;
	private ConfigData<Integer> customModelData;

	private boolean resolveForcePerItem;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		material = ConfigDataUtil.getMaterial(config, "type", null);

		force = ConfigDataUtil.getDouble(config, "force", 1);

		amount = ConfigDataUtil.getInteger(config, "amount", 15);
		duration = ConfigDataUtil.getInteger(config, "duration", 10);
		customModelData = ConfigDataUtil.getInteger(config, "custom-model-data", null);

		resolveForcePerItem = config.getBoolean("resolve-force-per-item", false);

	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		Material material = this.material.get(data);
		if (material == null) return null;

		ItemStack item = new ItemStack(material);

		if (customModelData != null) {

			Integer customModelData = this.customModelData.get(data);

			ItemMeta meta = item.getItemMeta();
			meta.setCustomModelData(customModelData);
			
			item.setItemMeta(meta);
		};

		Random rand = ThreadLocalRandom.current();
		Location loc = location.clone().add(0, 1, 0);

		double force = resolveForcePerItem ? 0 : this.force.get(data);
		int duration = this.duration.get(data);

		int amount = this.amount.get(data);
		Item[] items = new Item[amount];
		for (int i = 0; i < amount; i++) {
			items[i] = loc.getWorld().dropItem(loc, item);

			if (resolveForcePerItem) force = this.force.get(data);
			items[i].setVelocity(new Vector((rand.nextDouble() - 0.5d) * force, (rand.nextDouble() - 0.5d) * force, (rand.nextDouble() - 0.5d) * force));
			items[i].setPickupDelay(duration << 1);
		}

		MagicSpells.scheduleDelayedTask(() -> Arrays.stream(items).forEach(Item::remove), duration);
		return null;
	}

}
