package me.canyon.game.staff;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StaffUI {

    private Main plugin;

    public StaffUI(Main plugin) {
        this.plugin = plugin;
    }

    private ItemStack PLACE_HOLDER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);

    public Inventory getMainPage(PlayerData punished) {
        Inventory inventory;
        int amountOfPunishments = Punishment.values().length;
        int inventorySize;

        if (amountOfPunishments < 9)
            inventorySize = 9;
        else
            inventorySize = (int) Math.ceil((float) amountOfPunishments / 9) * 9;

        inventory = Bukkit.createInventory(null, inventorySize, ChatColor.DARK_GRAY + "[Staff UI] " + punished.getName() + " : " + punished.getUUID());

        for (int i = 0; i < inventorySize; i++)
            inventory.setItem(i, PLACE_HOLDER);

        List<String> sorted = new ArrayList<>();

        for (Punishment punishment : Punishment.values())
            sorted.add(punishment.getID());

        Collections.sort(sorted);

        int i = 0;
        for (String id : sorted) {
            inventory.setItem(i, createItem(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChatColor.LIGHT_PURPLE + Punishment.getFromID(id).getName(), id, null));
            i++;
        }

        return inventory;
    }

    public Inventory getPunishmentPage(PlayerData punished, String type) {
        Inventory inventory;
        Set<String> types = plugin.getPunishmentConfig().getConfigurationSection(type).getKeys(false);
        int amountOfPunishments = types.size();
        int inventorySize;

        if (amountOfPunishments < 9)
            inventorySize = 9;
        else
            inventorySize = (int) Math.ceil((float) amountOfPunishments / 9) * 9;

        inventory = Bukkit.createInventory(null, inventorySize, ChatColor.DARK_GRAY + "[Staff UI] " + punished.getName() + " : " + punished.getUUID());

        for (int i = 0; i < inventorySize; i++)
            inventory.setItem(i, PLACE_HOLDER);

        inventory.setItem(0, createItem(PLACE_HOLDER, "", type, null));

        List<String> sorted = new ArrayList<>(types);

        Collections.sort(sorted);

        int i = 0;
        for (String id : sorted) {
            inventory.setItem(i, createItem(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChatColor.LIGHT_PURPLE + Punishment.Type.getFromID(id).getName(), id, null));
            i++;
        }

        return inventory;
    }

    private ItemStack createItem(ItemStack item, String name, String id, String[] lore) {
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(name);

        if (lore != null)
            im.setLore(Arrays.asList(lore));

        item.setItemMeta(im);

        net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        tagCompound.setString("id", id);

        nmsItem.setTag(tagCompound);
        item = CraftItemStack.asBukkitCopy(nmsItem);

        return item;
    }



}
