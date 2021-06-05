package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

//THESE IMPORTS HERE ARE VOLATILE, THE CORRECT VERSION OF MINECRAFT MUST BE USED
//currently configured only for 1.12 mc
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import net.minecraft.server.v1_12_R1.NBTBase;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.EntityLiving;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

import java.util.Objects;

public class NBTDebug extends Condition {

	String[] key;

	@Override
	public boolean setVar(String var) {
		key = var.split(".");
		if(key.length == 0) {
			String[] t = {var};
			key = t;
		}
		return true;
	}

	@Override
	public boolean check(Player player) {
		return check((LivingEntity)player);
	}

	public boolean check(LivingEntity target) {
		NBTTagCompound compound = new NBTTagCompound();

		EntityLiving el = ((CraftLivingEntity)target).getHandle();

    	el.b(compound);

		boolean succ = true;
		for (int i = 0; i < key.length - 1; i++) {
			if (compound.hasKeyOfType(key[i], 10)) {
				compound = compound.getCompound(key[i]);
			} else {
				succ = false;
				break;
			}
		}

		if(succ) {
			MagicSpells.sendDebugMessage(compound.toString());
			if(compound.hasKey(key[key.length - 1])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
