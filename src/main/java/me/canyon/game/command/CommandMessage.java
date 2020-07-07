package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.toggle.ToggleEnum;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class CommandMessage extends BaseCommand {

    private Main plugin = Main.getInstance();

    public CommandMessage(String command, String usage, String description, List<String> aliases) {
        super(command, usage, description, aliases);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        if (args.length < 2)
            return false;

        Player player = (Player) sender;

        PlayerData playerData = plugin.getPlayerData(player);

        String message = String.join(" ", Arrays.asList(args));
        message = message.substring(args[0].length() + 1);

        boolean isMuted = playerData.getMute() != null;

        if (Bukkit.getPlayer(args[0]) != null)
            plugin.getSplunkInstance().log("chat", "Private", "Sender=" + player.getUniqueId().toString() + " | Recipient=" + Bukkit.getPlayer(args[0]).getUniqueId().toString() + " | Message=" + message + " | Muted=" + isMuted);

        if (isMuted) {
            Date expires = playerData.getMute();

            String datePattern = "MM-dd-yyyy HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
            String date = simpleDateFormat.format(expires);
            String reason = playerData.getMuteReason();

            player.sendMessage(plugin.getMessageFromConfig("mute.currently", Map.of("%reason%", reason, "%expires%", date)));


            return false;
        }

        String playerName = args[0];

        try {
            if (Bukkit.getPlayer(playerName) != null && Bukkit.getPlayer(playerName).isOnline()) {

                PlayerData otherPlayerData = plugin.getPlayerData(Bukkit.getPlayer(playerName));

                if (otherPlayerData.getToggle().get(ToggleEnum.PRIVATE_MESSAGE)) {
                    Bukkit.getPlayer(playerName).sendMessage(plugin.getMessageFromConfig("privateMessage.from", Map.of("%player_name%", playerName, "%message%", message)));
                    player.sendMessage(plugin.getMessageFromConfig("privateMessage.send", Map.of("%player_name%", player.getName(), "%message%", message)));
                } else
                    player.sendMessage(plugin.getMessageFromConfig("privateMessage.doNotDisturb", Map.of("%player_name%", playerName)));

            } else {
                player.sendMessage(plugin.getMessageFromConfig("other.playerIsntOnline", Map.of("%player_name%", playerName)));
                return false;
            }
        } catch (NullPointerException ex) {
            player.sendMessage(plugin.getMessageFromConfig("other.playerIsntOnline", Map.of("%player_name%", playerName)));
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().toLowerCase().matches("message|pm|w|whisper") && args.length <= 1) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return players;
        }

        return null;
    }
}
