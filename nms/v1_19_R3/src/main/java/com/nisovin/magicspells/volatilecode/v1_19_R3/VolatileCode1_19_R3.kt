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

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.phys.Vec3

import com.nisovin.magicspells.volatilecode.VolatileCodeHandle
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

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

    private var displayEntityList: MutableMap<Display, ServerPlayer> = HashMap<Display, ServerPlayer>();

    override fun createFalsePlayer(player: Player?, location: Location?, pose: String, cloneEquipment: Boolean): Int {
        val entityPlayer = (player as CraftPlayer).handle
        val craftLocation = (location as Location)

        val property = entityPlayer.gameProfile.properties.get("textures").iterator().next()
        val gp = GameProfile(UUID.randomUUID(), player.name)
        gp.properties.put("textures", Property("textures", property.value, property.signature))

        val clone = ServerPlayer((Bukkit.getServer() as CraftServer).server, (player.world as CraftWorld).handle, gp)

        var yOffset = 0.0

        if (pose == "SLEEPING") {
            yOffset = 0.15
        } else if (pose == "SWIMMING" || pose == "FALL_FLYING") {
            yOffset = -0.15
        }

        clone.setPos(craftLocation.x, craftLocation.y + yOffset, craftLocation.z)
        clone.setRot(player.location.yaw, player.location.pitch)

        var direction = Direction.WEST

        if (player.location.yaw >= 135f || player.location.yaw < -135f) {
            direction = Direction.NORTH
        } else if (player.location.yaw >= -135f && player.location.yaw < -45f) {
            direction = Direction.EAST
        } else if (player.location.yaw >= -45f && player.location.yaw < 45f) {
            direction = Direction.SOUTH
        }

        val bedPos = BlockPos(craftLocation.getBlockX(), craftLocation.getWorld().getMinHeight(), craftLocation.getBlockZ())
        val setBedPacket = ClientboundBlockUpdatePacket(bedPos, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, direction.getOpposite()).setValue(BedBlock.PART, BedPart.HEAD))
        val teleportNpcPacket = ClientboundTeleportEntityPacket(clone)

        //show outer skin layer
        clone.entityData.set(EntityDataAccessor(17, EntityDataSerializers.BYTE), 127.toByte())

        if (pose != "") {
            if (enumValues<nmsEntityPose>().map { it.name }.contains(pose)) {
                clone.pose = nmsEntityPose.valueOf(pose)

                if (pose == "SLEEPING") {
                    clone.entityData.set(EntityDataSerializers.OPTIONAL_BLOCK_POS.createAccessor(14), Optional.of(bedPos))
                }
            }
        }

        val equipment: MutableList<com.mojang.datafixers.util.Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> = mutableListOf()

        if (cloneEquipment) {
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

            connection.send(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, clone))
            connection.send(ClientboundAddPlayerPacket(clone))
            connection.send(ClientboundSetEntityDataPacket(clone.getId(), clone.entityData.getNonDefaultValues()))
            connection.send(ClientboundRotateHeadPacket(clone, (player.location.yaw % 360.0 * 256 / 360).toInt().toByte()))

            if (cloneEquipment) {
                connection.send(ClientboundSetEquipmentPacket(clone.id, equipment));
            }

            if (pose == "SLEEPING") {
                connection.send(setBedPacket)
                connection.send(teleportNpcPacket)
            }
        })

        val markerEntity: Display = player.world.spawnEntity(Location(player.world, clone.x, clone.y, clone.z), org.bukkit.entity.EntityType.BLOCK_DISPLAY) as Display
        markerEntity.displayHeight = 1.0f
        markerEntity.displayWidth = 1.0f
        displayEntityList[markerEntity] = clone //Create a Display Entity and spawn it ontop of the player. No collider means easy way to track positioning
        return clone.id
    }

    override fun removeFalsePlayer(id: Int) {
        //Discovering now a possible problem with CloneMap being stored in the CloneSpell. I can't remove the clone if the display entity is destroyed.
        Bukkit.getOnlinePlayers().forEach(action = {
            val serverPlayer: ServerPlayer = (it as CraftPlayer).handle

            val connection: ServerGamePacketListenerImpl = serverPlayer.connection

            connection.send(ClientboundRemoveEntitiesPacket(id))
        })
        if (getDisplayFromID(id) != null) {   //Dont want to keep removed clones in the track list
            displayEntityList.remove(getDisplayFromID(id));
        }
    }

    override fun updateFalsePlayer(entityDisplay: Display) {
        if (!entityDisplay.isValid) {
            val clone: ServerPlayer? = displayEntityList.remove(entityDisplay)

            if (clone != null) {   //If the display entity was removed that implies that the false player should be removed as well.
                removeFalsePlayer(clone.id)
            }
        } else {
            val clone: ServerPlayer = displayEntityList[entityDisplay]
                    ?: return
            val displayLocation = entityDisplay.location
            clone.setPos(displayLocation.x, displayLocation.y, displayLocation.z)    //Change the NPC Location to the displayEntities location
            val teleportPacket = ClientboundTeleportEntityPacket(clone)      //Update it for all players on the server
            for (player in Bukkit.getOnlinePlayers()) {
                (player as CraftPlayer).handle.connection.send(teleportPacket)
            }
        }
    }

    override fun isRelatedToFalsePlayer(entityDisplay: Display?): Boolean {
        return displayEntityList.contains(entityDisplay);   //Returns true if the Display Entity relates to a Server Player
        //More going to be used for a Passive Spell to avoid bug triggers
    }

    override fun updateAllFalsePlayers() {
        for (entityDisplay: Display in displayEntityList.keys) {     //Update all EntityPlayer positions (Useful for a random trigger or AOE Effects)
            updateFalsePlayer(entityDisplay)
        }
    }

    private fun getDisplayFromID(id: Int): Display? {
        for (entityDisplay: Display in displayEntityList.keys) {     //Need to obtain the Display Entity from the entity id sometimes
            if (displayEntityList[entityDisplay]!!.id == id) {
                return entityDisplay
            }
        }
        return null
    }

    override fun nicknamePlayer(player: Player?, nickname: String?): String {
        val sPlayer: ServerPlayer = ((player as CraftPlayer)).handle;
        val OGName = player.name;
        val newGP = GameProfile(player.uniqueId, nickname);
        sPlayer.gameProfile = newGP;

        for (otherPlayer in Bukkit.getOnlinePlayers()) {
            if (otherPlayer == player) continue
            otherPlayer.hidePlayer(helper.instance, player)
            otherPlayer.showPlayer(helper.instance, player)
        }
        return OGName;
    }
}
