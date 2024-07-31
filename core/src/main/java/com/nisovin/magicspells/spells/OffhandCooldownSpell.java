package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class OffhandCooldownSpell extends InstantSpell {

	private List<Player> players = new ArrayList<>();


	private ItemStack item;

	private Spell spellToCheck;
	private String spellToCheckName;

	public OffhandCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		if (isConfigString("item")) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString("item", "stone"));
			if (magicItem != null) item = magicItem.getItemStack();
		} else if (isConfigSection("item")) {
			MagicItem magicItem = MagicItems.getMagicItemFromSection(getConfigSection("item"));
			if (magicItem != null) item = magicItem.getItemStack();
		}

		spellToCheckName = getConfigString("spell", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellToCheck = MagicSpells.getSpellByInternalName(spellToCheckName);

		if (spellToCheck == null || item == null) return;
		
		MagicSpells.scheduleRepeatingTask(() -> {
			Iterator<Player> iter = players.iterator();
			while (iter.hasNext()) {
				Player pl = iter.next();
				if (!pl.isValid()) {
					iter.remove();
					continue;
				}
				float cd = spellToCheck.getCooldown(pl);
				int amt = 1;
				if (cd > 0) amt = (int) Math.ceil(cd);

				PlayerInventory inventory = pl.getInventory();
				ItemStack off = inventory.getItemInOffHand();

				if (!Util.isSimilarNoFlags(off, item)) {
					off = item.clone();
					inventory.setItemInOffHand(off);
				}

				off.setAmount(amt);
			}
		}, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			players.add(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
