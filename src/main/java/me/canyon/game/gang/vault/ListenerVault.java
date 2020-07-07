package me.canyon.game.gang.vault;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.gang.Gang;

import org.bson.Document;
import org.bukkit.Bukkit;
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

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ListenerVault implements Listener {

    private Main plugin;

    public ListenerVault(Main plugin) { this.plugin = plugin; }

    public HashMap<Player, InventoryView> hasInventoryOpen = new HashMap<>();

    public int getGangIDFromItem(ItemStack item) {
        if (item != null)
            if (item.getType().equals(Material.CHEST))
                if (item.hasItemMeta())
                    if (item.getItemMeta().hasLore() && item.getItemMeta().getLore() != null)
                        for (String string : item.getItemMeta().getLore())
                            if (string.contains(ChatColor.DARK_GRAY + "ID: "))
                                return Integer.parseInt(ChatColor.stripColor(string.split(" ")[1]));
        return 0;
    }

    public void syncVaultPage(Gang gang, int page) {
        Iterator<Map.Entry<Player, InventoryView>> iterator = this.hasInventoryOpen.entrySet().iterator();

        int ID = gang.getID();

        try {
            while (iterator.hasNext()) {
                Map.Entry<Player, InventoryView> entry = iterator.next();

                InventoryView inventory = entry.getValue();
                String inventoryName = inventory.getTitle();

                if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
                    int inventoryID = getGangIDFromItem(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5));
                    if (inventoryID != 0)
                        if (inventoryID == ID) {
                            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));
                            if (currentPage == page) {
                                gang.getVault().savePage(currentPage, inventory.getTopInventory());
                                return; //Just grab the first one and save it
                            }
                        }
                }
            }
        } catch(ConcurrentModificationException ex) {
            //TODO fix
        }
    }

    private void syncInventory(Player player, InventoryView inventory) {
        Iterator<Map.Entry<Player, InventoryView>> iterator = this.hasInventoryOpen.entrySet().iterator();

        String inventoryName = inventory.getTitle();

        if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
            int ID = getGangIDFromItem(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5)); //Returns 0 if it doesn't have the ID

            if (ID != 0) {
                int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", "")); //Strips a-z, A-Z, and spaces
                Gang gang = plugin.getPlayerData(player).getGang();

                String currentSteralized = gang.getVault().sterilize(inventory.getTopInventory());
                String lastSteralized = gang.getVault().sterilize(gang.getVault().getPage(currentPage));

                if (!lastSteralized.equals(currentSteralized))
                    try {
                        while (iterator.hasNext()) {
                            Map.Entry<Player, InventoryView> entry = iterator.next();

                            Player otherPlayer = entry.getKey();

                            if (otherPlayer != player) { //Make sure it's not the same player
                                InventoryView otherInventory = entry.getValue();
                                String otherInventoryName = otherInventory.getTitle();

                                if (otherInventoryName.contains(ChatColor.GRAY + "Vault")) {
                                    int otherID = getGangIDFromItem(otherInventory.getTopInventory().getItem(otherInventory.getTopInventory().getSize() - 5));

                                    if (otherID != 0) {
                                        int otherCurrentPage = Integer.parseInt(ChatColor.stripColor(otherInventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

                                        if (ID == otherID)
                                            if (currentPage == otherCurrentPage)
                                                if (gang.getID() == plugin.getPlayerData(otherPlayer).getGangID()) {

                                                    ItemStack cursor = otherPlayer.getOpenInventory().getCursor();
                                                    otherPlayer.getOpenInventory().setCursor(new ItemStack(Material.AIR));

                                                    otherPlayer.openInventory(inventory.getTopInventory());
                                                    otherPlayer.getOpenInventory().setCursor(cursor);

                                                    gang.getVault().savePage(currentPage, inventory.getTopInventory()); //save the page over the other saves
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

    @EventHandler
    public void playerOpenInventory(InventoryOpenEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();

        if (inventoryName.contains(ChatColor.GRAY + "Vault"))
            if (hasInventoryOpen.containsKey((Player) event.getPlayer()))
                hasInventoryOpen.replace((Player) event.getPlayer(), event.getView());
            else
                hasInventoryOpen.put((Player) event.getPlayer(), event.getView());
    }

    @EventHandler
    public void playerClickItem(InventoryClickEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();
        Player player = (Player) event.getWhoClicked();

        boolean admin = false;

        if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
            ItemStack information = inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5);

            Gang gang = plugin.getPlayerData(player).getGang();

            if (information != null)
                if (information.getType().equals(Material.CHEST))
                    if (information.hasItemMeta())
                        if (information.getItemMeta().hasDisplayName())
                            if (information.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Gang")) {
                                MongoDB mongoDB = plugin.getMongoDBInstance();
                                Document document = mongoDB.getDocument("gang", "NAME", ChatColor.stripColor(information.getItemMeta().getDisplayName().split(":")[1].replace(" ", "")));

                                if (document != null) {
                                    int ID = document.getInteger("ID");

                                    if (!plugin.hasGangData(ID))
                                        plugin.setGangData(ID, new Gang(ID));

                                    gang = plugin.getGangData(ID);
                                    admin = true;

                                }
                            }


            Vault vault = gang.getVault();
            ItemStack clickedItem = event.getCurrentItem();

            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s",""));

            syncInventory(player, inventory);

            if (clickedItem != null && (clickedItem.getType().equals(Material.ARROW) || clickedItem.getType().equals(Material.CHEST) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)))
                if (inventoryName.contains(ChatColor.GRAY + "Vault Page")) {

                    if (clickedItem.hasItemMeta())
                        if (clickedItem.getItemMeta().hasDisplayName())
                            if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Next Page ->")) { //save page first
                                vault.open(currentPage + 1, player, admin);
                                event.setCancelled(true);
                            } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "<- Previous Page")) {
                                vault.open(currentPage - 1, player, admin);
                                event.setCancelled(true);
                            } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GRAY + "Empty"))
                                event.setCancelled(true);
                            else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.DARK_PURPLE + "Upgrade Vault Size")) {
                                if (gang.getOfficers().contains(player.getUniqueId().toString()) || gang.getLeader().equals(player.getUniqueId())) {
                                    if (gang.getBalance() >= vault.getUpgradeCost()) {
                                        if (!vault.isMaxed()) {
                                            gang.setBalance(gang.getBalance() - vault.getUpgradeCost());
                                            vault.upgrade(inventory);
                                            vault.open(currentPage, player, admin);
                                            syncInventory(player, player.getOpenInventory());
                                        }
                                    } else
                                        player.sendMessage(plugin.getMessageFromConfig("gang.notEnoughMoney"));
                                } else
                                    player.sendMessage(plugin.getMessageFromConfig("gang.needToBeOfficerToUpgrade"));

                                event.setCancelled(true);
                            } else if (clickedItem.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Gang:"))
                                event.setCancelled(true);
                            else {
                                gang.getVault().savePage(currentPage, inventory.getTopInventory());
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
            Gang gang = plugin.getPlayerData(player).getGang();

            //TODO change to NBT tag
            ItemStack information = inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5);

            if (information != null)
                if (information.getType().equals(Material.CHEST)) {
                    if (information.hasItemMeta())
                        if (information.getItemMeta().hasDisplayName())
                            if (information.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Gang")) {
                                MongoDB mongoDB = plugin.getMongoDBInstance();
                                Document document = mongoDB.getDocument("gang", "NAME", ChatColor.stripColor(information.getItemMeta().getDisplayName().split(":")[1].replace(" ", "")));

                                if (document != null) {
                                    int ID = document.getInteger("ID");

                                    if (!plugin.hasGangData(ID))
                                        plugin.setGangData(ID, new Gang(ID));

                                    gang = plugin.getGangData(ID);
                                }
                            }
                }

            // Remove all letters and spaces, split at '/', and grab the first int args (Inventory name Ex: 'Vault Page 1/2'
            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

            // Save the current instance of the inventory to the Vault object class
            gang.getVault().savePage(currentPage, inventory.getTopInventory());
        }
    }
}
