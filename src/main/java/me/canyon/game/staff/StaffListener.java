package me.canyon.game.staff;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class StaffListener implements Listener {

    private Main plugin;

    public StaffListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerClickItem(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player);
        InventoryView inventoryView = event.getView();
        Inventory gui = inventoryView.getTopInventory();
        String inventoryName = inventoryView.getTitle();

        if (inventoryName.contains(ChatColor.DARK_GRAY + "[Staff UI]")) {
            ItemStack clickedItem = event.getCurrentItem();

            if (event.getRawSlot() >= 0 && event.getRawSlot() <= (gui.getSize() - 1)) {
                if (clickedItem != null && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                    net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(clickedItem);

                    if (nmsStack.hasTag()) {
                        NBTTagCompound tagCompound = nmsStack.getTag();

                        String id = tagCompound.getString("id");

                        if (Punishment.getFromID(id) != null) {
                            PlayerData punished = plugin.getPlayerData(UUID.fromString(ChatColor.stripColor(inventoryName).split(" : ")[1]));

                            if (punished.getTempStrings().containsKey("punishmentType"))
                                punished.getTempStrings().replace("punishmentType", id);
                            else
                                punished.getTempStrings().put("punishmentType", id);

                            player.openInventory(plugin.getStaffUI().getPunishmentPage(punished, id));
                        } else if (Punishment.Type.getFromID(id) != null) {
                            PlayerData punished = plugin.getPlayerData(UUID.fromString(ChatColor.stripColor(inventoryName).split(" : ")[1]));

                            String typeID = punished.getTempStrings().get("punishmentType");

                            if (typeID != null && Punishment.getFromID(typeID) != null) {
                                Punishment punishment = Punishment.getFromID(typeID);
                                Punishment.Type punishmentType = Punishment.Type.getFromID(id);

                                switch(punishment) {
                                    case MUTE:
                                        plugin.getServer().dispatchCommand(player, "mute " + punished.getName() + " " + plugin.getPunishmentConfig().getString("mute." + id) + " " + punishmentType.getName());
                                        System.out.println("mute " + punished.getName() + " " + plugin.getPunishmentConfig().getString("mute." + id) + punishmentType.getName());
                                        break;
                                    case TEMP_BAN:
                                        plugin.getServer().dispatchCommand(player, "tempban " + punished.getName() + " " + plugin.getPunishmentConfig().getString("tempBan." + id) + " " + punishmentType.getName());
                                        break;
                                }
                            }
                        }


                    }
                }

                event.setCancelled(true);
            }
        }
    }
}
