package me.canyon.game.gang.vault;

import me.canyon.Main;
import me.canyon.game.gang.Gang;

import me.canyon.game.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ListenerVault implements Listener {

    private Main plugin;

    public ListenerVault(Main plugin) { this.plugin = plugin; }

    HashMap<Player, InventoryView> hasInventoryOpen = new HashMap<>();

    /**
     * Get an NBT Tag value from an item
     * @param item Item that you want to get the NBT Tag from
     * @param key The key of the Tag
     * @return Returns the value of the specified key
     */
    String getNBTTag(ItemStack item, String key) {
        // Verify that the item isn't null
        if (item != null) {
            // Convert the Bukkit ItemStack to a Craft ItemStack
            net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);

            // Verify that the item has tags
            if (nmsStack.hasTag())
                // Return the keys value
                return nmsStack.getTag().getString(key);
        }
        return "";
    }

    /**
     * Saves the latest version of a vault page from opened inventories
     * @param gang The gang that owns the vault we're looking for
     * @param page The specific page we want synced
     */
    void setLatestVersion(Gang gang, int page) {
        // Get all players with inventories opened currently
        Iterator<Map.Entry<Player, InventoryView>> iterator = this.hasInventoryOpen.entrySet().iterator();

        int ID = gang.getID();

        try {
            // Loop through each player
            while (iterator.hasNext()) {
                Map.Entry<Player, InventoryView> entry = iterator.next();

                InventoryView inventory = entry.getValue();
                String inventoryName = inventory.getTitle();

                // Make sure that the inventory is a vault
                if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
                    // Get the inventories gang id
                    int inventoryID = Integer.parseInt(getNBTTag(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "gang_id"));
                    // Make sure the gang does exist and it's the correct gang
                    if (inventoryID != 0)
                        if (inventoryID == ID) {
                            // Make sure that their on the same page
                            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));
                            if (currentPage == page) {
                                // Save the page
                                gang.getVault().savePage(currentPage, inventory.getTopInventory());
                                // End the loop since we only need the first version of the page
                                return;
                            }
                        }
                }
            }
        } catch(ConcurrentModificationException ex) {
            //TODO fix
        }
    }

    /**
     *  Sends all players that have the same inventory open the updated inventory
     * @param player The player that updated the inventory
     * @param inventory The players inventory view
     */
    private void syncInventory(Player player, InventoryView inventory) {
        // Get all players that have an inventory open
        Iterator<Map.Entry<Player, InventoryView>> iterator = this.hasInventoryOpen.entrySet().iterator();

        String inventoryName = inventory.getTitle();

        // Make sure that the player has a vault open
        if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
            // Get the gangs ID from the inventory
            int ID = Integer.parseInt(getNBTTag(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "gang_id"));

            // Make sure that the gang exists
            if (ID != 0) {
                int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", "")); //Strips a-z, A-Z, and spaces
                Gang gang = plugin.getPlayerData(player).getGang();

                // Convert the last save and the current version of the inventory to strings to compare
                String currentSteralized = gang.getVault().sterilize(inventory.getTopInventory());
                String lastSteralized = gang.getVault().sterilize(gang.getVault().getPage(currentPage));

                // Make sure that the inventory actually changed
                if (!lastSteralized.equals(currentSteralized))
                    try {
                        // Loop through all players with inventories open
                        while (iterator.hasNext()) {
                            Map.Entry<Player, InventoryView> entry = iterator.next();

                            Player otherPlayer = entry.getKey();

                            // Make sure that it's not the same player
                            if (otherPlayer != player) {
                                InventoryView otherInventory = entry.getValue();
                                String otherInventoryName = otherInventory.getTitle();

                                // Make sure they have a vault open
                                if (otherInventoryName.contains(ChatColor.GRAY + "Vault")) {
                                    // Get their inventories gang ID
                                    int otherID = Integer.parseInt(getNBTTag(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "gang_id"));

                                    // Make sure the gang exists
                                    if (otherID != 0) {
                                        int otherCurrentPage = Integer.parseInt(ChatColor.stripColor(otherInventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

                                        // Make sure it's the same gang and they're on the same page
                                        if (ID == otherID)
                                            if (currentPage == otherCurrentPage)
                                                if (gang.getID() == plugin.getPlayerData(otherPlayer).getGangID()) { // This isn't needed and needs to be tested w/ this If

                                                    // Wipe their cursor to reset at a later point so they don't drop it since we're reopening the inventory
                                                    ItemStack cursor = otherPlayer.getOpenInventory().getCursor();
                                                    otherPlayer.getOpenInventory().setCursor(new ItemStack(Material.AIR));

                                                    // Set their cursor back to what it was and open the latest version of the inventory
                                                    otherPlayer.openInventory(inventory.getTopInventory());
                                                    otherPlayer.getOpenInventory().setCursor(cursor);

                                                    // Save the page
                                                    gang.getVault().savePage(currentPage, inventory.getTopInventory());
                                                }
                                    }
                                }
                            }
                        }
                    } catch (ConcurrentModificationException ex) {
                        //TODO figure this out
                    }
            }
        }
    }

    /**
     * Handles a player opening a vault inventory
     * @param event InventoryOpenEvent
     */
    @EventHandler
    public void playerOpenInventory(InventoryOpenEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();

        // If a player opened a vault inventory add/replace them to the 'hasInventoryOpen' HashMap
        if (inventoryName.contains(ChatColor.GRAY + "Vault"))
            if (hasInventoryOpen.containsKey((Player) event.getPlayer()))
                hasInventoryOpen.replace((Player) event.getPlayer(), event.getView());
            else
                hasInventoryOpen.put((Player) event.getPlayer(), event.getView());
    }

    /**
     * Handles a player upgrading, changing pages, and updating a vault
     * @param event InventoryClickEvent
     */
    @EventHandler
    public void playerClickItem(InventoryClickEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player);

        boolean admin = false;

        // Make sure they're in a vault inventory
        if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
            ItemStack information = inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5);
            Gang gang = null;

            // Get the gang object class
            if (playerData.getGangID() != 0)
                gang = plugin.getPlayerData(player).getGang();

            if (information != null) {

                int ID = Integer.parseInt(getNBTTag(information, "gang_id"));

                if (!plugin.hasGangData(ID))
                    plugin.setGangData(ID , new Gang(ID));

                gang = plugin.getGangData(ID);
                admin = Boolean.parseBoolean(getNBTTag(information, "admin"));
            }

            // Make sure the gang actually exists
            if (gang != null) {
                Vault vault = gang.getVault();
                ItemStack clickedItem = event.getCurrentItem();

                int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s",""));

                // Sync the inventory to everyone that has it open currently
                syncInventory(player, inventory);

                // Make sure the player didn't click on nothing and it was on an arrow/chest/stained glass.
                if (clickedItem != null && (clickedItem.getType().equals(Material.ARROW) || clickedItem.getType().equals(Material.CHEST) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)))
                    // Make sure that they're interacting with the top inventory
                    if (event.getRawSlot() >= 0 && event.getRawSlot() <= (inventory.getTopInventory().getSize() - 1)) {

                        // Get the gang ID from the inventory
                        String id = getNBTTag(clickedItem, "id");

                        switch (id) {
                            case "next":
                                vault.open(currentPage + 1, player, admin);
                                break;
                            case "prev":
                                vault.open(currentPage - 1, player, admin);
                                break;
                            case "upgrade":
                                // Prevent an admin from 'upgrading' the vault
                                if (admin)
                                    return;

                                // Making sure that the player is either a officer or the leader of the gang to upgrade
                                if (gang.getOfficers().contains(player.getUniqueId().toString()) || gang.getLeader().equals(player.getUniqueId())) {
                                    // Making sure they have enough money in the gangs balance to upgrade
                                    if (gang.getBalance() >= vault.getUpgradeCost()) {
                                        if (!vault.isMaxed()) {
                                            gang.setBalance(gang.getBalance() - vault.getUpgradeCost());
                                            vault.upgrade(inventory);
                                            vault.open(currentPage, player, false);
                                            syncInventory(player, player.getOpenInventory());
                                        }
                                    } else
                                        player.sendMessage(plugin.getMessageFromConfig("gang.notEnoughMoney"));
                                } else
                                    player.sendMessage(plugin.getMessageFromConfig("gang.needToBeOfficerToUpgrade"));
                                break;
                            default:
                                gang.getVault().savePage(currentPage, inventory.getTopInventory());
                                break;
                        }

                        event.setCancelled(true);
                    }
            }
        }
    }

    @EventHandler
    public void playerCloseVault(InventoryCloseEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();
        Player player = (Player) event.getPlayer();

        hasInventoryOpen.remove(player);

        // Making sure the player is in a Gang 'Vault'
        if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
            // Getting the gang id from the 'upgrade' button
            int ID = Integer.parseInt(getNBTTag(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "gang_id"));

            // Loading the gang locally if it isn't already
            if (!plugin.hasGangData(ID))
                plugin.setGangData(ID, new Gang(ID));

            Gang gang = plugin.getGangData(ID);

            // Remove all letters and spaces, split at '/', and grab the first int args (Inventory name Ex: 'Vault Page 1/2'
            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

            // Save the current instance of the inventory to the Vault object class
            gang.getVault().savePage(currentPage, inventory.getTopInventory());
        }
    }
}
