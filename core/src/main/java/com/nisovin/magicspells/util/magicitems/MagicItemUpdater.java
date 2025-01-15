package com.nisovin.magicspells.util.magicitems;

import java.io.File;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.configuration.file.YamlConfiguration;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;

import net.sneakymouse.sneakyvaults.SneakyVaults;
import net.sneakymouse.sneakyvaults.utlitiy.ChatUtility;

import net.sneakycharactermanager.paper.SneakyCharacterManager;
import net.sneakycharactermanager.paper.handlers.character.LoadCharacterEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.compat.CompatBasics;

import static com.nisovin.magicspells.MagicSpells.setCheckItemPersistentData;
import static com.nisovin.magicspells.util.magicitems.MagicItems.getMagicItems;

public class MagicItemUpdater {
    // This file is my own person nightmare of not knowing how multi-threading works
    // There is a chance that this may crash if you have too many players, characters, or vaults

    private static MagicItemUpdater.PersistentDataUpdater persistentDataUpdater = new PersistentDataUpdater();

    private static final Map<String, MagicItem> magicItems = getMagicItems();
    private static final Map<String, MagicItemData> magicItemsCache = new HashMap<>();
    private static final NamespacedKey NAME_KEY = new NamespacedKey(MagicSpells.getInstance(), "magicitem");

    // Active updating of items on login/inventory open
    public static class PersistentDataUpdater implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onJoin(PlayerJoinEvent event) {
            if (!MagicSpells.enableUpdateItemData()) return;
            PlayerInventory inv = event.getPlayer().getInventory();
            updateInventory(inv);
            ItemStack[] armor = inv.getArmorContents();
            updateInventory(armor);
            inv.setArmorContents(armor);
        }

        public void joinOrLoadCharacter(Player player) {
            if (!MagicSpells.enableUpdateItemData()) return;
            MagicSpells.error("UpdateItems");
            PlayerInventory inv = player.getInventory();
            updateInventory(inv);
            ItemStack[] armor = inv.getArmorContents();
            updateInventory(armor);
            inv.setArmorContents(armor);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onInvOpen(InventoryOpenEvent event) {
            if (!MagicSpells.enableUpdateItemData()) return;
            updateInventory(event.getInventory());
        }

        private void updateInventory(Inventory inv) {
            ItemStack[] contents = inv.getContents();
            updateInventory(contents);
            inv.setContents(contents);
        }

        private void updateInventory(ItemStack[] items) {
            if (items == null) return;
            for (int i = 0; i < items.length; i++) {
                ItemStack itemStack = items[i];
                if (itemStack == null) continue;

                ItemMeta meta = itemStack.getItemMeta();
                if (meta == null) continue;

                PersistentDataContainer container = meta.getPersistentDataContainer();

                String magicitemName = null;

                if (container.has(NAME_KEY, PersistentDataType.STRING)) {
                    magicitemName = container.get(NAME_KEY, PersistentDataType.STRING);
                }

                if (magicitemName != null && magicItems.containsKey(magicitemName)) {
                    MagicItemData stackData = MagicItems.getMagicItemDataFromItemStack(itemStack);
                    MagicItemData magicItemData = MagicItems.getMagicItemDataByInternalName(magicitemName);

                    if (magicItemData == null || stackData == null) continue;
                    if (magicItemData.matches(stackData)) continue;

                    items[i] = updateItem(itemStack, magicItems.get(magicitemName));
                }
            }
        }
    }

    public static class CharacterPersistentDataUpdater implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        private void onCharacterLoad(LoadCharacterEvent event) {
            if (!event.isCancelled()) {
                MagicSpells.scheduleDelayedTask(() -> {
                    persistentDataUpdater.joinOrLoadCharacter(event.getPlayer());
                }, 1);
            }
        }
    }

    private static ItemStack updateItem(ItemStack itemStack, MagicItem magicItem) {
        Integer durability = null;
        Long expiresAt = null;
        String creatorName = null;
        int amount = itemStack.getAmount();
        BookMeta bookMeta = null;

        if (itemStack.getItemMeta() instanceof Damageable damageable) {
            durability = damageable.getDamage();
        }

        ItemMeta sourceMeta = itemStack.getItemMeta();

        PersistentDataContainer sourceContainer = sourceMeta.getPersistentDataContainer();

        if (sourceContainer.has(new NamespacedKey(MagicSpells.getInstance(), "expires_at"), PersistentDataType.LONG)) {
            expiresAt = sourceContainer.get(new NamespacedKey(MagicSpells.getInstance(), "expires_at"), PersistentDataType.LONG);
        }

        if (sourceContainer.has(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING)) {
            creatorName = sourceContainer.get(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING);
        }

        if (sourceMeta instanceof BookMeta sourceBookMeta) {
            bookMeta = sourceBookMeta.clone();
        }

        ItemStack updatedItem = magicItem.getItemStack().clone();
        updatedItem.setAmount(amount);

        ItemMeta meta = updatedItem.getItemMeta();
        if (durability != null && durability != 0 && meta instanceof Damageable updatedDamageable) {
            updatedDamageable.setDamage(durability);
        }
        if (expiresAt != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(MagicSpells.getInstance(), "expires_at"), PersistentDataType.LONG, expiresAt);
        }

        if (creatorName != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(MagicSpells.getInstance(), "creator_name"), PersistentDataType.STRING, creatorName);
        }

        if (meta instanceof BookMeta updatedBookMeta && bookMeta != null) {
            updatedBookMeta.setTitle(bookMeta.getTitle());
            updatedBookMeta.setAuthor(bookMeta.getAuthor());
            updatedBookMeta.setPages(bookMeta.getPages());
            updatedBookMeta.setGeneration(bookMeta.getGeneration());
        }

        updatedItem.setItemMeta(meta);

        return updatedItem;
    }

    // Add PDC to all items for worlds used prior to PDC
    public static void addMagicItemPDC(boolean updatePlayers, boolean updateWorlds,
                                       boolean updateVaults, boolean updateCharacters) {

        MagicSpells.log(NamedTextColor.BLUE + "Adding PDC to Magic Items.");
        cacheItemData();
        setCheckItemPersistentData(false);

        // Update Player Inventories
        if (updatePlayers) updatePlayers();

        // Update Characters
        if (updateCharacters && CompatBasics.pluginEnabled("SneakyCharacterManager")) updateCharacters();

        // Update Vaults
        if (updateVaults && CompatBasics.pluginEnabled("SneakyVaults")) updateVaults();

        // Update Chunks
        if (updateWorlds) scanWorlds();
    }

    private static void cacheItemData() {
        // Cache data for performance purposes when adding PDC
        for (Map.Entry<String, MagicItem> entry : magicItems.entrySet()) {
            String key = entry.getKey();
            MagicItem value = entry.getValue();
            magicItemsCache.put(key, value.getMagicItemData());
        }
    }

    private static void addItemNames(ItemStack[] items) {
        if (items == null) return;

        for (int i = 0; i < items.length; i++) {
            ItemStack itemStack = items[i];
            if (itemStack == null) continue;

            MagicItemData item = MagicItems.getMagicItemDataFromItemStack(itemStack);
            if (item == null) continue;

            for (Map.Entry<String, MagicItemData> entry : magicItemsCache.entrySet()) {
                if (entry.getValue().matches(item)) {

                    items[i] = updateItem(itemStack, magicItems.get(entry.getKey()));
                    break;
                }
            }
        }
    }

    private static void updatePlayers() {
        CMI cmi = CMI.getInstance();
        List<CMIUser> users = new ArrayList<>(cmi.getPlayerManager().getAllUsers().values());
        int batchSize = 5;
        Iterator<CMIUser> userIterator = users.iterator();

        MagicSpells.log("Starting player update task...");

        Bukkit.getScheduler().runTaskTimer(MagicSpells.getInstance(), new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < batchSize && userIterator.hasNext(); i++) {
                    CMIUser user = userIterator.next();
                    Player player = user.getPlayer();
                    MagicSpells.log("Updating user " + user.getName() + ".");

                    if (player == null) {
                        MagicSpells.error("User " + user.getName() + " is null!");
                        continue;
                    }

                    Inventory inventory = player.getInventory();
                    ItemStack[] items = inventory.getContents();
                    Inventory enderChest = player.getEnderChest();
                    ItemStack[] enderChestContents = enderChest.getContents();

                    addItemNames(items);
                    addItemNames(enderChestContents);

                    inventory.setContents(items);
                    enderChest.setContents(enderChestContents);

                    player.updateInventory();
                    cmi.save(player);
                }

                if (!userIterator.hasNext()) {
                    MagicSpells.log(NamedTextColor.BLUE + "Finished updating all players.");
                    Bukkit.getScheduler().cancelTasks(MagicSpells.getInstance());
                }
            }
        }, 0L, 1L);
    }

    private static void updateCharacters() {
        // Adapted from SneakyCharacterManager by MomoPewPew & ItzBungo
        if (!SneakyCharacterManager.getInstance().getConfig().getBoolean("manageInventories", true)) {
            MagicSpells.error("manageInventories is currently set to false in the config, so you probably don't want this command.");
            return;
        }
        File dir = SneakyCharacterManager.getCharacterDataFolder();

        File[] playerData = dir.listFiles();
        if (playerData == null || playerData.length == 0) return;

        for (File playerDir : playerData) {
            if(!playerDir.exists() || !playerDir.isDirectory()) continue;
            File[] characterData = playerDir.listFiles();
            if (characterData == null) continue;

            for (File characterFile : characterData) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(characterFile);
                String inventoryB64 = config.getString("inventory");
                if (inventoryB64 == null) continue;

                ItemStack[] items = net.sneakycharactermanager.paper.util.InventoryUtility.getSavedInventory(inventoryB64);
                if (items == null || items.length == 0) continue;

                addItemNames(items);

                inventoryB64 = net.sneakycharactermanager.paper.util.InventoryUtility.inventoryToBase64(41, items);
                config.set("inventory", inventoryB64);

                try {
                    config.save(characterFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        MagicSpells.log(NamedTextColor.BLUE + "Updated items in SneakyCharacterManager.");
    }

    private static void updateVaults() {
        // Adapted from SneakyVaults written by ItzBungo
        File dir = SneakyVaults.getInstance().playerDataFolder;
        if(!dir.exists() || !dir.isDirectory()) {
            MagicSpells.error("SneakyVaults folder not found.");
            return;
        }
        File[] playerDataFiles = dir.listFiles();

        if(playerDataFiles == null || playerDataFiles.length == 0) {
            MagicSpells.error("No SneakyVaults data files found.");
            return;
        }

        for(File playerDataFile : playerDataFiles) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerDataFile);
            ConfigurationSection vaults = configuration.getConfigurationSection("player_vaults");
            if(vaults == null) continue;

            for(String vault : vaults.getKeys(false)) {
                if(!vaults.getBoolean(vault + ".paperConverted")) {
                    MagicSpells.error("Vault " + vault + " not found.");
                    continue;
                }

                String vaultInventory = vaults.getString(vault);
                if(vaultInventory == null) continue;
                List<String> vaultItems = vaults.getStringList(vault + ".items");

                Inventory inventory = Bukkit.createInventory(null, 9);

                ItemStack[] items = net.sneakymouse.sneakyvaults.utlitiy.InventoryUtility.inventoryPaperFromBase64(vaultItems).toArray(new ItemStack[0]);
                if(items.length > inventory.getSize()) {
                    if(items.length % 9 != 0) {
                        MagicSpells.error("Error: Saved inventory size is not a multiple of 9. This should be impossible!");
                        continue;
                    }
                    inventory = Bukkit.createInventory(null, items.length, ChatUtility.convertToComponent("&ePlayer Vault"));
                }

                addItemNames(items);
                inventory.setContents(items);

                List<String> itemEncoded = net.sneakymouse.sneakyvaults.utlitiy.InventoryUtility.inventoryPaperToBase64(inventory);
                vaults.set(vault  + ".items", itemEncoded);

                try {
                    configuration.save(playerDataFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        MagicSpells.log(NamedTextColor.BLUE + "Updated items in SneakyVaults.");
    }

    private static void scanWorlds() {
        Bukkit.getScheduler().runTaskAsynchronously(MagicSpells.getInstance(), () -> {
            CountDownLatch latch = new CountDownLatch(Bukkit.getWorlds().size());

            for (World world : Bukkit.getWorlds()) {
                Bukkit.getScheduler().runTaskAsynchronously(MagicSpells.getInstance(), () -> {
                    MagicSpells.error("Scanning world: " + world.getName());

                    File worldFolder = world.getWorldFolder();
                    File regionFolder;
                    switch (world.getEnvironment()) {
                        case NORMAL:
                            regionFolder = new File(worldFolder, "region");
                            break;
                        case NETHER:
                            regionFolder = new File(worldFolder, "DIM-1/region");
                            break;
                        case THE_END:
                            regionFolder = new File(worldFolder, "DIM1/region");
                            break;
                        default:
                            MagicSpells.error("Unknown world environment for world: " + world.getName());
                            latch.countDown();
                            return;
                    }

                    if (!regionFolder.exists() || !regionFolder.isDirectory()) {
                        MagicSpells.error("Region folder not found for world: " + world.getName());
                        latch.countDown();
                        return;
                    }

                    File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));

                    if (regionFiles == null || regionFiles.length == 0) {
                        MagicSpells.error("No region files found for world: " + world.getName());
                        latch.countDown();
                        return;
                    }

                    for (File regionFile : regionFiles) {
                        String fileName = regionFile.getName();
                        String[] parts = fileName.split("\\.");
                        if (parts.length != 4) {
                            MagicSpells.error("Invalid region file name: " + fileName);
                            continue;
                        }

                        try {
                            int regionX = Integer.parseInt(parts[1]);
                            int regionZ = Integer.parseInt(parts[2]);

                            MagicSpells.error("Scanning region: r." + regionX + "." + regionZ + ".mca in world " + world.getName());

                            for (int chunkX = regionX * 32; chunkX < (regionX + 1) * 32; chunkX++) {
                                for (int chunkZ = regionZ * 32; chunkZ < (regionZ + 1) * 32; chunkZ++) {
                                    if (world.isChunkGenerated(chunkX, chunkZ)) {
                                        final int finalChunkX = chunkX;
                                        final int finalChunkZ = chunkZ;

                                        world.getChunkAtAsync(finalChunkX, finalChunkZ).thenAccept(chunk -> {
                                            Bukkit.getScheduler().runTask(MagicSpells.getInstance(), () -> {
                                                if (chunk.isLoaded() && chunk.getTileEntities() != null) {
                                                    processChunk(chunk);
                                                } else {
                                                    MagicSpells.error("Chunk not fully loaded: " + finalChunkX + ", " + finalChunkZ);
                                                }
                                            });
                                        }).exceptionally(ex -> {
                                            MagicSpells.error("Error loading chunk: " + finalChunkX + ", " + finalChunkZ);
                                            return null;
                                        });
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            MagicSpells.error("Error parsing region coordinates from file name: " + fileName);
                        }
                    }
                    latch.countDown();
                });
            }

            Bukkit.getScheduler().runTaskAsynchronously(MagicSpells.getInstance(), () -> {
                try {
                    latch.await();
                    Bukkit.getScheduler().runTaskLater(MagicSpells.getInstance(),
                            () -> MagicSpells.log(NamedTextColor.BLUE + "All chunk data has been updated."),
                            1L
                    );
                    Bukkit.getScheduler().runTaskLater(MagicSpells.getInstance(),
                            () -> MagicSpells.log(NamedTextColor.GREEN + "All tasks complete. Please restart to proceed."),
                            1L
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    MagicSpells.error("Task interrupted while waiting for world scans to complete.");
                }
            });
        });
    }

    private static void processChunk(Chunk chunk) {
        synchronized (chunk) {
            for (BlockState blockState : chunk.getTileEntities()) {
                if (blockState instanceof Container container) {
                    Inventory inventory = container.getInventory();
                    ItemStack[] items = inventory.getContents();
                    addItemNames(items);

                    inventory.setContents(items);
                }
            }
        }
    }
}
