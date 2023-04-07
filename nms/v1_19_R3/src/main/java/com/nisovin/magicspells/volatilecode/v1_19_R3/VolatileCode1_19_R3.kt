package com.nisovin.magicspells.volatilecode.v1_19_R3

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.datafixers.util.Pair
import org.bukkit.Bukkit
import org.bukkit.entity.*
import org.bukkit.Location
import org.bukkit.util.Vector
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.event.entity.ExplosionPrimeEvent

import org.bukkit.craftbukkit.v1_19_R3.entity.*
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld
import org.bukkit.craftbukkit.v1_19_R3.CraftServer
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack

import net.minecraft.world.phys.Vec3
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

import com.nisovin.magicspells.volatilecode.VolatileCodeHandle
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.*
import kotlin.collections.ArrayList

private typealias nmsItemStack = net.minecraft.world.item.ItemStack
private typealias nmsEntityPose = net.minecraft.world.entity.Pose

class VolatileCode1_19_R3(helper: VolatileCodeHelper) : VolatileCodeHandle(helper) {

    private var entityLivingPotionEffectColor: EntityDataAccessor<Int>? = null

    init {
        try {
            // CHANGE THIS TO SPIGOT MAPPING VERSION OF MOJANG'S - EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID
            val entityLivingPotionEffectColorField = net.minecraft.world.entity.LivingEntity::class.java.getDeclaredField("bG")
            entityLivingPotionEffectColorField.isAccessible = true
            entityLivingPotionEffectColor = entityLivingPotionEffectColorField.get(null) as EntityDataAccessor<Int>
        } catch (e: Exception) {
            helper.error("THIS OCCURRED WHEN CREATING THE VOLATILE CODE HANDLE FOR 1.19.4, THE FOLLOWING ERROR IS MOST LIKELY USEFUL IF YOU'RE RUNNING THE LATEST VERSION OF MAGICSPELLS.")
            e.printStackTrace()
        }
    }

    override fun addPotionGraphicalEffect(entity: LivingEntity, color: Int, duration: Long) {
        val livingEntity = (entity as CraftLivingEntity).handle
        val entityData = livingEntity.entityData
        entityData.set(entityLivingPotionEffectColor, color)

        if (duration > 0) {
            helper.scheduleDelayedTask({
                var c = 0
                if (livingEntity.getActiveEffects().isNotEmpty()) {
                    c = PotionUtils.getColor(livingEntity.getActiveEffects())
                }
                entityData.set(entityLivingPotionEffectColor, c)
            }, duration)
        }
    }

    override fun sendFakeSlotUpdate(player: Player, slot: Int, item: ItemStack?) {
        val nmsItem: nmsItemStack?
        if (item != null) nmsItem = CraftItemStack.asNMSCopy(item)
        else nmsItem = null

        val packet = ClientboundContainerSetSlotPacket(0, 0, slot.toShort() + 36, nmsItem!!)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun simulateTnt(target: Location, source: LivingEntity, explosionSize: Float, fire: Boolean): Boolean {
        val e = PrimedTnt((target.world as CraftWorld).handle, target.x, target.y, target.z, (source as CraftLivingEntity).handle)
        val c = CraftTNTPrimed(Bukkit.getServer() as CraftServer, e)
        val event = ExplosionPrimeEvent(c, explosionSize, fire)
        Bukkit.getPluginManager().callEvent(event)
        return event.isCancelled
    }

    override fun setFallingBlockHurtEntities(block: FallingBlock, damage: Float, max: Int) {
        val efb = (block as CraftFallingBlock).handle
        block.setHurtEntities(true)
        efb.setHurtsEntities(damage, max)
    }

    override fun playDragonDeathEffect(location: Location) {
        val dragon = EnderDragon(EntityType.ENDER_DRAGON, (location.world as CraftWorld).handle)
        dragon.setPos(location.x, location.y, location.z)

        val addMobPacket = ClientboundAddEntityPacket(dragon)
        val entityEventPacket = ClientboundEntityEventPacket(dragon, 3)
        val removeEntityPacket = ClientboundRemoveEntitiesPacket(dragon.id)

        val players = ArrayList<Player>()
        for (player in location.getNearbyPlayers(64.0)) {
            players.add(player)
            (player as CraftPlayer).handle.connection.send(addMobPacket)
            player.handle.connection.send(addMobPacket)
            player.handle.connection.send(entityEventPacket)
        }

        helper.scheduleDelayedTask({
            for (player in players) {
                if (!player.isValid) continue
                (player as CraftPlayer).handle.connection.send(removeEntityPacket)
            }
        }, 250)

    }

    override fun setClientVelocity(player: Player, velocity: Vector) {
        val packet = ClientboundSetEntityMotionPacket(player.entityId, Vec3(velocity.x, velocity.y, velocity.z))
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun setInventoryTitle(player: Player, title: String) {
        val entityPlayer = (player as CraftPlayer).handle
        val container = entityPlayer.containerMenu
        val packet = ClientboundOpenScreenPacket(container.containerId, container.type, Component.literal(title))

        player.handle.connection.send(packet)
        player.updateInventory()
    }

    override fun startAutoSpinAttack(player: Player?, ticks: Int) {
        val entityPlayer = (player as CraftPlayer).handle
        entityPlayer.startAutoSpinAttack(ticks)
    }

    override fun createFalsePlayer(player: Player?, pose: String, cloneEquipment: Boolean): Int {
        val entityPlayer = (player as CraftPlayer).handle

        val property = entityPlayer.gameProfile.properties.get("textures").iterator().next()
        val gp = GameProfile(UUID.randomUUID(), "")
        gp.properties.put("textures", Property("textures", property.value, property.signature))

        val clone = ServerPlayer((Bukkit.getServer() as CraftServer).server, (player.world as CraftWorld).handle, gp)

        clone.setPos(player.location.x, player.location.y, player.location.z)
        clone.setRot(player.location.yaw, player.location.pitch)

		//show outer skin layer
    	clone.entityData.set(EntityDataAccessor(17, EntityDataSerializers.BYTE), 127.toByte())

        if(pose != ""){
		    clone.pose = nmsEntityPose.valueOf(pose)
        }

		val equipment: MutableList<com.mojang.datafixers.util.Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> = mutableListOf()

		if (cloneEquipment) {
			val inventory = entityPlayer.getInventory()

			if (inventory.getItem(36) != null) {
			    equipment.add(Pair(EquipmentSlot.FEET, inventory.getItem(36)))
			}
			if (inventory.getItem(37) != null) {
			    equipment.add(Pair(EquipmentSlot.LEGS, inventory.getItem(37)))
			}
			if (inventory.getItem(38) != null) {
			    equipment.add(Pair(EquipmentSlot.CHEST, inventory.getItem(38)))
			}
			if (inventory.getItem(39) != null) {
			    equipment.add(Pair(EquipmentSlot.HEAD, inventory.getItem(39)))
			}
			if (inventory.getItem(0) != null) {
			    equipment.add(Pair(EquipmentSlot.MAINHAND, inventory.getItem(0)))
			}
			if (inventory.getItem(40) != null) {
			    equipment.add(Pair(EquipmentSlot.OFFHAND, inventory.getItem(40)))
			}
		}

        Bukkit.getOnlinePlayers().forEach(action = {
            val serverPlayer: ServerPlayer = (it as CraftPlayer).handle

            val connection: ServerGamePacketListenerImpl = serverPlayer.connection

            connection.send(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, clone))
            connection.send(ClientboundAddPlayerPacket(clone))
            connection.send(ClientboundSetEntityDataPacket(clone.id, clone.entityData.packDirty()))
            connection.send(ClientboundRotateHeadPacket(clone, (player.location.yaw % 360.0 * 256 / 360).toInt().toByte()))

    		if (cloneEquipment) {
            	connection.send(ClientboundSetEquipmentPacket(clone.id, equipment));
	        }
        })

    	return clone.getId()
    }

	override fun removeFalsePlayer(id: Int) {
        Bukkit.getOnlinePlayers().forEach(action = {
            val serverPlayer: ServerPlayer = (it as CraftPlayer).handle

            val connection: ServerGamePacketListenerImpl = serverPlayer.connection

            connection.send(ClientboundRemoveEntitiesPacket(id))
        })
    }
}
