package me.canyon.game.item;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitListener implements Listener {

    private Main plugin;
    private List<String> availableKits;
    private FileConfiguration kitsConfig;

    private HashMap<Player, InventoryView> previousPage = new HashMap<>();

    public KitListener(Main plugin) {
        this.plugin = plugin;
        this.kitsConfig = plugin.getKitsConfig();

        this.availableKits = new ArrayList<>(kitsConfig.getConfigurationSection("rank-kits").getKeys(false));
        this.availableKits.addAll(kitsConfig.getConfigurationSection("game-kits").getKeys(false));
    }

    /**
     * Handles the InventoryClickEvent in a kit inventory
     *
     * If a player right-clicks a kit it'll open a preview of it
     * If a player left-clicks a kit that is unlocked and isn't on cooldown it'll "claim" it for them
     */
    @EventHandler
    public void kitClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player);
        InventoryView inventoryView = event.getView();
        Inventory gui = inventoryView.getTopInventory();
        String inventoryName = inventoryView.getTitle();

        // Verify the inventory is either a Rank Kit or Game Kit UI
        if (inventoryName.contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Rank Kits") || inventoryName.contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Game Kits")) {
            ItemStack clickedItem = event.getCurrentItem();

            // Making sure that the item clicked is in the UI and not in their inventory
            if (event.getRawSlot() >= 0 && event.getRawSlot() <= (gui.getSize() - 1))
                // Making sure that the item isn't null or a place holder
                if (clickedItem != null && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                    // Convert the Bukkit ItemStack to a CraftItemStack to get the items NBT tags
                    net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(clickedItem);

                    if (nmsStack.hasTag()) {
                        NBTTagCompound tagCompound = nmsStack.getTag();

                        String kitID = tagCompound.getString("id");
                        String kitType = tagCompound.getString("type");

                        // Verify that the type is either a rank kit or a game kit and that the ID is in the kits.yml file
                        if (kitType.equals("rank-kits")  || kitType.equals("game-kits") && this.availableKits.contains(kitID)) {
                            String kitDisplayName;

                            // Create the kits icon name
                            if (kitType.equals("rank-kits"))
                                kitDisplayName = kitID.toUpperCase().replace("_PLUS", "+");
                            else
                                kitDisplayName = WordUtils.capitalizeFully(kitID.replace("-", " "));

                            if (event.getClick() == ClickType.LEFT) {

                                // If the player tries to claim a locked kit cancel the event and return
                                if (kitType.equals("game-kits") && playerData.getKitLevel(kitID) == 0) {
                                    event.setCancelled(true);
                                    return;
                                }

                                String strKitType = "Rank-Kit";

                                if (kitType.equals("game-kits"))
                                    strKitType = "G-Kit";

                                // Making sure that the kit isn't currently on cooldown
                                if (!plugin.getKitInstance().onCooldown(player, kitID)) {
                                    String cooldown;

                                    /*
                                    Since game kits have multiple cooldowns dependant on it's level
                                    we'll need to make sure it is a game kit and grab their kit levels cooldown
                                     */
                                    if (kitType.equals("game-kits")) {
                                        int kitLevel = playerData.getKitLevel(kitID);
                                        cooldown = kitsConfig.getString(kitType + "." + kitID + ".cooldown." + kitLevel);
                                    } else
                                        cooldown = kitsConfig.getString(kitType + "." + kitID + ".cooldown");

                                    // Set the kits cooldown, tell the player it's been redeemed, and give them the kits contents
                                    setCooldown(player, kitID, cooldown);
                                    player.sendMessage(plugin.getMessageFromConfig("kit.redeemed", Map.of("%kit%", kitDisplayName, "%cooldown%", plugin.getKitInstance().getCooldownLeft(player, kitID))));
                                    plugin.getKitInstance().getKit(player, kitType, kitID).forEach(itemStack -> player.getInventory().addItem(itemStack));
                                } else {
                                    // Tell the player that the kit is currently on cooldown
                                    player.sendMessage(plugin.getMessageFromConfig("item.currentlyOnCooldown", Map.of("%kit_name%", kitDisplayName + " " + strKitType)));
                                }
                            } else { // Player right-clicked the item
                                // Open a preview of the kit and store the current page before doing so
                                previousPage.put(player, inventoryView);
                                player.openInventory(plugin.getKitInstance().preview(player, kitType, kitID));
                            }

                            event.setCancelled(true);
                        }
                    }
                } else
                    event.setCancelled(true);
        } else if (inventoryName.contains("Kit Preview")) {
            // Making sure that the player is clicking in the UI
            if (event.getRawSlot() >= 0 && event.getRawSlot() <= (gui.getSize() - 1)) {
                ItemStack clickedItem = event.getCurrentItem();

                // If the player clicked on the 'back' arrow take them to their previos page that's stored in the 'previosPage' HashMap
                if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                    if (previousPage.containsKey(player)) {
                        if (previousPage.get(player).getTitle().contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Rank Kits"))
                            plugin.getKitInstance().getUIWithRefresh(player, "rank-kits");
                        else if (previousPage.get(player).getTitle().contains(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Game Kits"))
                            plugin.getKitInstance().getUIWithRefresh(player, "game-kits");
                        else
                            player.openInventory(previousPage.get(player));

                        previousPage.remove(player);
                    }
                }

                // Cancel the event to make sure the player isn't able to take any items their not suppose to
                event.setCancelled(true);
            }
        }
    }

    /**
     * Set a players kit on a specified cooldown time
     * @param player The player we'll be applying the cooldown to
     * @param kitID The Kit ID that'll have the cooldown
     * @param time The amount of time the kit will be on cooldown
     */
    public void setCooldown(Player player, String kitID, String time) {
        // Convert the 'time' string to a Date object
        String[] times = time.split(":");

        long millis = System.currentTimeMillis();

        millis += Cooldown.getTicks("DAY", Integer.parseInt(times[0]));
        millis += Cooldown.getTicks("HOUR", Integer.parseInt(times[1]));
        millis += Cooldown.getTicks("MINUTE", Integer.parseInt(times[2]));
        millis += Cooldown.getTicks("SECOND", Integer.parseInt(times[3]));

        // Set the cooldown in the players PlayerData object
        plugin.getPlayerData(player).setKitCooldown(kitID, new Date(millis));
    }

    private enum Cooldown {
        SECOND("sec", 1),
        MINUTE("min", 60),
        HOUR("hour", 60*60),
        DAY("day", 60*60*24);

        private String unit;
        private int multiplier;

        Cooldown(String unit, int multiplier) {
            this.unit = unit;
            this.multiplier = multiplier;
        }

        /**
         * Converts a time unit to milliseconds
         * @param unit Unit of time
         * @param time Amount of the specified unit
         * @return Returns the amount of time in milliseconds
         */
        public static long getTicks(String unit, int time) {
            return (time * (Cooldown.valueOf(unit).multiplier * 1000));
        }
    }
}
