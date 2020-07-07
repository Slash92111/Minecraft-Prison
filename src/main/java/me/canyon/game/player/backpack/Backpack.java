package me.canyon.game.player.backpack;

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
                pages.put(page, desteralize(backpackList.get(i), page));
            }
         else {
            Inventory inventory = Bukkit.getServer().createInventory(null, 9, "");
            pages.put(1, desteralize(steralize(inventory), 1));
        }

        this.numberOfPages = pages.size();
    }

    public int getNumberOfPages() { return this.numberOfPages; }

    public UUID getUUID() { return this.uuid; }

    public Player getOwner() { return Bukkit.getPlayer(uuid); }

    private String steralize(Inventory inventory) {
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

    private Inventory desteralize(String string, int pageNumber) {
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
                    Inventory pageInventory = pages.get(this.numberOfPages);

                    for (int i = 0; i < pageInventory.getSize(); i++) {
                        if (pageInventory.getItem(i) != null)
                            tempInventory.setItem(i, pageInventory.getItem(i));
                        else
                            tempInventory.setItem(i, new ItemStack(Material.AIR));
                    }

                    update(this.numberOfPages, tempInventory);

                    this.totalRows += 1;
                }
            }
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

    public void open (int page, Player player, boolean admin) {
        player.openInventory(new BackpackUI(page, this, admin).getInventory());
    }

    public String toString() {
        String string = "";

        for (int i = 1; i <= pages.keySet().size(); i++) {
            string += steralize(pages.get(i));

            if (i < pages.keySet().size())
                string += ",";
        }

        return string;
    }

    public List<String> toList() {
        List<String> list = new ArrayList<>();

        for (Inventory inventory : pages.values())
            list.add(steralize(inventory));

        return list;
    }

    private static class BackpackUI {
        private int page;
        private Backpack backpack;
        private String ID;

        private Inventory inventory;

        private ItemStack UPGRADE;
        private ItemStack NEXT_PAGE;
        private ItemStack PREV_PAGE;

        private ItemStack PLACE_HOLDER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1); //, (short) 15

        private boolean admin; //Other user is opening the backpack, so we need to remove the upgrade button and replace it w/ the owners UUID

        public BackpackUI(int page, Backpack backpack, boolean admin) {
            this.page = page;
            this.ID = backpack.getUUID().toString();
            this.admin = admin;

            this.inventory = Bukkit.createInventory(null, backpack.getPage(page).getSize() + 9, ChatColor.GRAY + "Backpack Page " + page + "/" + backpack.numberOfPages);

            if (!admin)
                this.UPGRADE = createItem(new ItemStack(Material.CHEST), ChatColor.DARK_PURPLE + "Upgrade Backpack Size", new String[] {
                        ChatColor.GRAY + "Click to upgrade backpack size",
                        "",
                        ChatColor.YELLOW + "Current Size:",
                        ChatColor.GRAY + "Rows: " + ChatColor.YELLOW + backpack.totalRows,
                        "",
                        ChatColor.GREEN + "Next Size:",
                        ChatColor.GRAY + "Rows: " + ChatColor.GREEN + backpack.getNextSize(),
                        "",
                        ChatColor.GRAY + "Cost: " + ChatColor.GOLD + new Utilities().formatEconomy(Long.parseLong(backpack.getUpgradeCost() + ""))
                });
            else
                this.UPGRADE = createItem(new ItemStack(Material.CHEST), ChatColor.DARK_PURPLE + "Owner: " + this.ID, new String[] {
                        ChatColor.GRAY + "Name: " + backpack.playerData.getName()
                });

            //this.NEXT_PAGE = new ItemStack(Material.ARROW);
            //this.PREV_PAGE = new ItemStack(Material.ARROW);

            this.NEXT_PAGE = createItem(new ItemStack(Material.ARROW), ChatColor.YELLOW + "Next Page ->", null);

            this.PREV_PAGE = createItem(new ItemStack(Material.ARROW), ChatColor.YELLOW + "<- Previous Page", null);

            //Set Controls
            ItemMeta meta = PLACE_HOLDER.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "Empty");
            PLACE_HOLDER.setItemMeta(meta);

            int yMod = (inventory.getSize() - 9);

            for (int i = 0; i < 9; i++)
                this.inventory.setItem(yMod + i, PLACE_HOLDER);

            this.inventory.setItem(yMod + 4, this.UPGRADE);

            if (page == backpack.getNumberOfPages() && page != 1)
                this.inventory.setItem(yMod, PREV_PAGE);
            else if (page < backpack.getNumberOfPages() && page > 1) {
                this.inventory.setItem(yMod + 8, NEXT_PAGE);
                this.inventory.setItem(yMod, PREV_PAGE);
            } else if (page < backpack.getNumberOfPages())
                this.inventory.setItem(yMod + 8, NEXT_PAGE);

            //Set Contents
            Inventory contents = backpack.getPage(page);

            for (int i = 0; i < backpack.getPage(page).getSize(); i++)
                if (contents.getItem(i) != null)
                    inventory.setItem(i, contents.getItem(i));
                else
                    inventory.setItem(i, new ItemStack(Material.AIR));
        }

        public Inventory getInventory() { return inventory; }

        private void setContents() {
            Inventory contents = backpack.getPage(this.page);

            for (int i = 0; i < backpack.getPage(page).getSize(); i++)
                if (contents.getItem(i) != null)
                    inventory.setItem(i, contents.getItem(i));
                else
                    inventory.setItem(i, new ItemStack(Material.AIR));
        }

        private void setControls(Inventory inventory) {
            ItemMeta meta = PLACE_HOLDER.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "Empty");
            PLACE_HOLDER.setItemMeta(meta);

            int yMod = (inventory.getSize() / 9) - 1;

            for (int i = 1; i < 10; i++)
                this.inventory.setItem(yMod + i, PLACE_HOLDER);

            this.inventory.setItem(yMod + 5, this.UPGRADE);

            if (page == backpack.getNumberOfPages() && page != 1)
                this.inventory.setItem(yMod + 1, PREV_PAGE);
            else if (page < backpack.getNumberOfPages() && page > 1) {
                this.inventory.setItem(yMod + 9, NEXT_PAGE);
                this.inventory.setItem(yMod + 1, PREV_PAGE);
            } else if (page < backpack.getNumberOfPages())
                this.inventory.setItem(yMod + 9, NEXT_PAGE);
        }

        private ItemStack createItem(ItemStack item, String name, String[] lore) {
            ItemMeta im = item.getItemMeta();
            im.setDisplayName(name);

            if (lore != null)
                im.setLore(Arrays.asList(lore));

            item.setItemMeta(im);
            return item;
        }
    }
}
