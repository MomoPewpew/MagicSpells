package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class ForcetossSpell extends TargetedSpell implements TargetedEntitySpell {

	private int damage;

	private ConfigData<Double> vForce;
	private ConfigData<Double> hForce;

	private ConfigData<Float> rotation;

	private boolean checkPlugins;
	private boolean powerAffectsForce;
	private boolean addVelocityInstead;
	private boolean avoidDamageModification;

	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigInt("damage", 0);

		vForce = getConfigDataDouble("vertical-force", 10);
		hForce = getConfigDataDouble("horizontal-force", 20);

		rotation = getConfigDataFloat("rotation", 0);

		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsForce = getConfigBoolean("power-affects-force", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", true);
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);

			toss(data.builder().target(targetInfo.target()).power(targetInfo.getPower()).build());
			sendMessages(data.caster(), targetInfo.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target())) return false;
		toss(data);
		return true;
	}

	private void toss(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		float power = data.power();

		if (target == null) return;
		if (caster == null) return;
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return;

		if (damage > 0) {
			double dmg = damage * power;
			if (checkPlugins) {
				MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, damage, this);
				EventUtil.call(event);
				if (!avoidDamageModification) dmg = event.getDamage();
			}
			target.damage(dmg);
		}

		Vector v;
		if (caster.equals(target)) v = caster.getLocation().getDirection();
		else v = target.getLocation().toVector().subtract(caster.getLocation().toVector());

		double hForce = this.hForce.get(data) / 10;
		double vForce = this.vForce.get(data) / 10;
		if (powerAffectsForce) {
			hForce *= power;
			vForce *= power;
		}
		v.setY(0).normalize().multiply(hForce).setY(vForce);

		float rotation = this.rotation.get(data);
		if (rotation != 0) Util.rotateVector(v, rotation);

		v = Util.makeFinite(v);

		if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);

		playSpellEffects(data);
	}

}
