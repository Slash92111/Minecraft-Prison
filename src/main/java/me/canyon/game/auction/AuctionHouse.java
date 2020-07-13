package me.canyon.game.auction;

import me.canyon.Main;
import me.canyon.game.item.NBTTag;
import me.canyon.util.Utilities;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AuctionHouse {

    private Main plugin;

    private HashMap<Integer, Auction> items = new HashMap<>();

    public AuctionHouse(Main plugin) {
        this.plugin = plugin;

        getItemsListed();

        new BukkitRunnable() {
            public void run() {
                plugin.getAuctionListenerInstance().update();
            }
        }.runTaskTimer(this.plugin, 0, 20); // Refresh the inventory every second
    }

    public Inventory test(int page) {
        int totalInventorySize = 9;
        int amountOfPages = 1;

        // Calculate who many rows we'll need and then convert it to total slots
        if (this.items.size() > 9)
            totalInventorySize = (int) Math.ceil((float) this.items.size() / 9) * 9;

        // Calculate the total amount of pages we'll need for all items
        if (totalInventorySize > 45)
            amountOfPages = (int) Math.ceil((float) totalInventorySize / 5);

        if (page < amountOfPages)
            page = amountOfPages;

        List<Auction> auctions = new ArrayList<>(items.values());
        int auctionMod = (page - 1) * 45;

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Auction House Page " + page + "/" + amountOfPages);

        // Add the controls to the bottom of the inventory
        ItemStack NEXT_PAGE = NBTTag.setString(Utilities.createItem(new ItemStack(Material.ARROW), ChatColor.YELLOW + "Next Page ->", null), "id", "next");

        ItemStack PREV_PAGE = NBTTag.setString(Utilities.createItem(new ItemStack(Material.ARROW), ChatColor.YELLOW + "<- Previous Page", null), "id", "prev");

        ItemStack PLACE_HOLDER = Utilities.createItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), ChatColor.GRAY + "Empty", null);

        int yMod = (gui.getSize() - 9);

        // Set the entire bottom row as place holders then replace with the needed controls
        for (int j = 0; j < 9; j++)
            gui.setItem(yMod + j, PLACE_HOLDER);


        if (page == amountOfPages && page != 1)
            gui.setItem(yMod, PREV_PAGE);
        else if (page < amountOfPages && page > 1) {
            gui.setItem(yMod + 8, NEXT_PAGE);
            gui.setItem(yMod, PREV_PAGE);
        } else if (page < amountOfPages)
            gui.setItem(yMod + 8, NEXT_PAGE);

        // Start settings the items into the Auction House GUI
        while (gui.firstEmpty() >= 0 && auctionMod < auctions.size()) {
            if (auctions.get(auctionMod) != null) {
                Auction auction = auctions.get(auctionMod);

                // Get the item from the auction posting and add its lore
                ItemStack item = Utilities.addLore(Utilities.deserialize(auctions.get(auctionMod).getSterilizedItem()), new String[]{
                        " ",
                        ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + auction.getCost(),
                        ChatColor.GRAY + "Amount: " + ChatColor.GOLD + "1", //TODO change to support amount
                        ChatColor.GRAY + "Expires: " + ChatColor.GOLD + formatExpireDate(auction.getEnd()),
                        " ",
                        ChatColor.GRAY + "Seller: " + ChatColor.RED + auction.getOwnerIGN(),
                });

                item = NBTTag.setString(item, "id", "auction");
                item = NBTTag.setInteger(item, "auction_id", auction.getID());

                gui.setItem(gui.firstEmpty(), item);
            }

            auctionMod += 1;
        }

        return gui;
    }

    Inventory confirmBuy(int auctionID) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "" + ChatColor.BOLD +  "CONFIRM PURCHASE");

        Auction auction = this.items.get(auctionID);

        ItemStack CONFIRM = Utilities.createItem(new ItemStack(Material.GREEN_STAINED_GLASS_PANE),  ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM", new String[]{
                " ",
                ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + auction.getCost(),
                ChatColor.GRAY + "Amount: " + ChatColor.GOLD + "1", //TODO change to support amount
                ChatColor.GRAY + "Expires: " + ChatColor.GOLD + formatExpireDate(auction.getEnd()),
                " ",
                ChatColor.GRAY + "Seller: " + ChatColor.RED + auction.getOwnerIGN(),
        });

        CONFIRM = NBTTag.setString(CONFIRM, "id", "confirm");
        CONFIRM = NBTTag.setInteger(CONFIRM, "auction_id", auctionID);

        ItemStack CANCEL = Utilities.createItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), ChatColor.RED + "" + ChatColor.BOLD +  "CANCEL", new String[]{
                " ",
                ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + auction.getCost(),
                ChatColor.GRAY + "Amount: " + ChatColor.GOLD + "1", //TODO change to support amount
                ChatColor.GRAY + "Expires: " + ChatColor.GOLD + formatExpireDate(auction.getEnd()),
                " ",
                ChatColor.GRAY + "Seller: " + ChatColor.RED + auction.getOwnerIGN(),
        });

        CANCEL = NBTTag.setString(CANCEL, "id", "cancel");
        CANCEL = NBTTag.setInteger(CANCEL, "auction_id", auctionID);

        ItemStack item = auction.getItem();

        gui.setItem(0, CANCEL);
        gui.setItem(1, CANCEL);
        gui.setItem(2, CANCEL);

        gui.setItem(4, item);

        gui.setItem(6, CONFIRM);
        gui.setItem(7, CONFIRM);
        gui.setItem(8, CONFIRM);

        return gui;
    }

    private String formatExpireDate(Date expires) {
        String timeLeft;

        Date currentTime = new Date();

        long millis = Math.abs(currentTime.getTime() - expires.getTime());

        timeLeft = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

        return timeLeft;
    }

    private void getItemsListed() {
        for (Document document : plugin.getMongoDBInstance().getColletion("auction").find()) {
            int id = document.getInteger("ID");
            ItemStack item = Utilities.deserialize(document.getString("ITEM"));
            UUID ownerID = UUID.fromString(document.getString("OWNER_ID"));
            String ownerIGN = document.getString("OWNER_IGN");
            Date ends = document.getDate("ENDS");
            Long cost = document.getLong("COST");

            Auction auction = new Auction(id, item, ownerID, ownerIGN, ends, cost, false);

            this.items.put(id, auction);
        }
    }

    public Auction getAuction(int id) {
        return this.items.get(id);
    }

    public void create(UUID ownerID, String ownerIGN, ItemStack item) {
        int id = Utilities.getRandomNumber(1, 1000); //TODO replace
        items.put(id, new Auction(id, item, ownerID, ownerIGN, new Date(), 10, true));
    }

    public void remove(int id) {
        this.items.get(id).removeFromDatabase();
        this.items.remove(id);
    }
}
