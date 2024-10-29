package com.nisovin.magicspells;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class Subspell {

	private static final Random random = ThreadLocalRandom.current();

	private Spell spell;
	private final String spellName;
	private CastMode mode = CastMode.PARTIAL;
	private CastTargeting targeting = CastTargeting.NORMAL;

	private boolean invert = false;
	private boolean passPower = true;
	private boolean passTargeting = false;
	private ConfigData<Integer> delay = (caster, target, power, args) -> -1;
	private ConfigData<Double> chance = (caster, target, power, args) -> -1D;
	private ConfigData<Float> subPower = (caster, target, power, args) -> 1F;
	private ConfigData<String[]> args = (caster, target, power, args) -> null;

	private boolean isTargetedEntity = false;
	private boolean isTargetedLocation = false;
	private boolean isTargetedEntityFromLocation = false;

	// spellName(mode=hard|h|full|f|partial|p|direct|d;power=[subpower];delay=[delay];chance=[chance])
	public Subspell(String data) {
		String[] split = data.split("\\(", 2);

		spellName = split[0].trim();

		if (split.length > 1) {
			split[1] = split[1].trim();
			if (split[1].endsWith(")")) split[1] = split[1].substring(0, split[1].length() - 1);

			String[] castArguments = split[1].split(";");
			for (String castArgument : castArguments) {
				String[] keyValue = castArgument.split("=", 2);
				if (keyValue.length != 2) {
					MagicSpells.error("Invalid cast argument '" + castArgument + "' on subspell '" + data + "'.");
					continue;
				}

				String key = keyValue[0].toLowerCase().trim(), value = keyValue[1].trim();
				switch (key) {
					case "mode" -> mode = CastMode.getFromString(value);
					case "targeting" -> {
						try {
							targeting = CastTargeting.valueOf(value.toUpperCase());
						} catch (IllegalArgumentException e) {
							MagicSpells.error("Invalid target type '" + value + "' on subspell '" + data + "'.");
							DebugHandler.debugIllegalArgumentException(e);
						}
					}
					case "invert" -> invert = Boolean.parseBoolean(value);
					case "pass-power" -> passPower = Boolean.parseBoolean(value);
					case "pass-targeting" -> passTargeting = Boolean.parseBoolean(value);
					case "args" -> {
						try {
							JsonElement element = JsonParser.parseString(value);
							JsonArray array = element.getAsJsonArray();

							List<ConfigData<String>> argumentData = new ArrayList<>();
							List<String> arguments = new ArrayList<>();

							boolean constant = true;
							for (JsonElement je : array) {
								String val = je.getAsString();
								ConfigData<String> supplier = ConfigDataUtil.getString(val);

								argumentData.add(supplier);
								arguments.add(val);

								constant &= supplier.isConstant();
							}

							if (constant) {
								String[] arg = arguments.toArray(new String[0]);
								args = (caster, target, power, args) -> arg;

								continue;
							}

							args = (caster, target, power, args) -> {
								String[] ret = new String[argumentData.size()];
								for (int i = 0; i < argumentData.size(); i++)
									ret[i] = argumentData.get(i).get(caster, target, power, args);

								return ret;
							};
						} catch (IllegalArgumentException e) {
							MagicSpells.error("Invalid spell arguments '" + value + "' on subspell '" + data + "'.");
							DebugHandler.debugIllegalArgumentException(e);
						} catch (ClassCastException | JsonSyntaxException e) {
							MagicSpells.error("Invalid spell arguments '" + value + "' on subspell '" + data + "'.");
							DebugHandler.debug(e);
						}
					}
					case "power" -> {
						try {
							float subPower = Float.parseFloat(value);
							this.subPower = (caster, target, power, args) -> subPower;
						} catch (NumberFormatException e) {
							FunctionData<Float> subPowerData = FunctionData.build(value, Double::floatValue, true);
							if (subPowerData == null) {
								MagicSpells.error("Invalid power '" + value + "' on subspell '" + data + "'.");
								continue;
							}

							subPower = subPowerData;
						}
					}
					case "delay" -> {
						try {
							int delay = Integer.parseInt(value);
							this.delay = (caster, target, power, args) -> delay;
						} catch (NumberFormatException e) {
							FunctionData<Integer> delayData = FunctionData.build(value, Double::intValue, true);
							if (delayData == null) {
								MagicSpells.error("Invalid delay '" + value + "' on subspell '" + data + "'.");
								continue;
							}

							delay = delayData;
						}
					}
					case "chance" -> {
						try {
							double chance = Double.parseDouble(value);
							this.chance = (caster, target, power, args) -> chance;
						} catch (NumberFormatException e) {
							FunctionData<Double> chanceData = FunctionData.build(value, Function.identity(), true);
							if (chanceData == null) {
								MagicSpells.error("Invalid chance '" + value + "' on subspell '" + data + "'.");
								continue;
							}

							chance = chanceData;
						}
					}
					default -> MagicSpells.error("Invalid cast argument '" + castArgument + "' on subspell '" + data + "'.");
				}
			}
		}
	}

	public boolean process() {
		spell = MagicSpells.getSpellByInternalName(spellName);
		if (spell != null) {
			isTargetedEntity = spell instanceof TargetedEntitySpell;
			isTargetedLocation = spell instanceof TargetedLocationSpell;
			isTargetedEntityFromLocation = spell instanceof TargetedEntityFromLocationSpell;

			switch (targeting) {
				case NONE -> {
					isTargetedEntity = false;
					isTargetedLocation = false;
					isTargetedEntityFromLocation = false;
				}
				case ENTITY -> {
					if (!isTargetedEntity) return false;

					isTargetedLocation = false;
					isTargetedEntityFromLocation = false;
				}
				case LOCATION -> {
					if (!isTargetedLocation) return false;

					isTargetedEntity = false;
					isTargetedEntityFromLocation = false;
				}
				case ENTITY_FROM_LOCATION -> {
					if (!isTargetedEntityFromLocation) return false;

					isTargetedEntity = false;
					isTargetedLocation = false;
				}
				case NORMAL -> throw new UnsupportedOperationException("Unimplemented case: " + targeting);
				default -> throw new IllegalArgumentException("Unexpected value: " + targeting);
			}
		}

		return spell != null;
	}

	public Spell getSpell() {
		return spell;
	}

	public boolean isTargetedEntitySpell() {
		return isTargetedEntity;
	}

	public boolean isTargetedLocationSpell() {
		return isTargetedLocation;
	}

	public boolean isTargetedEntityFromLocationSpell() {
		return isTargetedEntityFromLocation;
	}


	public boolean subcast(SpellData data) {
		return subcast(data, passTargeting, true);
	}

	public boolean subcast(SpellData data, boolean passTargeting) {
		return subcast(data, passTargeting, true);
	}

	public boolean subcast(SpellData data, boolean passTargeting, boolean useTargetForLocation) {
		data = data.builder().build();

		if (invert) data.invert();

		CastTargeting targeting = this.targeting;
		if (targeting == CastTargeting.NORMAL) {
			if (spell instanceof TargetedEntityFromLocationSpell) targeting = CastTargeting.ENTITY_FROM_LOCATION;
			else if (spell instanceof TargetedEntitySpell) targeting = CastTargeting.ENTITY;
			else if (spell instanceof TargetedLocationSpell) targeting = CastTargeting.LOCATION;
			else targeting = CastTargeting.NONE;
		}

		return switch (targeting) {
			case ENTITY_FROM_LOCATION ->
				spell instanceof TargetedEntityFromLocationSpell && castAtEntityFromLocation(data, passTargeting);
			case ENTITY -> spell instanceof TargetedEntitySpell && castAtEntity(data, passTargeting);
			case LOCATION ->
				spell instanceof TargetedLocationSpell && castAtLocation(data.builder().location(useTargetForLocation ? data.target().getLocation() : data.location()).build());
			case NONE -> {
				if (data.caster() == null) yield false;

				PostCastAction action = cast(data);
				yield action == PostCastAction.HANDLE_NORMALLY || action == PostCastAction.NO_MESSAGES;
			}
			default -> false;
		};
	}

	public PostCastAction cast(SpellData data) {
		SpellData data_ = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).build();

		double chance = this.chance.get(data_);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return PostCastAction.ALREADY_HANDLED;

		int delay = this.delay.get(data_);
		if (delay < 0) return castReal(data_);

		MagicSpells.scheduleDelayedTask(() -> castReal(data_), delay);

		return PostCastAction.HANDLE_NORMALLY;
	}

	private PostCastAction castReal(SpellData data) {
		data = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).args(args.get(data)).build();

		return switch (mode) {
			case HARD, FULL -> spell.cast(data).action;
			case DIRECT -> spell.castSpell(SpellCastState.NORMAL, data);
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield PostCastAction.ALREADY_HANDLED;

				SpellData data_ = data.builder().power(castEvent.getPower()).build();

				PostCastAction action = spell.castSpell(SpellCastState.NORMAL, data_);
				new SpellCastedEvent(spell, SpellCastState.NORMAL, data_, 0, null, action);

				yield action;
			}
		};
	}

	public boolean castAtEntity(SpellData data) {
		return castAtEntity(data, passTargeting);
	}

	public boolean castAtEntity(SpellData data, boolean passTargeting) {
		SpellData data_ = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).build();

		double chance = this.chance.get(data_);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return false;

		int delay = this.delay.get(data_);
		if (delay < 0) return castAtEntityReal(data_, passTargeting);

		MagicSpells.scheduleDelayedTask(() -> castAtEntityReal(data_, passTargeting), delay);

		return true;
	}

	private boolean castAtEntityReal(SpellData data, boolean passTargeting) {
		if (!isTargetedEntity) return isTargetedLocation && castAtLocationReal(data);

		data = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).args(args.get(data)).build();

		return switch (mode) {
			case HARD -> {
				if (data.caster() == null) yield false;

				SpellCastResult result = spell.cast(data);
				yield result.state == SpellCastState.NORMAL && (result.action == PostCastAction.HANDLE_NORMALLY || result.action == PostCastAction.NO_MESSAGES);
			}
			case DIRECT -> {
				if (passTargeting) yield passTargetingEntity(data);
				else {
					TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
					yield data.caster() != null ? targetedSpell.castAtEntity(data) : targetedSpell.castAtEntity(data);
				}
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL) yield false;

				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data);
				if (!targetEvent.callEvent()) yield false;

				data.target(targetEvent.getTarget());
				data.power(targetEvent.getPower());

				boolean success;
				if (passTargeting) success = passTargetingEntity(data);
				else {
					TargetedEntitySpell targetedEntitySpell = (TargetedEntitySpell) spell;
					success = targetedEntitySpell.castAtEntity(data);
				}

				if (success)
					new SpellCastedEvent(spell, SpellCastState.NORMAL, data, 0, null,
						PostCastAction.HANDLE_NORMALLY).callEvent();

				yield success;
			}
			case FULL -> {
				if (data.caster() == null) yield false;

				SpellCastEvent castEvent = spell.preCast(data);
				if (castEvent == null) yield false;

				PostCastAction action = PostCastAction.HANDLE_NORMALLY;
				boolean success = false;
				if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
					data = data.builder().power(castEvent.getPower()).build();
					SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data);
					if (targetEvent.callEvent()) {
						data = data.builder().target(targetEvent.getTarget()).power(targetEvent.getPower()).build();

						if (passTargeting) success = passTargetingEntity(data);
						else ((TargetedEntitySpell) spell).castAtEntity(data);
					}
				}

				if (success) {
					if (spell instanceof TargetedSpell targetedSpell) {
						action = PostCastAction.NO_MESSAGES;
						targetedSpell.sendMessages(data.caster(), data.target(), data.args());
					}
				} else action = PostCastAction.ALREADY_HANDLED;

				spell.postCast(castEvent, action);

				yield success;
			}
		};
	}

	public boolean passTargetingEntity(SpellData data) {
		ValidTargetList list = spell.getValidTargetList();
		ValidTargetList originalList = list.clone();
		if (Objects.equals(data.caster(), data.target()) && !list.canTargetSelf()) list.setTargetCaster(true);
		if (!list.canTargetEntity(data.target())) {
			list.addEntityTarget(data.target());
			spell.setValidTargetList(list);
		}

		boolean success = data.caster() != null ? ((TargetedEntitySpell) spell).castAtEntity(data) : ((TargetedEntitySpell) spell).castAtEntity(data);
		spell.setValidTargetList(originalList);
		return success;
	}

	public boolean castAtLocation(SpellData data) {
		SpellData data_ = data.builder().power(passPower ? data.power() : 1).build();

		double chance = this.chance.get(data_);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return false;

		int delay = this.delay.get(data_);
		if (delay < 0) return castAtLocationReal(data_);

		MagicSpells.scheduleDelayedTask(() -> castAtLocationReal(data_), delay);

		return true;
	}

	private boolean castAtLocationReal(SpellData data) {
		if (!isTargetedLocation) return false;

		data = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).build();

		return switch (mode) {
			case HARD -> {
				if (data.caster() == null) yield false;

				SpellCastResult result = spell.cast(data);
				yield result.state == SpellCastState.NORMAL && (result.action == PostCastAction.HANDLE_NORMALLY ||
					result.action == PostCastAction.NO_MESSAGES);
			}
			case DIRECT -> {
				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				yield targetedSpell.castAtLocation(data);
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL) yield false;

				data = data.builder().power(castEvent.getPower()).build();
				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetEvent.callEvent()) yield false;

				data = data.builder().location(targetEvent.getTargetLocation()).power(targetEvent.getPower()).build();

				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				boolean success = targetedSpell.castAtLocation(data);

				if (success)
					new SpellCastedEvent(spell, SpellCastState.NORMAL, data, 0, null,
						PostCastAction.HANDLE_NORMALLY).callEvent();

				yield success;
			}
			case FULL -> {
				if (data.caster() == null) yield false;

				SpellCastEvent castEvent = spell.preCast(data);
				if (castEvent == null) yield false;

				PostCastAction action = PostCastAction.HANDLE_NORMALLY;
				boolean success = false;
				if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, data);
					if (targetEvent.callEvent()) {
						data = data.builder().location(targetEvent.getTargetLocation()).power(targetEvent.getPower()).build();

						success = ((TargetedLocationSpell) spell).castAtLocation(data);
					}
				}

				if (!success) action = PostCastAction.ALREADY_HANDLED;

				spell.postCast(castEvent, action);

				yield success;
			}
		};
	}

	public boolean castAtEntityFromLocation(SpellData data) {
		return castAtEntityFromLocation(data, passTargeting);
	}


	public boolean castAtEntityFromLocation(SpellData data, boolean passTargeting) {
		SpellData data_ = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).build();

		double chance = this.chance.get(data_);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return false;

		int delay = this.delay.get(data_);
		if (delay < 0) return castAtEntityFromLocationReal(data_, passTargeting);

		MagicSpells.scheduleDelayedTask(() -> castAtEntityFromLocationReal(data_, passTargeting), delay);

		return true;
	}

	private boolean castAtEntityFromLocationReal(SpellData data, boolean passTargeting) {
		if (!isTargetedEntityFromLocation) return false;

		data = data.builder().power((passPower ? data.power() : 1) * subPower.get(data)).build();

		return switch (mode) {
			case HARD -> {
				if (data.caster() == null) yield false;

				SpellCastResult result = spell.cast(data);
				yield result.state == SpellCastState.NORMAL && (result.action == PostCastAction.HANDLE_NORMALLY ||
					result.action == PostCastAction.NO_MESSAGES);
			}
			case DIRECT -> {
				if (passTargeting) yield passTargetingEntityFromLocation(data);
				else {
					TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
					yield targetedSpell.castAtEntityFromLocation(data);
				}
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL) yield false;

				data = data.builder().power(castEvent.getPower()).build();
				SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, data);
				if (!targetEntityEvent.callEvent()) yield false;

				data = data.builder().power(targetEntityEvent.getPower()).build();
				SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetLocationEvent.callEvent()) yield false;

				data = data.builder().target(targetEntityEvent.getTarget()).power(targetLocationEvent.getPower()).location(targetLocationEvent.getTargetLocation()).build();

				boolean success;
				if (passTargeting) success = passTargetingEntityFromLocation(data);
				else {
					TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
					success = targetedSpell.castAtEntityFromLocation(data);
				}

				if (success)
					new SpellCastedEvent(spell, SpellCastState.NORMAL, data, 0, null,
						PostCastAction.HANDLE_NORMALLY).callEvent();

				yield success;
			}
			case FULL -> {
				if (data.caster() == null) yield false;

				SpellCastEvent castEvent = spell.preCast(data);
				if (castEvent == null) yield false;

				PostCastAction action = PostCastAction.HANDLE_NORMALLY;
				boolean success = false;
				if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
					data = data.builder().power(castEvent.getPower()).build();
					SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, data);
					if (targetEntityEvent.callEvent()) {
						data = data.builder().target(targetEntityEvent.getTarget()).power(targetEntityEvent.getPower()).build();

						SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, data);
						if (targetLocationEvent.callEvent()) {
							data = data.builder().location(targetLocationEvent.getTargetLocation()).power(targetLocationEvent.getPower()).build();

							if (passTargeting)
								success = passTargetingEntityFromLocation(data);
							else
								success = ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(data);
						}
					}
				}

				if (success) {
					if (spell instanceof TargetedSpell targetedSpell) {
						action = PostCastAction.NO_MESSAGES;
						targetedSpell.sendMessages(data.caster(), data.target(), data.args());
					}
				} else action = PostCastAction.ALREADY_HANDLED;

				spell.postCast(castEvent, action);

				yield success;
			}
		};
	}

	public boolean passTargetingEntityFromLocation(SpellData data) {
		ValidTargetList list = spell.getValidTargetList();
		ValidTargetList originalList = list.clone();
		if (Objects.equals(data.caster(), data.target()) && !list.canTargetSelf()) list.setTargetCaster(true);
		if (!list.canTargetEntity(data.target())) {
			list.addEntityTarget(data.target());
			spell.setValidTargetList(list);
		}

		boolean success = data.caster() != null ? ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(data) : ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(data);
		spell.setValidTargetList(originalList);
		return success;
	}

	public void setCastMode(CastMode mode){
		this.mode = mode;
	}

	public enum CastMode {

		HARD("hard", "h"),
		FULL("full", "f"),
		PARTIAL("partial", "p"),
		DIRECT("direct", "d");

		private static final Map<String, CastMode> nameMap = new HashMap<>();

		private final String[] names;

		CastMode(String... names) {
			this.names = names;
		}

		public static CastMode getFromString(String label) {
			return nameMap.get(label.toLowerCase());
		}

		static {
			for (CastMode mode : CastMode.values()) {
				nameMap.put(mode.name().toLowerCase(), mode);
				for (String s : mode.names) {
					nameMap.put(s.toLowerCase(), mode);
				}
			}
		}

	}

	public enum CastTargeting {

		NORMAL,
		ENTITY_FROM_LOCATION,
		ENTITY,
		LOCATION,
		NONE

	}

}
