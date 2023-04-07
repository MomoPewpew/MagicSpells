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
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.*

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

    override fun createFalsePlayer(player: Player?, isSleeping: Boolean, cloneEquipment: Boolean, inventory: Inventory): Int {
        val entityPlayer = (player as CraftPlayer).handle

        val property = entityPlayer.gameProfile.properties.get("textures").iterator().next()
        val gp = GameProfile(UUID.randomUUID(), "")
        gp.properties.put("textures", Property("textures", property.value, property.signature))

        val corpse = ServerPlayer((Bukkit.getServer() as CraftServer).server, (player.world as CraftWorld).handle, gp)

        corpse.setPos(player.location.x, player.location.y, player.location.z)

        if(isSleeping){
            corpse.pose = nmsEntityPose.SLEEPING
        }

        val equipment: MutableList<Pair<EquipmentSlot, nmsItemStack>> = ArrayList()
        if(cloneEquipment){
            //Supplied inventory argument will always be a player inventory, but in the event that it is not, I am directly getting the players inventory
            //to handle setting the equipment up (A possible option to make use of the Inventory argument would be to make it PlayerInventory instead)
            equipment += Pair<EquipmentSlot, nmsItemStack>(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(player.inventory.boots))
            equipment += Pair<EquipmentSlot, nmsItemStack>(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(player.inventory.leggings))
            equipment += Pair<EquipmentSlot, nmsItemStack>(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(player.inventory.chestplate))
            equipment += Pair<EquipmentSlot, nmsItemStack>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(player.inventory.helmet))
            equipment += Pair<EquipmentSlot, nmsItemStack>(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(player.inventory.itemInMainHand))
            equipment += Pair<EquipmentSlot, nmsItemStack>(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(player.inventory.itemInOffHand))

        }

        Bukkit.getOnlinePlayers().forEach(action = {
            val serverPlayer: ServerPlayer = (it as CraftPlayer).handle

            val connection: ServerGamePacketListenerImpl = serverPlayer.connection

            connection.send(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, corpse))
            //connection.send(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, corpse))
            connection.send(ClientboundAddPlayerPacket(corpse))

            connection.send(ClientboundSetEquipmentPacket(corpse.id, equipment))

            connection.send(ClientboundSetEntityDataPacket(corpse.id, corpse.entityData.packDirty()))

            /*
            * The below code sets the rotation/position of the playerEntity after it is spawned.
            * This DOES rotate the sleeping players so they do not all sit in the same direction
            * However: This is not very consistant in how it rotates them.
            * Sometimes, it will be the same direction the player is looking,
            * Other times it will be the opposite,
            * and sometimes it will default to one direction no matter how you look.
            * Could be something wrong with my math.. I do not know 100%
            * */

            //connection.send(ClientboundMoveEntityPacket.PosRot(corpse.id, 0, 0, 0,
            //        (player.location.yaw * 256.0f / 360.0f).toInt().toByte(), 0, false))

            //connection.send(ClientboundPlayerInfoRemovePacket(arrayListOf(corpse.uuid)))
        })

    	return corpse.getId()
    }

	override fun removeFalsePlayer(id: Int) {
        Bukkit.getOnlinePlayers().forEach(action = {
            val serverPlayer: ServerPlayer = (it as CraftPlayer).handle

            val connection: ServerGamePacketListenerImpl = serverPlayer.connection

            connection.send(ClientboundRemoveEntitiesPacket(id))
        })
    }
}
