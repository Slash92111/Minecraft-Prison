package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.gang.Gang;
import me.canyon.game.player.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBackup extends BaseCommand {

    private int staffLevel;

    public CommandBackup(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (plugin.getPlayerData(player).getStaff().getOrder() < staffLevel)
            return false;

        for (PlayerData playerData : plugin.getAllPlayerData().values())
            playerData.upload();

        for (Gang gang : plugin.getAllGangData().values())
            gang.upload();

        player.sendMessage(plugin.getMessageFromConfig("database.successfulBackup"));

        return true;
    }
}
