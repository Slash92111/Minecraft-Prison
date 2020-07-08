package me.canyon.game.player.backpack;

import me.canyon.Main;
import me.canyon.game.item.NBTTag;
import me.canyon.game.player.PlayerData;
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

public class Backpack {

    private HashMap<Integer, Inventory> pages = new HashMap<>();

    private PlayerData playerData;

    private UUID uuid;
    private int numberOfPages, totalRows, upgradeCost;

    public Backpack(PlayerData playerData) {
        this.uuid = playerData.getUUID();

        this.totalRows = playerData.getTotalBackpackRows();
        this.upgradeCost = 15000;

        this.playerData = playerData;

        load();
    }

    private void load() {
        List<String> backpackList = playerData.getBackpackList();

        if (backpackList != null && backpackList.size() != 0)
            for (int i = 0; i < backpackList.size(); i++) {
                int page = i + 1;
                pages.put(page, deserialize(backpackList.get(i), page));
            }
         else {
            Inventory inventory = Bukkit.getServer().createInventory(null, 9, "");
            pages.put(1, deserialize(sterilize(inventory), 1));
        }

        this.numberOfPages = pages.size();
    }

    public int getNumberOfPages() { return this.numberOfPages; }

    public UUID getUUID() { return this.uuid; }

    public Player getOwner() { return Bukkit.getPlayer(uuid); }

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

    private Inventory deserialize(String string, int pageNumber) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(string));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt(), ChatColor.GRAY + "Backpack Page " + pageNumber + "/" + this.numberOfPages);

            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();

            return inventory;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public Inventory getPage(int page) {
        return pages.get(page);
    }

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

    public int getTotalRows() { return this.totalRows; }

    public boolean isMaxed() {
        return playerData.getMaxBackpackRows() == this.totalRows;
    }

    public void upgrade(InventoryView inventory) {
        int currentPage = Integer.parseInt(ChatColor.stripColor(inventory.getTitle()).replaceAll("[a-zA-Z]", "").split("/")[0].replaceAll("\\s", "")); //Strips a-z, A-Z, and spaces

        if (playerData.getMaxBackpackRows() > this.totalRows) { //Verify they're not maxed out on pages/rows already

            if ((this.numberOfPages * 5) == this.totalRows) { //Create new page if it's needed
                if (currentPage == this.numberOfPages) { //Save current page they're on
                    Inventory temp = Bukkit.createInventory(null, inventory.getTopInventory().getSize() - 9);

                    for (int i = 0; i < temp.getSize(); i++)
                        if (inventory.getTopInventory().getItem(i) != null)
                            temp.setItem(i, inventory.getTopInventory().getItem(i));
                        else
                            temp.setItem(i, new ItemStack(Material.AIR));

                    if (playerData.getMaxBackpackRows() > this.totalRows)
                        update(currentPage, temp);
                }

                pages.put(this.numberOfPages + 1, Bukkit.createInventory(null, 9, "Backpack Page " + this.numberOfPages + 1));

                this.numberOfPages += 1;
                this.totalRows += 1;

                ListenerBackpack listenerBackpack = Main.getInstance().getListenerBackpackInstance();

                listenerBackpack.setLatestVersion(this.uuid, this.numberOfPages);
                sendPageUpdate(this.numberOfPages - 1);
                listenerBackpack.setLatestVersion(this.uuid, this.numberOfPages);

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

                    ListenerBackpack listenerBackpack = Main.getInstance().getListenerBackpackInstance();
                    listenerBackpack.setLatestVersion(this.uuid, this.numberOfPages);

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

                    listenerBackpack.setLatestVersion(this.uuid, this.numberOfPages);
                }
            }
        }
    }

    private void sendPageUpdate(int page) {
        // Grab all players that currently have an inventory open
        Iterator<Map.Entry<Player, InventoryView>> iterator = Main.getInstance().getListenerBackpackInstance().hasBackpackOpen.entrySet().iterator();

        try {
            // Loop through all players that have an inventory open
            while (iterator.hasNext()) {
                Map.Entry<Player, InventoryView> entry = iterator.next();

                Player player = entry.getKey();
                InventoryView inventory = entry.getValue();
                String inventoryName = entry.getValue().getTitle();

                if (inventoryName.contains(ChatColor.GRAY + "Backpack")) {

                    UUID inventoryUUID = UUID.fromString(NBTTag.getString(inventory.getTopInventory().getItem(inventory.getTopInventory().getSize() - 5), "player_id"));

                    // Making sure that the Gang ID isn't 0 and that it matches this Gangs ID
                    if (inventoryUUID.equals(this.uuid)) {
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

    public String getNextSize() {
        int upgraded = this.totalRows + 1;

        if (upgraded <= playerData.getMaxBackpackRows())
            return upgraded + "";
        else
            return "Maxed Size";
    }

    public int getUpgradeCost() {
        if (!getNextSize().equalsIgnoreCase("maxed size"))
            return (this.totalRows + 1) * this.upgradeCost;

        return 0;
    }

    public void open (int page, Player player) {
        player.openInventory( setUI(page));
    }

    public String toString() {
        String string = "";

        for (int i = 1; i <= pages.keySet().size(); i++) {
            string += sterilize(pages.get(i));

            if (i < pages.keySet().size())
                string += ",";
        }

        return string;
    }

    public List<String> toList() {
        List<String> list = new ArrayList<>();

        for (Inventory inventory : pages.values())
            list.add(sterilize(inventory));

        return list;
    }

    private Inventory setUI(int page) { return setUI(getPage(page), page); }

    public Inventory setUI(Inventory inventory, int page) {
        Inventory ui = Bukkit.createInventory(null, inventory.getSize() + 9, ChatColor.GRAY + "Backpack Page " + page + "/" + this.numberOfPages);

        ItemStack PLACE_HOLDER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);

        ItemStack UPGRADE = Utilities.createItem(new ItemStack(Material.CHEST), ChatColor.DARK_PURPLE + "Upgrade Backpack Size", new String[] {
                ChatColor.GRAY + "Click to upgrade backpack size",
                "",
                ChatColor.YELLOW + "Current Size:",
                ChatColor.GRAY + "Rows: " + ChatColor.YELLOW + totalRows,
                "",
                ChatColor.GREEN + "Next Size:",
                ChatColor.GRAY + "Rows: " + ChatColor.GREEN + getNextSize(),
                "",
                ChatColor.GRAY + "Cost: " + ChatColor.GOLD + new Utilities().formatEconomy(Long.parseLong(getUpgradeCost() + ""))
        });

        // Set the NBT tag for gang ID onto the 'upgrade' chest
        UPGRADE = NBTTag.setString(UPGRADE, "id", "upgrade");
        UPGRADE = NBTTag.setString(UPGRADE, "player_id", getUUID().toString());

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
}
