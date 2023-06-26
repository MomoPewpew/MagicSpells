package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class BowSpell extends Spell {

	private static final String METADATA_KEY = "MSBowSpell";
	private static HitListener hitListener;

	private List<Component> bowNames;
	private List<Component> disallowedBowNames;

	private List<MagicItemData> bowItems;
	private List<MagicItemData> ammoItems;
	private List<MagicItemData> disallowedBowItems;
	private List<MagicItemData> disallowedAmmoItems;

	private ValidTargetList triggerList;

	private Component bowName;

	private String spellOnShootName;
	private String spellOnHitEntityName;
	private String spellOnHitGroundName;
	private String spellOnEntityLocationName;

	private Subspell spellOnShoot;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;
	private Subspell spellOnEntityLocation;

	private boolean cancelShot;
	private boolean denyOffhand;
	private boolean removeArrow;
	private boolean requireBind;
	private boolean useBowForce;
	private boolean cancelShotOnFail;

	private float minimumForce;
	private float maximumForce;

	public BowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		List<String> names = getConfigStringList("bow-names", null);
		if (names != null) {
			bowNames = new ArrayList<>();
			names.forEach(str -> bowNames.add(Util.getMiniMessage(str)));
		} else {
			String bowNameString = getConfigString("bow-name", null);
			if (bowNameString != null) bowName = Util.getMiniMessage(bowNameString);
		}

		List<String> disallowedNames = getConfigStringList("disallowed-bow-names", null);
		if (disallowedNames != null) {
			disallowedBowNames = new ArrayList<>();
			disallowedNames.forEach(str -> disallowedBowNames.add(Util.getMiniMessage(str)));
		}

		if (config.isList("spells." + internalName + ".can-trigger")) {
			List<String> targets = getConfigStringList("can-trigger", new ArrayList<>());
			if (targets.isEmpty()) targets.add("players");
			triggerList = new ValidTargetList(this, targets);
		} else triggerList = new ValidTargetList(this, getConfigString("can-trigger", "players"));

		bowItems = getFilter("bow-items");
		ammoItems = getFilter("ammo-items");
		disallowedBowItems = getFilter("disallowed-bow-items");
		disallowedAmmoItems = getFilter("disallowed-ammo-items");

		spellOnShootName = getConfigString("spell", "");
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");
		spellOnEntityLocationName = getConfigString("spell-on-entity-location", "");

		bindable = getConfigBoolean("bindable", false);
		cancelShot = getConfigBoolean("cancel-shot", true);
		denyOffhand = getConfigBoolean("deny-offhand", false);
		removeArrow = getConfigBoolean("remove-arrow", false);
		requireBind = getConfigBoolean("require-bind", false);
		useBowForce = getConfigBoolean("use-bow-force", true);
		cancelShotOnFail = getConfigBoolean("cancel-shot-on-fail", true);

		minimumForce = getConfigFloat("minimum-force", 0F);
		maximumForce = getConfigFloat("maximum-force", 1F);

		if (minimumForce < 0F) minimumForce = 0F;
		else if (minimumForce > 1F) minimumForce = 1F;
		if (maximumForce < 0F) maximumForce = 0F;
		else if (maximumForce > 1F) maximumForce = 1F;
	}

	private List<MagicItemData> getFilter(String key) {
		List<String> itemStrings = getConfigStringList(key, null);
		if (itemStrings == null || itemStrings.isEmpty()) return null;

		List<MagicItemData> itemData = new ArrayList<>();
		for (String itemString : itemStrings) {
			MagicItemData data = MagicItems.getMagicItemDataFromString(itemString);
			if (data == null) {
				MagicSpells.error("BowSpell '" + internalName + "' has an magic item '" + itemString + "' defined!");
				continue;
			}

			itemData.add(data);
		}

		return itemData.isEmpty() ? null : itemData;
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnShoot = initSubspell(spellOnShootName, "BowSpell '" + internalName + "' has an invalid spell defined!");
		spellOnHitEntity = initSubspell(spellOnHitEntityName, "BowSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
		spellOnHitGround = initSubspell(spellOnHitGroundName, "BowSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");

		spellOnEntityLocation = new Subspell(spellOnEntityLocationName);
		if (!spellOnEntityLocation.process()) {
			if (!spellOnEntityLocationName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-entity-location defined!");
			spellOnEntityLocation = null;
		}

		if (hitListener == null) {
			hitListener = new HitListener();
			registerEvents(hitListener);
		}

		if (!requireBind) registerEvents(new ShootListener());
	}

	@Override
	public void turnOff() {
		super.turnOff();

		hitListener = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean canCastWithItem() {
		return bindable;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}

	public boolean isBindRequired() {
		return requireBind;
	}

	public void handleBowCast(EntityShootBowEvent event) {
		if (!cancelShot && event.isCancelled()) return;
		if (!(event.getProjectile() instanceof Arrow)) return;
		if (denyOffhand && event.getHand() == EquipmentSlot.OFF_HAND) return;

		LivingEntity caster = event.getEntity();
		if (!triggerList.canTarget(caster, true)) return;

		if (caster instanceof Player) {
			Spellbook spellbook = MagicSpells.getSpellbook((Player) caster);
			if (!spellbook.hasSpell(this) || !spellbook.canCast(this)) return;
		}

		ItemStack bow = event.getBow();
		if (bow == null || (bow.getType() != Material.BOW && bow.getType() != Material.CROSSBOW)) return;

		float force = event.getForce();
		if (force < minimumForce || force > maximumForce) return;

		Component name = bow.getItemMeta().displayName();
		if (bowNames != null && !bowNames.contains(name)) return;
		if (disallowedBowNames != null && disallowedBowNames.contains(name)) return;
		if (bowName != null && !bowName.equals(name)) return;

		if (bowItems != null && !check(bow, bowItems)) return;
		if (disallowedBowItems != null && check(bow, disallowedBowItems)) return;

		ItemStack ammo = event.getConsumable();
		if (ammoItems != null && !check(ammo, ammoItems)) return;
		if (disallowedAmmoItems != null && check(ammo, disallowedAmmoItems)) return;

		SpellCastEvent castEvent = preCast(caster, useBowForce ? force : 1f, null);
		if (castEvent == null) {
			if (cancelShotOnFail) event.setCancelled(true);
			return;
		}

		if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
			if (cancelShot) event.setCancelled(true);
			if (!event.isCancelled()) {
				Entity projectile = event.getProjectile();

				ArrowData arrowData = new ArrowData(this, new SpellData(caster, null, castEvent.getPower(), null));
				List<ArrowData> arrowDataList = null;
				if (projectile.hasMetadata(METADATA_KEY)) {
					List<MetadataValue> metas = projectile.getMetadata(METADATA_KEY);
					for (MetadataValue meta : metas) {
						if (!MagicSpells.plugin.equals(meta.getOwningPlugin())) continue;

						arrowDataList = (List<ArrowData>) meta.value();
						if (arrowDataList != null) arrowDataList.add(arrowData);
						break;
					}
				}

				if (arrowDataList == null) {
					arrowDataList = new ArrayList<>();
					arrowDataList.add(arrowData);

					projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicSpells.plugin, arrowDataList));
				}

				playSpellEffects(EffectPosition.PROJECTILE, projectile, arrowData.spellData);
				playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), projectile.getLocation(), caster, projectile, arrowData.spellData);
			}

			if (spellOnShoot != null) spellOnShoot.subcast(caster, castEvent.getPower(), null);
		} else if (cancelShotOnFail) event.setCancelled(true);

		postCast(castEvent, PostCastAction.HANDLE_NORMALLY);
	}

	private boolean check(ItemStack item, List<MagicItemData> filters) {
		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		for (MagicItemData data : filters)
			if (data.matches(itemData))
				return true;

		return false;
	}

	private class ShootListener implements Listener {

		@EventHandler
		public void onArrowLaunch(EntityShootBowEvent event) {
			handleBowCast(event);
		}

	}

	private static class HitListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onArrowHitGround(ProjectileHitEvent event) {
			if (event.getHitBlock() == null) return;

			Projectile proj = event.getEntity();
			if (!proj.hasMetadata(METADATA_KEY)) return;

			List<MetadataValue> metas = proj.getMetadata(METADATA_KEY);
			boolean remove = false;
			for (MetadataValue meta : metas) {
				if (!MagicSpells.plugin.equals(meta.getOwningPlugin())) continue;

				ProjectileSource shooter = proj.getShooter();
				if (!(shooter instanceof LivingEntity caster)) break;

				List<ArrowData> arrowDataList = (List<ArrowData>) meta.value();
				if (arrowDataList == null || arrowDataList.isEmpty()) break;

				for (ArrowData data : arrowDataList) {
					Subspell groundSpell = data.bowSpell.spellOnHitGround;
					if (groundSpell == null) continue;

					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(data.bowSpell, caster, proj.getLocation(), data.spellData.power());
					if (!targetEvent.callEvent()) continue;

					groundSpell.subcast(caster, targetEvent.getTargetLocation(), targetEvent.getPower(), null);

					if (data.bowSpell.removeArrow) remove = true;
				}

				break;
			}

			proj.removeMetadata(METADATA_KEY, MagicSpells.plugin);
			if (remove) proj.remove();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onArrowHitEntity(EntityDamageByEntityEvent event) {
			Entity damager = event.getDamager();
			if (!(damager instanceof Arrow arrow)) return;
			if (!damager.hasMetadata(METADATA_KEY)) return;

			List<MetadataValue> metas = damager.getMetadata(METADATA_KEY);
			boolean remove = false;
			for (MetadataValue meta : metas) {
				if (!MagicSpells.plugin.equals(meta.getOwningPlugin())) continue;

				Entity damaged = event.getEntity();
				if (!(damaged instanceof LivingEntity target)) break;

				ProjectileSource shooter = arrow.getShooter();
				if (!(shooter instanceof LivingEntity caster)) break;

				List<ArrowData> arrowDataList = (List<ArrowData>) meta.value();
				if (arrowDataList == null || arrowDataList.isEmpty()) break;

				for (ArrowData data : arrowDataList) {
					Subspell entitySpell = data.bowSpell.spellOnHitEntity;
					Subspell entityLocationSpell = data.bowSpell.spellOnEntityLocation;

					SpellTargetEvent targetEvent = new SpellTargetEvent(data.bowSpell, caster, target, data.spellData.power());
					if (!targetEvent.callEvent()) continue;

					LivingEntity subTarget = targetEvent.getTarget();
					float subPower = targetEvent.getPower();

					if (entitySpell != null) entitySpell.subcast(caster, caster.getLocation(), subTarget, subPower, null);
					if (entityLocationSpell != null) entityLocationSpell.subcast(caster, arrow.getLocation(), subPower, null);

					if (data.bowSpell.removeArrow) remove = true;
				}

				break;
			}

			damager.removeMetadata(METADATA_KEY, MagicSpells.plugin);
			if (remove) damager.remove();
		}

	}

	private record ArrowData(BowSpell bowSpell, SpellData spellData) {

	}

}
