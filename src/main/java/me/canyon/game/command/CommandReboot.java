package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.gang.Gang;
import me.canyon.game.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ConcurrentModificationException;
import java.util.List;

public class CommandReboot extends BaseCommand {

    private int staffLevel;

    public CommandReboot(String command, String usage, String description, List<String> aliases, int staffLevel) {
        super(command, usage, description, aliases);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (plugin.getPlayerData(player).getStaff().getOrder() < staffLevel)
            return false;

        try {
            for (PlayerData playerData : plugin.getAllPlayerData().values()) { //Upload all currently loaded player data to the MongoDB
                playerData.upload();
                Player players = Bukkit.getPlayer(playerData.getUUID());

                plugin.getAllPlayerData().remove(playerData.getUUID());

                if (players != null) player.kickPlayer(plugin.getMessageFromConfig("command.reboot"));
            }
        } catch (ConcurrentModificationException ex) {
            //TODO
        }


        for (Gang gangData : plugin.getAllGangData().values())
            gangData.upload();

        Bukkit.getServer().shutdown();

        return true;
    }
}
