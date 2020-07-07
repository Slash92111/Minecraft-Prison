package me.canyon.game.command;

import me.canyon.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandReloadPlayer extends BaseCommand {

    private int staffLevel;

    public CommandReloadPlayer(String command, String usage, String description, List<String> aliases, int staffLevel) {
        super(command, usage, description, aliases);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length < 1) {
            plugin.removePlayerData(player);
            plugin.setPlayerData(player);
            player.sendMessage(plugin.getMessageFromConfig("player.reloadedData"));
        } else {
            if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
                return false;

            Player otherPlayer = Bukkit.getPlayer(args[0]);

            if (otherPlayer != null && otherPlayer.isOnline()) {
                plugin.removePlayerData(otherPlayer);
                plugin.setPlayerData(otherPlayer);
                otherPlayer.sendMessage(plugin.getMessageFromConfig("player.reloadedData"));
                player.sendMessage(plugin.getMessageFromConfig("player.reloadedData", Map.of("%player_name%", otherPlayer.getName())));

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=" + cmd.getName().toLowerCase() + " | Target=" + otherPlayer.getUniqueId().toString());
            } else
                player.sendMessage(plugin.getMessageFromConfig("player.playerIsntOnline", Map.of("%player_name%", args[0])));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player cmdSender = (Player) sender;

        if (!(plugin.getPlayerData(cmdSender).getStaff().getOrder() >= staffLevel))
            return null;

        if (cmd.getName().toLowerCase().matches("reloadplayer") && args.length <= 1) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> players.add(onlinePlayer.getName()));
            return players;
        }

        return null;
    }
}
