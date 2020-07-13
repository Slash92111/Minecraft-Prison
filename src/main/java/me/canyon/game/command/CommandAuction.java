package me.canyon.game.command;

import me.canyon.Main;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandAuction extends BaseCommand {

    public CommandAuction(String command, String usage, String description) {
        super(command, usage, description);
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length == 0) {
            player.openInventory(plugin.getAuctionHouseInstance().test(1));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR)
                    return false;

                plugin.getAuctionHouseInstance().create(player.getUniqueId(), player.getName(), player.getInventory().getItemInMainHand());
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                break;
            case "test":
                break;
            default:
                break;
        }

        return true;
    }
}
