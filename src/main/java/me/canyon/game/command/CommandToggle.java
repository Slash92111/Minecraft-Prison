package me.canyon.game.command;

import me.canyon.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandToggle extends BaseCommand {

    public CommandToggle(String command, String usage, String description) {
        super(command, usage, description);
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        player.openInventory(plugin.getPlayerData(player).getToggle().getUI());
        return true;
    }
}
