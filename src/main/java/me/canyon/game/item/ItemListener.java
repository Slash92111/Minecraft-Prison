package me.canyon.game.item;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Rank;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemListener implements Listener {

    private Main plugin;
    private FileConfiguration messageConfig;

    private List<String> customItems;

    public ItemListener (Main plugin) {
        this.plugin = plugin;
        FileConfiguration itemsConfig = plugin.getItemsConfig();
        this.messageConfig = plugin.getMessageConfig();

        this.customItems = new ArrayList<>(itemsConfig.getConfigurationSection("items").getKeys(false));
    }

    @EventHandler
    public void playerUseItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();

            if (heldItem.getType() != Material.AIR) {
                if (!plugin.hasPlayerData(player))
                    plugin.setPlayerData(player);

                PlayerData playerData = plugin.getPlayerData(player);

                net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(heldItem);

                if (nmsStack.hasTag()) {
                    NBTTagCompound tagCompound = nmsStack.getTag();

                    String itemID = tagCompound.getString("id");

                    if (customItems.contains(itemID)) {
                        if (itemID.contains("backpack-upgrade")) {
                            int amount = Integer.parseInt(itemID.split("-")[2]);

                            if (playerData.getMaxBackpackRows() < playerData.getHardcapBackpackRows()) {

                                if (playerData.getMaxBackpackRows() + amount <= playerData.getHardcapBackpackRows()) {
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageConfig.getString("item.backpackPageUpgradeItemUse").replace("%a", ""+ amount)));
                                    playerData.setMaxBackpackRows(playerData.getMaxBackpackRows() + amount);
                                } else {
                                    int remainder = (playerData.getMaxBackpackRows() + amount) - playerData.getHardcapBackpackRows();

                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageConfig.getString("item.backpackPageUpgradeItemUse").replace("%a", ""+ (amount - remainder))));
                                    playerData.setMaxBackpackRows(playerData.getMaxBackpackRows() + (amount - remainder));

                                    giveItem(player, "backpack-upgrade-" + remainder);
                                }

                                usedItemInHand(player, 1);
                            } else
                                player.sendMessage(plugin.getMessageFromConfig("item.backpackMaxedOut"));

                        } else if (itemID.contains("kit-upgrade")) {
                            String kitID = itemID.split("-")[2];

                            if (playerData.getKitLevel(kitID) < 3) {
                                playerData.setKitLevel(kitID, playerData.getKitLevel(kitID) + 1);
                                player.sendMessage(plugin.getMessageFromConfig("item.gkitLevelUp", Map.of("%kit_name%", WordUtils.capitalizeFully(kitID))));

                                usedItemInHand(player, 1);
                            } else
                                player.sendMessage(plugin.getMessageFromConfig("item.gkitMaxLevel", Map.of("%kit_name%", WordUtils.capitalizeFully(kitID))));
                        } else if (itemID.contains("rank-upgrade")) {
                            Rank rank = Rank.valueOf(itemID.split("-")[2].toUpperCase());
                            String rankName =  rank.toString().replace("_PLUS", "+").toUpperCase();

                            if (playerData.getRank().getOrder() >= rank.getOrder()) {
                                player.sendMessage(plugin.getMessageFromConfig("item.alreadyBetterRank", Map.of("%rank_name%", rankName)));
                                return;
                            }

                            player.sendMessage(plugin.getMessageFromConfig("item.redeemedRank", Map.of("%rank_name%", rankName)));
                            playerData.setRank(rank);

                            playerData.setMaxBackpackRows(playerData.getMaxBackpackRows() + Integer.parseInt(tagCompound.getString("max-backpack-rows")));
                            playerData.setHardcapBackpackRows(Integer.parseInt(tagCompound.getString("hardcap-backpack-rows")));
                            playerData.setMaxHomes(playerData.getMaxHomes() + Integer.parseInt(tagCompound.getString("max-homes")));

                            usedItemInHand(player, 1);
                        }
                    }
                }
            }
        }
    }

    private void usedItemInHand(Player player, int amount) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - amount);
        } else
            player.getInventory().setItemInMainHand(null);
    }

    private void giveItem(Player player, String itemID) {
        FileConfiguration itemsConfig = plugin.getItemsConfig();

        if (itemsConfig.get("items." + itemID) != null) {
            Material material = Material.valueOf(itemsConfig.getString("items." + itemID + ".material"));
            String name = ChatColor.translateAlternateColorCodes('&', itemsConfig.getString("items." + itemID + ".name"));
            List<String> lore  = new ArrayList<>();

            for (String string : itemsConfig.getStringList("items." + itemID + ".lore"))
                lore.add(ChatColor.translateAlternateColorCodes('&', string));

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);

            net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
            tagCompound.setString("id", itemID);

            itemsConfig.getStringList("items." + itemID + ".nbt").forEach(string -> tagCompound.setString(string.split(":")[0], string.split(":")[1]));

            nmsItem.setTag(tagCompound);
            item = CraftItemStack.asBukkitCopy(nmsItem);

            player.getInventory().addItem(item);
        }
    }
}
