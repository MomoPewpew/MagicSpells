package com.nisovin.magicspells;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class MagicLogger implements Listener {

	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	FileWriter writer;
	boolean debug;
	boolean enableLogging;
	
	public MagicLogger(MagicSpells plugin, boolean debug, boolean enableLogging) {
		this.debug = debug;
		this.enableLogging = enableLogging;

		if (enableLogging) {
			File file = new File(plugin.getDataFolder(), "log-" + System.currentTimeMillis() + ".txt");
			try {
				writer = new FileWriter(file, true);
			} catch (IOException e) {
				MagicSpells.handleException(e);
			}
		} else {
			writer = null;
		}

		MagicSpells.registerEvents(this);
	}
	
	public void disable() {
		if (writer != null) {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				MagicSpells.handleException(e);
			}
		}
		writer = null;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellLearn(SpellLearnEvent event) {
		Spell spell = event.getSpell();
		String msg = "&eLEARN" + 
			"&7; spell=&a" + spell.getInternalName();
		String params = "&7; player=&f" + event.getLearner().getName() + 
			"&7; loc=&f" + formatLoc(event.getLearner().getLocation()) +
			"&7; source=&f" + event.getSource().name() +
			"&7; teacher=&f" + getTeacherName(event.getTeacher()) +
			"&7; canceled=&f" + event.isCancelled();
		if (enableLogging) log((msg + params).replaceAll("&([0-9a-f])", ""));
		if (debug) {
			if (spell.getDebugLevel() >= 3) {
				MagicSpells.sendDebugMessage(msg + params);
			} else if (spell.getDebugLevel() >= 2) {
				MagicSpells.sendDebugMessage(msg);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellForget(SpellForgetEvent event) {
		Spell spell = event.getSpell();
		String msg = "&eFORGET" + 
			"&7; spell=&a" + spell.getInternalName();
		String params = "&7; player=&f" + event.getForgetter().getName() + 
			"&7; loc=&f" + formatLoc(event.getForgetter().getLocation()) +
			"&7; canceled=&f" + event.isCancelled();
		if (enableLogging) log((msg + params).replaceAll("&([0-9a-f])", ""));
		if (debug) {
			if (spell.getDebugLevel() >= 3) {
				MagicSpells.sendDebugMessage(msg + params);
			} else if (spell.getDebugLevel() >= 2) {
				MagicSpells.sendDebugMessage(msg);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellCast(SpellCastEvent event) {
		Spell spell = event.getSpell();
		String msg = "&eBEGIN CAST" + 
			"&7; spell=&a" + spell.getInternalName();
		String params = "&7; caster=&f" + event.getCaster().getName() + 
			"&7; loc=&f" + formatLoc(event.getCaster().getLocation()) +
			"&7; state=&f" + event.getSpellCastState().name() +
			"&7; power=&f" + event.getPower() +
			"&7; canceled=&f" + event.isCancelled();
		if (enableLogging) log((msg + params).replaceAll("&([0-9a-f])", ""));
		if (debug) {
			if (spell.getDebugLevel() >= 3) {
				MagicSpells.sendDebugMessage(msg + params);
			} else if (spell.getDebugLevel() >= 1) {
				MagicSpells.sendDebugMessage(msg);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellTarget(SpellTargetEvent event) {
		Spell spell = event.getSpell();
		Player caster = event.getCaster();
		String msg = "&eTARGET ENTITY" + 
			"&7; spell=&a" + spell.getInternalName();
		String params = "&7; caster=&f" + (caster != null ? caster.getName() : "null") + 
			"&7; casterloc=&f" + (caster != null ? formatLoc(caster.getLocation()) : "null") +
			"&7; target=&f" + getTargetName(event.getTarget()) + 
			"&7; targetloc=&f" + formatLoc(event.getTarget().getLocation()) +
			"&7; canceled=&f" + event.isCancelled();
		if (enableLogging) log("  " + (msg + params).replaceAll("&([0-9a-f])", ""));
		if (debug) {
			if (spell.getDebugLevel() >= 3) {
				MagicSpells.sendDebugMessage(msg + params);
			} else if (spell.getDebugLevel() >= 2) {
				MagicSpells.sendDebugMessage(msg);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellTargetLocation(SpellTargetLocationEvent event) {
		Spell spell = event.getSpell();
		String msg = "&eTARGET LOCATION" + 
			"&7; spell=&a" + spell.getInternalName();
		String params = "&7; caster=&f" + event.getCaster().getName() + 
			"&7; casterloc=&f" + formatLoc(event.getCaster().getLocation()) +
			"&7; targetloc=&f" + formatLoc(event.getTargetLocation()) +
			"&7; canceled=&f" + event.isCancelled();
		if (enableLogging) log("  " + (msg + params).replaceAll("&([0-9a-f])", ""));
		if (debug) {
			if (spell.getDebugLevel() >= 3) {
				MagicSpells.sendDebugMessage(msg + params);
			} else if (spell.getDebugLevel() >= 2) {
				MagicSpells.sendDebugMessage(msg);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellCasted(SpellCastedEvent event) {
		Spell spell = event.getSpell();
		String msg = "&eEND CAST" + 
			"&7; spell=&a" + spell.getInternalName();
		String params = "&7; caster=&f" + event.getCaster().getName() + 
			"&7; loc=&f" + formatLoc(event.getCaster().getLocation()) +
			"&7; state=&f" + event.getSpellCastState().name() +
			"&7; power=&f" + event.getPower() +
			"&7; result=&f" + event.getPostCastAction().name();
		if (enableLogging) log((msg + params).replaceAll("&([0-9a-f])", ""));
		if (debug) {
			if (spell.getDebugLevel() >= 3) {
				MagicSpells.sendDebugMessage(msg + params);
			} else if (spell.getDebugLevel() >= 1) {
				MagicSpells.sendDebugMessage(msg);
			}
		}
	}
	
	private String formatLoc(Location location) {
		return location.getWorld().getName() + ',' + location.getBlockX() + ',' + location.getBlockY() + ',' + location.getBlockZ();
	}
	
	private String getTargetName(LivingEntity target) {
		if (target instanceof Player) return target.getName();
		return target.getType().name();
	}
	
	private String getTeacherName(Object o) {
		if (o == null) return "none";
		if (o instanceof Player) return "player-" + ((Player)o).getName();
		if (o instanceof Spell) return "spell-" + ((Spell)o).getInternalName();
		if (o instanceof Block) return "block-" + formatLoc(((Block)o).getLocation());
		return o.toString();
	}
	
	private void log(String string) {
		if (writer != null) {
			try {
				writer.write('[' + dateFormat.format(new Date()) + "] " + string + '\n');
			} catch (IOException e) {
				DebugHandler.debugIOException(e);
			}
		}
	}
	
}
