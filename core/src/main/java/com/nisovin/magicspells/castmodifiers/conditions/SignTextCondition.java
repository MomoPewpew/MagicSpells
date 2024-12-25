package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.block.Sign;
import org.bukkit.block.Block;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class SignTextCondition extends Condition {

	//world,x,y,z,text

	private MagicLocation location;
	private List<Component> text;

	@Override
	public boolean initialize(String var) {
		try {
			String[] vars = var.split(",");
			location = new MagicLocation(vars[0], Integer.parseInt(vars[1]), Integer.parseInt(vars[2]), Integer.parseInt(vars[3]));

			text = new ArrayList<>();
			for (String line : vars[4].split("\\\\n")) {
				text.add(Util.getLegacyFromString(line.replaceAll("__", " ")));
			}
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean checkCaster(SpellData data) {
		return checkSignText();
	}

	@Override
	public boolean checkTarget(SpellData data) {
		return checkSignText();
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return checkSignText();
	}

	public boolean checkSignText() {
		Block block = location.getLocation().getBlock();
		if (!block.getType().name().contains("SIGN")) return false;

		Sign sign = (Sign) block.getState();
		List<Component> lines = sign.lines();
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equals(text.get(i))) continue;
			return false;
		}

		return true;
	}

}
