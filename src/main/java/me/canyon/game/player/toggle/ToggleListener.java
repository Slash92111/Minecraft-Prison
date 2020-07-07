package me.canyon.game.player.toggle;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
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

public class ToggleListener implements Listener {

    private Main plugin;

    public ToggleListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerClickItem(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerData(player);
        InventoryView inventoryView = event.getView();
        Inventory gui = inventoryView.getTopInventory();
        String inventoryName = inventoryView.getTitle();

        if (inventoryName.contains(ChatColor.DARK_GRAY + "Toggles")) {
            ItemStack clickedItem = event.getCurrentItem();

            if (event.getRawSlot() >= 0 && event.getRawSlot() <= (gui.getSize() - 1)) {
                if (clickedItem != null && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                    net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(clickedItem);

                    if (nmsStack.hasTag()) {
                        NBTTagCompound tagCompound = nmsStack.getTag();

                        String id = tagCompound.getString("id");

                        ToggleEnum toggleEnum = ToggleEnum.getFromID(id);
                        Toggle toggle = playerData.getToggle();

                        toggle.set(toggleEnum, !toggle.get(toggleEnum));

                        gui.setContents(toggle.getUI().getContents());
                    }
                }

                event.setCancelled(true);
            }
        }
    }
}
