package me.canyon.game.player.backpack.listener;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.backpack.Backpack;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ListenerBackpack implements Listener {

    private Main plugin;

    public ListenerBackpack(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerClickItem(InventoryClickEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();
        Player player = (Player) event.getWhoClicked();

        boolean admin = false;

        if (inventoryName.contains(ChatColor.GRAY + "Backpack")) {
            PlayerData owner = plugin.getPlayerData(player);

            ItemStack backpackInformation = inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5);

            if (backpackInformation != null)
                if (backpackInformation.getType().equals(Material.CHEST)) {
                    if (backpackInformation.hasItemMeta())
                        if (backpackInformation.getItemMeta().hasDisplayName())
                            if (backpackInformation.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Owner")) {
                                MongoDB mongoDB = plugin.getMongoDBInstance();
                                Document document = mongoDB.getDocument("UUID", ChatColor.stripColor(backpackInformation.getItemMeta().getDisplayName().split(":")[1].replace(" ", "")));

                                if (document != null) {
                                    UUID uuid = UUID.fromString(document.getString("UUID"));

                                    Player target = Bukkit.getPlayer(uuid);

                                    if (target != null) {
                                        if (!plugin.hasPlayerData(target))
                                            plugin.setPlayerData(target, true);
                                    } else {
                                        uuid = UUID.fromString(document.getString("UUID"));
                                        if (!plugin.hasPlayerData(uuid))
                                            plugin.setPlayerData(uuid, true);
                                    }

                                    owner = plugin.getPlayerData(uuid);
                                    admin = true;

                                }
                            }
                }

            Backpack backpack = owner.getBackpack();
            ItemStack clickedItem = event.getCurrentItem();

            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s",""));

            if (clickedItem != null && (clickedItem.getType().equals(Material.ARROW) || clickedItem.getType().equals(Material.CHEST) || clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)))
                if (inventoryName.contains(ChatColor.GRAY + "Backpack Page")) {
                    if (clickedItem.hasItemMeta())
                        if (clickedItem.getItemMeta().hasDisplayName())
                            if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Next Page ->")) { //save page first
                                backpack.open(currentPage + 1, player, admin);
                                event.setCancelled(true);
                            } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "<- Previous Page")) {
                                backpack.open(currentPage - 1, player, admin);
                                event.setCancelled(true);
                            } else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GRAY + "Empty"))
                                event.setCancelled(true);
                            else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.DARK_PURPLE + "Upgrade Backpack Size")) {
                                if (owner.getBalance() >= backpack.getUpgradeCost()) {
                                    if (!backpack.isMaxed()) {
                                        owner.setBalance(owner.getBalance() - backpack.getUpgradeCost());
                                        backpack.upgrade(inventory);
                                        backpack.open(currentPage, player, admin);
                                    }
                                } else
                                    owner.getPlayer().sendMessage(plugin.getMessageFromConfig("player.notEnoughMoneyBackpack"));

                                event.setCancelled(true);
                            } else if (clickedItem.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Owner:"))
                                event.setCancelled(true);
                }
        }
    }


    @EventHandler
    public void playerCloseBackpack(InventoryCloseEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();
        Player player = (Player) event.getPlayer();

        if (inventoryName.contains(ChatColor.GRAY + "Backpack")) {
            PlayerData owner = plugin.getPlayerData(player);

            ItemStack backpackInformation = inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5);

            if (backpackInformation != null)
                if (backpackInformation.getType().equals(Material.CHEST)) {
                    if (backpackInformation.hasItemMeta())
                        if (backpackInformation.getItemMeta().hasDisplayName())
                            if (backpackInformation.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Owner")) {
                                MongoDB mongoDB = plugin.getMongoDBInstance();
                                Document document = mongoDB.getDocument("UUID", ChatColor.stripColor(backpackInformation.getItemMeta().getDisplayName().split(":")[1].replace(" ", "")));

                                if (document != null) {
                                    UUID uuid = UUID.fromString(document.getString("UUID"));

                                    Player target = Bukkit.getPlayer(uuid);

                                    if (target != null) {
                                        if (!plugin.hasPlayerData(target))
                                            plugin.setPlayerData(target, true);
                                    } else {
                                        uuid = UUID.fromString(document.getString("UUID"));
                                        if (!plugin.hasPlayerData(uuid))
                                            plugin.setPlayerData(uuid, true);
                                    }

                                    owner = plugin.getPlayerData(uuid);
                                }
                            }
                }

            int currentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", "")); //Strips a-z, A-Z, and spaces

            Inventory temp = Bukkit.createInventory(null, inventory.getTopInventory().getSize() - 9);

            for (int i = 0; i < temp.getSize(); i++)
                if (inventory.getTopInventory().getItem(i) != null)
                    temp.setItem(i, inventory.getTopInventory().getItem(i));
                else
                    temp.setItem(i, new ItemStack(Material.AIR));

            owner.getBackpack().update(currentPage, temp);
        }
    }
}
