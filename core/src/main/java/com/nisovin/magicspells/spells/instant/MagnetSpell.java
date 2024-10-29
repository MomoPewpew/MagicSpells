package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class MagnetSpell extends InstantSpell implements TargetedLocationSpell {

	private ConfigData<Double> radius;
	private ConfigData<Double> velocity;

	private boolean teleport;
	private boolean forcePickup;
	private boolean removeItemGravity;
	private boolean powerAffectsRadius;
	private boolean powerAffectsVelocity;
	private boolean resolveVelocityPerItem;

	public MagnetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataDouble("radius", 5);
		velocity = getConfigDataDouble("velocity", 1);

		teleport = getConfigBoolean("teleport-items", false);
		forcePickup = getConfigBoolean("force-pickup", false);
		removeItemGravity = getConfigBoolean("remove-item-gravity", false);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);
		powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);
		resolveVelocityPerItem = getConfigBoolean("resolve-velocity-per-item", false);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			List<Item> items = getNearbyItems(data);
			magnet(data, items);

			playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(SpellData data) {
		Collection<Item> targetItems = getNearbyItems(data);
		magnet(data, targetItems);

		return true;
	}

	private List<Item> getNearbyItems(SpellData data) {
		double radius = this.radius.get(data);
		if (powerAffectsRadius) radius *= data.power();
		radius = Math.min(radius, MagicSpells.getGlobalRadius());

		Collection<Entity> entities = data.location().getWorld().getNearbyEntities(data.location(), radius, radius, radius);
		List<Item> ret = new ArrayList<>();
		for (Entity e : entities) {
			if (!(e instanceof Item i)) continue;
			ItemStack stack = i.getItemStack();
			if (InventoryUtil.isNothing(stack)) continue;
			if (i.isDead()) continue;

			if (forcePickup) {
				i.setPickupDelay(0);
				ret.add(i);
			} else if (i.getPickupDelay() < i.getTicksLived()) {
				ret.add(i);
			}
		}
		return ret;
	}

	private void magnet(SpellData data, Collection<Item> items) {
		double velocity = 0;
		if (!resolveVelocityPerItem) {
			velocity = this.velocity.get(data);
			if (powerAffectsVelocity) velocity *= data.power();
		}

		for (Item i : items) magnet(i, data, velocity);
	}

	private void magnet(Item item, SpellData data, double velocity) {
		if (removeItemGravity) item.setGravity(false);
		if (teleport) item.teleportAsync(data.location());
		else {
			if (resolveVelocityPerItem) {
				velocity = this.velocity.get(data);
				if (powerAffectsVelocity) velocity *= data.power();
			}

			item.setVelocity(data.location().toVector().subtract(item.getLocation().toVector()).normalize().multiply(velocity));
		}
		playSpellEffects(EffectPosition.PROJECTILE, item, data);
	}

	public void setTeleport(boolean teleport) {
		this.teleport = teleport;
	}

}
