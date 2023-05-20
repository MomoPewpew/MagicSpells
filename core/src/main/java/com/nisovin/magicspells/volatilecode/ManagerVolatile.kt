package com.nisovin.magicspells.volatilecode

import org.bukkit.Bukkit

import com.nisovin.magicspells.MagicSpells
import org.bukkit.plugin.Plugin

object ManagerVolatile {

    private val helper = object: VolatileCodeHelper {
        override fun error(message: String) {
            MagicSpells.error(message)
        }

        override fun scheduleDelayedTask(task: Runnable?, delay: Long): Int {
            return MagicSpells.scheduleDelayedTask(task, delay)
        }

        override fun getInstance(): Plugin {
            return MagicSpells.getInstance()
        }
    }

    fun constructVolatileCodeHandler(): VolatileCodeHandle {
        try {
            val nmsPackage = Bukkit.getServer().javaClass.getPackage().name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[3]
            val volatileCode = Class.forName("com.nisovin.magicspells.volatilecode.$nmsPackage.VolatileCode${nmsPackage.replace("v", "")}")

            MagicSpells.log("Found volatile code handler for $nmsPackage.")
            return volatileCode.getConstructor(VolatileCodeHelper::class.java).newInstance(helper) as VolatileCodeHandle
        } catch (ex: Exception) {
            MagicSpells.log("Volatile code handler not found, using fallback.")
            return VolatileCodeDisabled()
        }
    }
}
