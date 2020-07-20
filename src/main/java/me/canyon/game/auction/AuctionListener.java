package me.canyon.game.auction;

import me.canyon.Main;
import me.canyon.game.item.NBTTag;
import me.canyon.game.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AuctionListener implements Listener {

    private Main plugin;

    public AuctionListener(Main plugin) {
        this.plugin = plugin;
    }

    private HashMap<Player, InventoryView> hasInventoryOpen = new HashMap<>();

    void update() {
        for (Map.Entry<Player, InventoryView> entry : this.hasInventoryOpen.entrySet()) {
            Player player = entry.getKey();

            InventoryView inventory = entry.getValue();
            String inventoryName = inventory.getTitle();

            // Make sure they have a vault open
            if (inventoryName.contains(ChatColor.DARK_GRAY + "Auction House")) {
                int page = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

                player.getOpenInventory().getTopInventory().setContents(plugin.getAuctionHouseInstance().test(page).getContents());
            } else if (inventoryName.contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "CONFIRM PURCHASE")) { //TODO check if item still exists
                int auctionID = NBTTag.getInteger(inventory.getTopInventory().getItem(0), "auction_id");
                player.getOpenInventory().getTopInventory().setContents(plugin.getAuctionHouseInstance().confirmBuy(auctionID).getContents());
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

        if (inventoryView.getTitle().contains(ChatColor.DARK_GRAY + "Auction House") || inventoryView.getTitle().contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "CONFIRM PURCHASE")) {
            if (event.getRawSlot() >= 0 && event.getRawSlot() <= (inventorySize - 1)) {
                ItemStack clickedItem = event.getCurrentItem();

                int currentPage = 0;

                if (inventoryView.getTitle().contains(ChatColor.DARK_GRAY + "Auction House"))
                    currentPage = Integer.parseInt(ChatColor.stripColor(inventoryView.getTitle()).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));

                String id = NBTTag.getString(clickedItem, "id");

                switch (id) {
                    case "listings":
                        break;
                    case "next":
                        plugin.getAuctionHouseInstance().test(currentPage + 1);
                        break;
                    case "prev":
                        plugin.getAuctionHouseInstance().test(currentPage - 1);
                        break;
                    case "auction": {
                        int auctionID = NBTTag.getInteger(clickedItem, "auction_id");

                        Auction auction = plugin.getAuctionHouseInstance().getAuction(auctionID);

                        if (auction.getOwnerID().equals(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "You can't buy your own auction! You need to cancel it instead"); //TODO replace w/ config message
                            return;
                        }


                        if (plugin.getPlayerData(player).getBalance() > auction.getCost())
                            player.openInventory(plugin.getAuctionHouseInstance().confirmBuy(auctionID));
                        else
                            player.sendMessage(ChatColor.RED + "Not enough money"); //TODO replace w/ config message
                        break;
                    }
                    case "confirm": {
                        int auctionID = NBTTag.getInteger(clickedItem, "auction_id");

                        Auction auction = plugin.getAuctionHouseInstance().getAuction(auctionID);

                        player.getInventory().addItem(auction.getItem());

                        plugin.getAuctionHouseInstance().remove(auctionID);

                        player.openInventory(plugin.getAuctionHouseInstance().test(1));
                        break;
                    }
                    case "cancel": {
                        player.openInventory(plugin.getAuctionHouseInstance().test(1)); //TODO previous page?
                        break;
                    }
                }

                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerOpenInventory(InventoryOpenEvent event) {
        InventoryView inventory = event.getView();
        String inventoryName = inventory.getTitle();

        // If a player opened a vault inventory add/replace them to the 'hasInventoryOpen' HashMap
        if (inventoryName.contains(ChatColor.DARK_GRAY + "Auction House") || inventoryName.contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "CONFIRM PURCHASE")) {
            if (hasInventoryOpen.containsKey((Player) event.getPlayer()))
                hasInventoryOpen.replace((Player) event.getPlayer(), event.getView());
            else
                hasInventoryOpen.put((Player) event.getPlayer(), event.getView());
        }
    }

    @EventHandler
    public void playerCloseInventory(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        hasInventoryOpen.remove(player);
    }
}
