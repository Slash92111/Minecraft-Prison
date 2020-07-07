package me.canyon.game.command;

import me.canyon.Main;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandItem extends BaseCommand {
    private int staffLevel;

    public CommandItem(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();
    private FileConfiguration itemsConfig = plugin.getItemsConfig();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
            return false;

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageFromConfig("correctUsage", Map.of("%correct_usage%", "/item <item>")));
            return false;
        }

        String itemID = args[0].toLowerCase();

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

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player player = (Player) sender;

        if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
            return null;

        if (cmd.getName().toLowerCase().equals("item")) {
            if (args.length <= 1) {
                return new ArrayList<>(itemsConfig.getConfigurationSection("items").getKeys(false));
            }
        }

        return null;
    }
}
