package me.canyon.game.player.backpack;

import me.canyon.Main;
import me.canyon.game.gang.Gang;
import me.canyon.game.item.NBTTag;
import me.canyon.game.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ListenerBackpack implements Listener {

    private Main plugin;

    public ListenerBackpack(Main plugin) {
        this.plugin = plugin;
    }

    HashMap<Player, InventoryView> hasBackpackOpen = new HashMap<>();

    void setLatestVersion(UUID ownerUUID, int page) {
        // Get all players with inventories opened currently
        Iterator<Map.Entry<Player, InventoryView>> iterator = this.hasBackpackOpen.entrySet().iterator();

        try {
            // Loop through each player
            while (iterator.hasNext()) {
                Map.Entry<Player, InventoryView> entry = iterator.next();

                InventoryView inventory = entry.getValue();
                String inventoryName = inventory.getTitle();

                // Make sure that the inventory is a vault
                if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
                    // Get the inventories gang id
                    UUID inventoryUUID = UUID.fromString(NBTTag.getString(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "player_id"));

                    if (ownerUUID.equals(inventoryUUID)) {
                        // Make sure that their on the same page
                        int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));
                        if (currentPage == page) {

                            if (!plugin.hasPlayerData(ownerUUID))
                                plugin.setPlayerData(ownerUUID);

                            // Save the page
                            plugin.getPlayerData(ownerUUID).getBackpack().savePage(currentPage, inventory.getTopInventory());
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

    private void syncInventory(Player player, InventoryView inventory) {
        // Get all players that have an inventory open
        Iterator<Map.Entry<Player, InventoryView>> iterator = this.hasBackpackOpen.entrySet().iterator();

        String inventoryName = inventory.getTitle();
        int inventorySize = inventory.getTopInventory().getSize();

        // Make sure that the player has a vault open
        if (inventoryName.contains(ChatColor.GRAY + "Backpack")) {
            // Get the gangs ID from the inventory
            UUID ownerUUID = UUID.fromString(NBTTag.getString(inventory.getTopInventory().getItem(inventorySize - 5), "player_id"));

            if (!plugin.hasPlayerData(ownerUUID))
                plugin.setPlayerData(ownerUUID);

            PlayerData ownerData = plugin.getPlayerData(ownerUUID);

            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", "")); //Strips a-z, A-Z, and spaces

            // Convert the last save and the current version of the inventory to strings to compare
            String currentSteralized = ownerData.getBackpack().sterilize(inventory.getTopInventory());
            String lastSteralized = ownerData.getBackpack().sterilize(ownerData.getBackpack().getPage(currentPage));

            // Make sure that the inventory actually changed
            if (!lastSteralized.equals(currentSteralized))
                try {
                    // Loop through all players with inventories open
                    while (iterator.hasNext()) {
                        Map.Entry<Player, InventoryView> entry = iterator.next();

                        Player otherPlayer = entry.getKey();

                        if (otherPlayer != player) {
                            InventoryView otherInventory = entry.getValue();
                            String otherInventoryName = otherInventory.getTitle();

                            if (otherInventoryName.contains(ChatColor.GRAY + "Backpack")) {

                                UUID otherUUID = UUID.fromString(NBTTag.getString(inventory.getTopInventory().getItem(inventorySize - 5), "player_id"));

                                int otherCurrentPage = Integer.parseInt(ChatColor.stripColor(otherInventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

                                    if (ownerUUID.equals(otherUUID))
                                        if (currentPage == otherCurrentPage) {
                                            // Wipe their cursor to reset at a later point so they don't drop it since we're reopening the inventory
                                            ItemStack cursor = otherPlayer.getOpenInventory().getCursor();
                                            otherPlayer.getOpenInventory().setCursor(new ItemStack(Material.AIR));

                                            otherPlayer.openInventory(inventory.getTopInventory());

                                            // Set their cursor back to what it was and open the latest version of the inventory
                                            otherPlayer.getOpenInventory().setCursor(cursor);

                                            // Save the page over the 'close event' save
                                            ownerData.getBackpack().savePage(currentPage, inventory.getTopInventory());
                                        }
                                }
                            }
                        }
                } catch (ConcurrentModificationException ex) {
                        //TODO figure this out
                }
        }
    }


    @EventHandler
    public void playerClickItem(InventoryClickEvent event) {
        InventoryView inventoryView = event.getView();
        Inventory inventory = inventoryView.getTopInventory();
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player);
        int inventorySize = inventoryView.getTopInventory().getSize();

        if (inventoryView.getTitle().contains(ChatColor.GRAY + "Backpack")) {
            ItemStack information = inventory.getItem(inventorySize - 5);

            if (information != null) {
                UUID ownerUUID = UUID.fromString(NBTTag.getString(information, "player_id"));

                ItemStack clickedItem = event.getCurrentItem();

                if (!plugin.hasPlayerData(ownerUUID))
                    plugin.setPlayerData(ownerUUID);

                PlayerData ownerData = plugin.getPlayerData(ownerUUID);
                Backpack backpack = ownerData.getBackpack();

                int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryView.getTitle()).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s",""));

                backpack.savePage(currentPage, inventory);

                syncInventory(player, inventoryView);

                if (clickedItem != null && (clickedItem.getType().equals(Material.ARROW) || clickedItem.getType().equals(Material.CHEST) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)))
                    // Make sure that they're interacting with the top inventory
                    if (event.getRawSlot() >= 0 && event.getRawSlot() <= (inventorySize - 1)) {
                        String id = NBTTag.getString(clickedItem, "id");

                        switch(id) {
                            case "next":
                                backpack.open(currentPage + 1, player);
                                break;
                            case "prev":
                                backpack.open(currentPage - 1, player);
                                break;
                            case "upgrade":
                                if (!playerData.getUUID().equals(ownerUUID)) {
                                    player.sendMessage(plugin.getMessageFromConfig("player.notYourBackpack"));
                                    break;
                                }

                                if (ownerData.getBalance() >= backpack.getUpgradeCost()) {
                                    if (!backpack.isMaxed()) {
                                        ownerData.setBalance(ownerData.getBalance() - backpack.getUpgradeCost());
                                        backpack.upgrade(inventoryView);
                                        backpack.open(currentPage, player);
                                        syncInventory(player, inventoryView);
                                    }

                                    syncInventory(player, inventoryView);
                                } else
                                    ownerData.getPlayer().sendMessage(plugin.getMessageFromConfig("player.notEnoughMoneyBackpack"));

                                break;
                            default:
                                backpack.savePage(currentPage, inventory);
                                break;
                        }

                        event.setCancelled(true);
                    }

            }
        }
    }

    @EventHandler
    public void playerOpenBackpack(InventoryOpenEvent event) {
        if (event.getView().getTitle().contains(ChatColor.GRAY + "Backpack"))
            if (hasBackpackOpen.containsKey((Player) event.getPlayer()))
                hasBackpackOpen.replace((Player) event.getPlayer(), event.getView());
            else
                hasBackpackOpen.put((Player) event.getPlayer(), event.getView());
    }

    @EventHandler
    public void playerCloseBackpack(InventoryCloseEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();
        Player player = (Player) event.getPlayer();

        hasBackpackOpen.remove(player);

        if (inventoryName.contains(ChatColor.GRAY + "Backpack")) {
            UUID ownerUUID = UUID.fromString(NBTTag.getString(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "player_id"));

            if (!plugin.hasPlayerData(ownerUUID))
                plugin.setPlayerData(ownerUUID);

            PlayerData ownerData = plugin.getPlayerData(ownerUUID);

            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

            ownerData.getBackpack().savePage(currentPage, inventory.getTopInventory());
        }
    }
}
