package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class ManaRegenSpell extends BuffSpell {

	private final Map<UUID, SpellData> players;

	private ConfigData<Integer> regenModAmt;

	public ManaRegenSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		regenModAmt = getConfigDataInt("regen-mod-amt", 3);

		players = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.caster() instanceof Player)) return false;
		players.put(data.caster().getUniqueId(), data);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		players.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		players.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		Player player = event.getPlayer();
		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		if (!isActive(player)) return;
		if (!event.getReason().equals(ManaChangeReason.REGEN)) return;

		SpellData data = players.get(player.getUniqueId());

		int newAmt = event.getNewAmount() + regenModAmt.get(data);
		if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
		else if (newAmt < 0) newAmt = 0;

		addUseAndChargeCost(player);
		event.setNewAmount(newAmt);
	}

	public Map<UUID, SpellData> getPlayers() {
		return players;
	}

}
