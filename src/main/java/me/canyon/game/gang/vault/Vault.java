package me.canyon.game.gang.vault;

import me.canyon.Main;
import me.canyon.game.gang.Gang;
import me.canyon.game.item.NBTTag;
import me.canyon.util.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class Vault {
    private HashMap<Integer, Inventory> pages = new HashMap<>();

    private Gang gang;
    private int id;
    private int numberOfPages, totalRows, upgradeCost;

    public Vault(Gang gang) {
        this.gang = gang;
        this.id = gang.getID();

        this.totalRows = gang.getTotalVaultRows();
        this.upgradeCost = Main.getInstance().getDefaultsConfig().getInt("gang.upgradeCost");

        load();
    }

    /*
    Loads the gangs vault from the database
     */
    private void load() {
        // Grab the list of steralized inventories
        List<String> list = gang.getVaultList();

        // Verify that the list isn't null (Null on gang creation)
        if (list != null)
            for (int i = 0; i < list.size(); i++) {
                // Add the page to the 'pages' HashMap for storage
                int page = i + 1;
                pages.put(page, deserialize(list.get(i), page));
            }
        else {
            // Create the first page and add it to the 'pages' HashMap for storage
            Inventory inventory = Bukkit.getServer().createInventory(null, 9, "");
            pages.put(1, deserialize(sterilize(inventory), 1));
        }

        this.numberOfPages = pages.size();
    }

    /*
    Converts an Inventory Object to a string for storage in the MongoDB
     */
    String sterilize(Inventory inventory) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(inventory.getSize());

            for (int i = 0; i < inventory.getSize(); i++)
                dataOutput.writeObject(inventory.getItem(i));

            dataOutput.close();

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks!", e);
        }
    }

    /*
    Converts a string to an Inventory Object
     */
    private Inventory deserialize(String string, int pageNumber) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(string));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            // Create the inventory
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt(), ChatColor.GRAY + "Backpack Page " + pageNumber + "/" + this.numberOfPages);

            // Set contents
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();

            return inventory;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Gang getGang() { return this.gang; }

    private int getNumberOfPages() { return this.numberOfPages; }

    public int getTotalRows() { return this.totalRows; }

    Inventory getPage(int page) { return pages.get(page); }

    public void update(int page, Inventory inventory) { this.pages.replace(page, inventory); }

    void savePage(int page, Inventory inventory) {
        Inventory temp = Bukkit.createInventory(null, inventory.getSize() - 9);

        for (int i = 0; i < temp.getSize(); i++)
            if (inventory.getItem(i) != null)
                temp.setItem(i, inventory.getItem(i));
            else
                temp.setItem(i, new ItemStack(Material.AIR));

        update(page, temp);
    }

    boolean isMaxed() { return (gang.getMaxVaultPages() * 5) == this.totalRows; }

    String getNextSize() {
        int upgraded = this.totalRows + 1;
        int maxRowsPerPage = 5;
        int maxPages = gang.getMaxVaultPages();

        if (upgraded <= maxRowsPerPage * maxPages)
            return upgraded + "";
        else
            return "Maxed Size";
    }

    int getUpgradeCost() {
        if (!getNextSize().equalsIgnoreCase("maxed size"))
            return (this.totalRows + 1) * this.upgradeCost;

        return 0;
    }

    public void open (int page, Player player) {
        player.openInventory(setUI(page));
    }

    private Inventory setUI(int page) {
        return setUI(getPage(page), page);
    }

    Inventory setUI(Inventory inventory, int page) {
        Inventory ui = Bukkit.createInventory(null, inventory.getSize() + 9, ChatColor.GRAY + "Vault Page " + page + "/" + this.numberOfPages);

        ItemStack PLACE_HOLDER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);

        ItemStack UPGRADE = Utilities.createItem(new ItemStack(Material.CHEST), ChatColor.DARK_PURPLE + "Upgrade Vault Size", new String[]{
                    ChatColor.GRAY + "Click to upgrade vault size",
                    "",
                    ChatColor.YELLOW + "Current Size:",
                    ChatColor.GRAY + "Rows: " + ChatColor.YELLOW + this.totalRows,
                    "",
                    ChatColor.GREEN + "Next Size:",
                    ChatColor.GRAY + "Rows: " + ChatColor.GREEN + this.getNextSize(),
                    "",
                    ChatColor.GRAY + "Cost: " + ChatColor.GOLD + new Utilities().formatEconomy(Long.parseLong(getUpgradeCost() + "")),
            });

        // Set the NBT tag for gang ID onto the 'upgrade' chest
        UPGRADE = NBTTag.setString(UPGRADE, "id", "upgrade");
        UPGRADE = NBTTag.setInteger(UPGRADE, "gang_id", this.gang.getID());

        ItemStack NEXT_PAGE = NBTTag.setString(Utilities.createItem(new ItemStack(Material.ARROW), ChatColor.YELLOW + "Next Page ->", null), "id", "next");

        ItemStack PREV_PAGE = NBTTag.setString(Utilities.createItem(new ItemStack(Material.ARROW), ChatColor.YELLOW + "<- Previous Page", null), "id", "prev");

        // Set the vault controls on the bottom row of the inventory
        ItemMeta meta = PLACE_HOLDER.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Empty");
        PLACE_HOLDER.setItemMeta(meta);

        int yMod = (ui.getSize() - 9);

        // Set the entire bottom row as place holders then replace with the needed controls
        for (int i = 0; i < 9; i++)
            ui.setItem(yMod + i, PLACE_HOLDER);

        ui.setItem(yMod + 4, UPGRADE);

        if (page == getNumberOfPages() && page != 1)
            ui.setItem(yMod, PREV_PAGE);
        else if (page < getNumberOfPages() && page > 1) {
            ui.setItem(yMod + 8, NEXT_PAGE);
            ui.setItem(yMod, PREV_PAGE);
        } else if (page < getNumberOfPages())
            ui.setItem(yMod + 8, NEXT_PAGE);

        // Set the contents of the inventory

        for (int i = 0; i < inventory.getSize(); i++)
            if (inventory.getItem(i) != null)
                ui.setItem(i, inventory.getItem(i));
            else
                ui.setItem(i, new ItemStack(Material.AIR));

        return ui;
    }

    void upgrade(InventoryView inventory) {
        int currentPage = Integer.parseInt(ChatColor.stripColor(inventory.getTitle()).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", "")); //Strips a-z, A-Z, and spaces

        // Verify that the vault is not maxed out on pages/rows already
        if ((gang.getMaxVaultPages() * 5) > this.totalRows) {
            // Creates a new page since they're last page is maxed on rows
            if ((this.numberOfPages * 5) == this.totalRows) {
                // If they're on their last page save it
                if (currentPage == this.numberOfPages) {
                    Inventory temp = Bukkit.createInventory(null, inventory.getTopInventory().getSize() - 9);

                    for (int i = 0; i < temp.getSize(); i++)
                        if (inventory.getTopInventory().getItem(i) != null)
                            temp.setItem(i, inventory.getTopInventory().getItem(i));
                        else
                            temp.setItem(i, new ItemStack(Material.AIR));

                    if ((gang.getMaxVaultPages() * 5) > this.totalRows)
                        update(currentPage, temp);
                }

                pages.put(this.numberOfPages + 1, Bukkit.createInventory(null, 9, "Backpack Page " + this.numberOfPages + 1));

                this.numberOfPages += 1;
                this.totalRows += 1;

                ListenerVault listenerVault = Main.getInstance().getListenerVaultInstance();
                // Grab the latest player sync (Which would be the latest save of the inventory)
                listenerVault.setLatestVersion(this.gang, this.numberOfPages);
                // Send all players that have
                sendPageUpdate(this.numberOfPages - 1); //Send them the inventory update
                listenerVault.setLatestVersion(this.gang, this.numberOfPages); //Sync/save the updated inventory

            } else { //Upgrade page
                if (currentPage == this.numberOfPages) { //If they're on their last page and trying to upgrade it
                    Inventory tempInventory = Bukkit.createInventory(null, inventory.getTopInventory().getSize()); //Already has the added 9 because of control bar

                    for (int i = 0; i < inventory.getTopInventory().getSize() - 9; i++) {
                        if (inventory.getTopInventory().getItem(i) != null)
                            tempInventory.setItem(i, inventory.getTopInventory().getItem(i));
                        else
                            tempInventory.setItem(i, new ItemStack(Material.AIR));
                    }

                    update(currentPage, tempInventory);

                    this.totalRows += 1;
                } else { //On a different page than what they're upgrading
                    Inventory tempInventory = Bukkit.createInventory(null, pages.get(this.numberOfPages).getSize() + 9); //Add the new row since this DOESN'T have the control bar

                    ListenerVault listenerVault = Main.getInstance().getListenerVaultInstance();
                    listenerVault.setLatestVersion(this.gang, this.numberOfPages);

                    Inventory pageInventory = pages.get(this.numberOfPages);

                    for (int i = 0; i < pageInventory.getSize(); i++) {
                        if (pageInventory.getItem(i) != null)
                            tempInventory.setItem(i, pageInventory.getItem(i));
                        else
                            tempInventory.setItem(i, new ItemStack(Material.AIR));
                    }

                    update(this.numberOfPages, tempInventory);

                    this.totalRows += 1;

                    sendPageUpdate(this.numberOfPages);

                    listenerVault.setLatestVersion(this.gang, this.numberOfPages);
                }
            }
        }
    }

    private void sendPageUpdate(int page) {
        // Grab all players that currently have an inventory open
        Iterator<Map.Entry<Player, InventoryView>> iterator = Main.getInstance().getListenerVaultInstance().hasInventoryOpen.entrySet().iterator();

        try {
            // Loop through all players that have an inventory open
            while (iterator.hasNext()) {
                Map.Entry<Player, InventoryView> entry = iterator.next();

                Player player = entry.getKey();
                InventoryView inventory = entry.getValue();
                String inventoryName = entry.getValue().getTitle();

                // Making sure they have a gang 'Vault' open
                if (inventoryName.contains(ChatColor.GRAY + "Vault")) {
                    // Grab the Gang ID from the chest in the middle of the bottom row of the inventory
                    int inventoryID = NBTTag.getInteger(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "gang_id");

                    // Making sure that the Gang ID isn't 0 and that it matches this Gangs ID
                    if (inventoryID != 0)
                        if (inventoryID == this.id) {
                            // Remove all letters from the inventory name, split at '/', and grab the first int from the args (Inventory name Ex: 'Vault Page 1/2')
                            int playerCurrentPage = Integer.parseInt(ChatColor.stripColor(inventoryName).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", ""));
                            // Make sure that the player has the same page open
                            if (playerCurrentPage == page) {
                                // Clear their cursor to prevent dropping the item since we'll be reopening the inventory
                                ItemStack cursor = player.getOpenInventory().getCursor();

                                if (cursor != null)
                                    player.getOpenInventory().setCursor(new ItemStack(Material.AIR));

                                // Open the newest version of the page
                                open(playerCurrentPage, player);

                                // Set the players cursor back to what it was
                                player.getOpenInventory().setCursor(cursor);
                            }
                        }
                }
            }
        } catch (ConcurrentModificationException ex) {
            //TODO
        }
    }

    /*
    Convert the 'pages' HashMap to a list for storage in the database
     */
    public List<String> toList() {
        List<String> list = new ArrayList<>();

        for (int i = 1; i <= pages.keySet().size(); i++)
            list.add(sterilize(pages.get(i)));

        return list;
    }
}
