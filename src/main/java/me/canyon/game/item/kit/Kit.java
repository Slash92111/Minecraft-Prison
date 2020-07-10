package me.canyon.game.item.kit;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Rank;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Kit {

    private Main plugin;
    private FileConfiguration kitsConfig;
    private ItemStack PLACE_HOLDER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);

    public Kit(Main plugin) {
        this.plugin = plugin;

        kitsConfig = plugin.getKitsConfig();

        ItemMeta meta = PLACE_HOLDER.getItemMeta();
        meta.setDisplayName(" ");
        PLACE_HOLDER.setItemMeta(meta);
    }

    /**
     * Returns the UI for a specified kit type (Game/Rank Kit)
     * @param player The player that will be redeeming/viewing kit(s)
     * @param kitType The type of kit(s) the player will be interacting with
     * @return Returns the Kits UI
     */
    public Inventory getUI(Player player, String kitType) {
        PlayerData playerData = plugin.getPlayerData(player);
        Rank rank = playerData.getRank();
        String rankName = rank.toString().toLowerCase();

        // Set the inventories name (Ex: 'game-kit' -> 'Game Kit')
        String inventoryName = WordUtils.capitalizeFully(kitType.replace("-", " "));

        // TODO figure out a way to combine the if else to a single block of code
        if (kitType.equals("rank-kits")) {
            // Create the UI and return it
            Inventory inventory = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + inventoryName);

            for (int i = 0; i < inventory.getSize(); i++)
                inventory.setItem(i, PLACE_HOLDER);

            if (kitsConfig.get(kitType + "." + rankName) != null) {
                Material material = Material.valueOf(kitsConfig.getString(kitType + "." + rankName + ".icon.material"));
                String name = ChatColor.translateAlternateColorCodes('&', kitsConfig.getString(kitType + "." + rankName + ".icon.name"));

                // Construct the kits lore
                List<String> lore  = new ArrayList<>();

                if (onCooldown(player, rankName)) {
                    lore.add(ChatColor.RED + "Time Left " + getCooldownLeft(player, rankName));
                    lore.add (" ");
                } else {
                    lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Ready to claim");
                    lore.add(" ");
                }

                for (String string : kitsConfig.getStringList(kitType + "." + rankName + ".icon.lore"))
                    lore.add(ChatColor.translateAlternateColorCodes('&', string));

                // Set the items display name and lore
                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(lore);
                item.setItemMeta(meta);

                // Set the 'id' and 'type' NBT tags for the KitListener class
                net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
                tagCompound.setString("id", rankName);
                tagCompound.setString("type", kitType);
                nmsItem.setTag(tagCompound);
                item = CraftItemStack.asBukkitCopy(nmsItem);

                inventory.setItem(inventory.getSize() - 5, item);
            }

            return inventory;
        } else if (kitType.equals("game-kits")) {
            int amountOfKits = kitsConfig.getConfigurationSection(kitType).getKeys(false).size();
            int inventorySize;

            if (amountOfKits <= 9)
                inventorySize = 9;
            else
                inventorySize = amountOfKits / 9;

            Inventory inventory = Bukkit.createInventory(null, inventorySize, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + inventoryName);

            int amountOfItems = 0;

            for (String kit : kitsConfig.getConfigurationSection(kitType).getKeys(false)) {
                Material material = Material.valueOf(kitsConfig.getString(kitType + "." + kit + ".icon.material"));
                String name = ChatColor.translateAlternateColorCodes('&', kitsConfig.getString(kitType + "." + kit + ".icon.name"));

                // Construct the kits lore
                List<String> lore  = new ArrayList<>();

                if (playerData.getKitLevel(kit) != 0) {
                    if (onCooldown(player, kit)) {
                        lore.add(" ");
                        lore.add(ChatColor.RED + "Time Left " + getCooldownLeft(player, kit));
                        lore.add (" ");
                    } else {
                        lore.add(" ");
                        lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Ready to claim");
                        lore.add(" ");
                    }
                } else {
                    lore.add(" ");
                    lore.add(ChatColor.RED + "" + ChatColor.BOLD + "LOCKED:" + ChatColor.GRAY + " Use a " + ChatColor.stripColor(name) + " Beacon");
                    lore.add(ChatColor.GRAY + "to obtain access to this kit.");
                    lore.add(ChatColor.GRAY + "You can find them in-game");
                    lore.add(ChatColor.GRAY + "or on our /store");
                    lore.add(" ");
                }

                for (String string : kitsConfig.getStringList(kitType + "." + kit + ".icon.lore"))
                    lore.add(ChatColor.translateAlternateColorCodes('&', string));

                // Set the items display name and lore
                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(lore);
                item.setItemMeta(meta);

                // Set the 'id' and 'type' NBT tags for the KitListener class
                net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
                tagCompound.setString("id", kit);
                tagCompound.setString("type", kitType);
                nmsItem.setTag(tagCompound);
                item = CraftItemStack.asBukkitCopy(nmsItem);

                inventory.addItem(item);
                amountOfItems += 1;
            }

            for (int i = amountOfItems; i < inventory.getSize(); i++)
                inventory.setItem(i, PLACE_HOLDER);

            return inventory;
        }

        return null;
    }

    /**
     * Open a kits UI that refreshes for real-time cooldown updates
     * @param player The player that will be redeeming/viewing kit(s)
     * @param kitType The type of kit(s) the player will be interacting with
     */
    public void getUIWithRefresh(Player player, String kitType) {
        player.openInventory(getUI(player, kitType));
        String inventoryName = WordUtils.capitalizeFully(kitType.replace("-", " "));

        new BukkitRunnable() {
            public void run() {
                // Making sure that the player is in the correct inventory
                if (player.getOpenInventory().getTitle().contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + inventoryName)) {
                    // Replace the contents rather than reopen the inventory
                    player.getOpenInventory().getTopInventory().setContents(getUI(player, kitType).getContents());
                } else
                    this.cancel();
            }
        }.runTaskTimer(this.plugin, 0, 20); // Refresh the inventory every second
    }

    /**
     * Get the kits contents specified to a players level
     * @param player The player that is redeeming the kit
     * @param kitType The type of kit
     * @param kitID The kits ID
     * @return Return a list of ItemStacks from the kit
     */
    public List<ItemStack> getKit(Player player, String kitType, String kitID) {
        List<ItemStack> items = new ArrayList<>();

        // Loop through the items in the kits.yml
        for (String item : kitsConfig.getConfigurationSection(kitType + "." + kitID + ".items").getKeys(false)) {
            if (item.toLowerCase().matches("helmet|chestplate|leggings|boots")) {
                // Convert helmets/chestplates/leggings/boots to the players level
                items.add(getTieredItem(player, kitsConfig.getString(kitType + "." + kitID + ".items." + item + ".name"), kitsConfig.getStringList(kitType + "." + kitID + ".items." + item + ".lore"), item));
            } else {
                // Construct the item based off the kits.yml and add it to the list to return
                Material material = Material.valueOf(item);
                String name = ChatColor.translateAlternateColorCodes('&', kitsConfig.getString(kitType + "." + kitID + ".items." + item + ".name"));
                List<String> lore  = new ArrayList<>();

                for (String string : kitsConfig.getStringList(kitType + "." + kitID + ".items." + item + ".lore"))
                    lore.add(ChatColor.translateAlternateColorCodes('&', string));

                ItemStack itemStack = new ItemStack(material, 1);
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(lore);
                itemStack.setItemMeta(meta);

                items.add(itemStack);
            }
        }
        return items;
    }

    /**
     *
     * @param player That is redeeming the kit
     * @param name The name of the item
     * @param lore The lore of the item
     * @param materialString The type of item
     * @return Returns an ItemStack of the item built through the specified parameters
     */
    private ItemStack getTieredItem(Player player, String name, List<String> lore, String materialString) {
        PlayerData playerData = plugin.getPlayerData(player);
        long playerLevel = playerData.getLevel();
        String tier;
        int requiredLevel;

        if (playerLevel >= 90) {
            tier = "DIAMOND_";
            requiredLevel = 90;
        } else if (playerLevel >= 60) {
            tier = "IRON_";
            requiredLevel = 60;
        } else if (playerLevel >= 40) {
            tier = "GOLDEN_";
            requiredLevel = 40;
        } else if (playerLevel >= 20) {
            tier = "CHAINMAIL_";
            requiredLevel = 20;
        } else {
            tier = "LEATHER_";
            requiredLevel = 1;
        }

        // Construct the item based off the parameters and return it
        Material material = Material.valueOf(tier + materialString);

        name = name.replace("%material%", WordUtils.capitalizeFully(material.toString().replace("_", " ")));
        List<String> newLore  = new ArrayList<>();

        int finalRequiredLevel = requiredLevel;
        lore.forEach(string -> newLore.add(ChatColor.translateAlternateColorCodes('&', string.replace("%required_level%", "" + finalRequiredLevel))));

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(newLore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Returns a kits preview UI
     * @param player The player that is viewing the kit
     * @param kitType The type of kit
     * @param kitID The kits ID
     * @return Returns a kits preview UI
     */
    public Inventory preview(Player player, String kitType, String kitID) {
        String name = "";
        if (kitType.equals("rank-kits"))
            name = kitID.toUpperCase().replace("_PLUS", "+");
        else if (kitType.equals("game-kits"))
            name = WordUtils.capitalizeFully(kitID.replace("-", " "));

        int amountOfKits = kitsConfig.getConfigurationSection(kitType + "." + kitID + ".items").getKeys(false).size();
        int inventorySize;

        // Set the inventory size based off the amount of kits
        if (amountOfKits <= 9)
            inventorySize = 9;
        else
            inventorySize = (int) Math.ceil((float) amountOfKits / 9) * 9;

        // Create the inventory
        Inventory inventory = Bukkit.createInventory(null, inventorySize + 18, ChatColor.DARK_GRAY + name + " Kit Preview");

        // Set the inventory controls
        ItemStack backArrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = backArrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.YELLOW + "<- Previous Page");
        backArrow.setItemMeta(arrowMeta);

        for (int i = 0; i < inventory.getSize(); i++)
            inventory.setItem(i, PLACE_HOLDER);

        inventory.setItem(0, backArrow);

        // Set the kits contents starting in the second row of the inventory
        int j = 9;
        for (ItemStack item : getKit(player, kitType, kitID)) {
            inventory.setItem(j, item);
            j++;
        }

        return inventory;
    }

    /**
     * Check if a kits currently on cooldown
     * @param player The player that's checking if the kits on cooldown
     * @param kitID The kit ID
     * @return Returns if the kit is on cooldown
     */
    public boolean onCooldown(Player player, String kitID) {
        // returns if the cooldown date is before the current time
        return !(plugin.getPlayerData(player).getKitCooldown(kitID).getTime() <= new Date().getTime());
    }

    /**
     * Get the remaining time the kit is on cooldown in a formatted version
     * @param player The player that is currently on cooldown
     * @param kitID The kit id that is on cooldown
     * @return Returns a formatted date in a string
     */
    public String getCooldownLeft(Player player, String kitID) {
        String timeLeft;

        Date expires = plugin.getPlayerData(player).getKitCooldown(kitID);
        Date currentTime = new Date();

        long millis = Math.abs(currentTime.getTime() - expires.getTime());

        timeLeft = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

        return timeLeft;
    }
}
