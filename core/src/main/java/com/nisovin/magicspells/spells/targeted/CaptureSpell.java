package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CaptureSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker CAPTURABLE = entity -> MobUtil.hasEggMaterialForEntityType(entity.getType());

	private Component itemName;
	private List<Component> itemLore = new ArrayList<>();

	private boolean gravity;
	private boolean addToInventory;
	private boolean powerAffectsQuantity;

	public CaptureSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		itemName = Util.getMiniMessage(getConfigString("item-name", null));
		List<String> lore = getConfigStringList("item-lore", null);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", false);
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", true);

		if (lore != null) {
			for (int i = 0; i < lore.size(); i++) {
				itemLore.set(i, Util.getMiniMessage(lore.get(i)));
			}
		}
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(data, CAPTURABLE);
			if (target.noTarget()) return noTarget(data);

			boolean ok = capture(data.builder().target(target.target()).power(target.getPower()).build());
			if (!ok) return noTarget(data);

			sendMessages(data.caster(), target.target(), data.args());
			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target()) || !MobUtil.hasEggMaterialForEntityType(data.target().getType())) return false;
		return capture(data);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return CAPTURABLE;
	}

	private boolean capture(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		ItemStack item = MobUtil.getEggItemForEntityType(target.getType());
		if (item == null) return false;

		if (powerAffectsQuantity) {
			int q = Math.round(data.power());
			if (q > 1) item.setAmount(q);
		}

		String entityName = MagicSpells.getEntityNames().get(target.getType());
		if (itemName != null || itemLore != null) {
			if (entityName == null) entityName = "unknown";
			ItemMeta meta = item.getItemMeta();
			if (itemName != null) meta.displayName(Util.getMiniMessage(Util.getStringFromComponent(itemName).replace("%name%", entityName)));
			if (itemLore != null) {
				List<Component> lore = new ArrayList<>();
				for (Component l : itemLore) lore.add(Util.getMiniMessage(Util.getStringFromComponent(l).replace("%name%", entityName)));
				meta.lore(lore);
			}

			item.setItemMeta(meta);
		}

		target.remove();
		boolean added = false;

		if (addToInventory && caster instanceof Player player) added = Util.addToInventory(player.getInventory(), item, true, false);
		if (!added) {
			Item dropped = target.getWorld().dropItem(target.getLocation().add(0, 1, 0), item);
			dropped.setItemStack(item);
			dropped.setGravity(gravity);
		}

		if (caster != null) playSpellEffects(caster, target.getLocation(), data);
		else playSpellEffects(EffectPosition.TARGET, target.getLocation(), data);

		return true;
	}

}
