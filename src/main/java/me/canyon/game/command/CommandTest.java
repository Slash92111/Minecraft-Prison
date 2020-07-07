package me.canyon.game.command;

import me.canyon.Main;

import me.canyon.database.MongoDB;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Staff;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandTest extends BaseCommand {

    public CommandTest(String command) {
        super(command);
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length < 1)
            return false;

        PlayerData playerData = plugin.getPlayerData(player);

        if (playerData.getStaff() == Staff.NONE)
            return false;

        Player target = Bukkit.getPlayer(args[0]);

        MongoDB mongoDB = plugin.getMongoDBInstance();
        Document document = mongoDB.getDocument("IGN", args[0]);

        if (document != null) {
            UUID uuid;

            if (target != null) {
                if (!plugin.hasPlayerData(target))
                    plugin.setPlayerData(target, true);

                uuid = target.getUniqueId();
            } else {
                uuid = UUID.fromString(document.getString("UUID"));
                if (!plugin.hasPlayerData(uuid))
                    plugin.setPlayerData(uuid, true);
            }

            player.openInventory(plugin.getStaffUI().getMainPage(plugin.getPlayerData(uuid)));
        }

        /*
        new BukkitRunnable() {
            Location loc = player.getLocation();
            double t = 0;
            double r = 1;

            public void run() {
                t = t + Math.PI / 16;
                double x = r * Math.cos(t);
                double y = 0.085 * t;
                double z = r * Math.sin(t);
                loc.add(x, y, z);
                player.getWorld().spawnParticle(Particle.valueOf(args[1].toUpperCase()), loc, 0, 0, 0, 0, 1);
                loc.subtract(x, y, z);
                if (t > Math.PI * 9) {
                    this.cancel();

                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 1);
        */

        return true;
    }
}
