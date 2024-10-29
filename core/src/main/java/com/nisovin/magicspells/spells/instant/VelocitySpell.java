package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class VelocitySpell extends InstantSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final Set<UUID> jumping;

	private final ConfigData<Double> speed;

	private boolean cancelDamage;
	private boolean powerAffectsSpeed;
	private boolean addVelocityInstead;

	public VelocitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		jumping = new HashSet<>();

		speed = getConfigDataDouble("speed", 40);

		cancelDamage = getConfigBoolean("cancel-damage", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
		powerAffectsSpeed = getConfigBoolean("power-affects-speed", true);
	}

	public boolean isJumping(LivingEntity livingEntity) {
		return jumping.contains(livingEntity.getUniqueId());
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			launch(data);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntityFromLocation(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return launch(data);
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		return launch(data);
	}

	private boolean launch(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location from = data.location();

		if (target == null) return false;

		if (from == null) from = target.getLocation();

		double speed = this.speed.get(data) / 10;
		if (powerAffectsSpeed) speed *= data.power();

		Vector velocity = from.getDirection().normalize().multiply(speed);

		if (addVelocityInstead) target.setVelocity(target.getVelocity().add(velocity));
		else target.setVelocity(velocity);
		jumping.add(target.getUniqueId());

		if (caster != null) playSpellEffects(data);
		else playSpellEffects(EffectPosition.TARGET, target, data);

		return true;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
		LivingEntity livingEntity = (LivingEntity) event.getEntity();
		if (!jumping.remove(livingEntity.getUniqueId())) return;
		playSpellEffects(EffectPosition.TARGET, livingEntity.getLocation(), new SpellData(livingEntity));
		if (cancelDamage) event.setCancelled(true);
	}

}
