package me.canyon.game.player.listener;

import com.mojang.authlib.GameProfile;
import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Staff;
import me.canyon.game.player.tablist.TabList;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.server.v1_15_R1.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Scoreboard;

import java.text.SimpleDateFormat;
import java.util.*;

public class Join implements Listener {

    private Main plugin;

    public Join(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void checkTempBan(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (plugin.getMongoDBInstance().getDocument("UUID", player.getUniqueId().toString()) != null) { //Only check if it's not first time joining
            Document document = plugin.getMongoDBInstance().getDocument("UUID", player.getUniqueId().toString());

            Date expires = document.getDate("TEMP_BAN");
            boolean permBanned = document.getBoolean("PERM_BAN");

            if (expires != null) {
                String datePattern = "MM-dd-yyyy HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
                String date = simpleDateFormat.format(expires);

                String reason = document.getString("TEMP_BAN_REASON");
                String banByWho = document.getString("TEMP_BAN_BY_WHO");

                if (!expires.before(new Date()))
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', plugin.getMessageConfig().getString("tempBanJoin").replace("%d", date).replace("%r", reason).replace("%w", banByWho)));
                else {
                    Bson newValues = new Document("TEMP_BAN", null)
                            .append("TEMP_BAN_REASON", "")
                            .append("TEMP_BAN_BY_WHO", "");
                    Bson updateDocument = new Document("$set", newValues);

                    plugin.getMongoDBInstance().getColletion().updateMany(document, updateDocument);
                }
            } else if (permBanned) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', plugin.getMessageConfig().getString("permBanJoin").replace("%r", document.getString("PERM_BAN_REASON")).replace("%w", document.getString("PERM_BAN_BY_WHO"))));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerData playerData = plugin.getPlayerData(player);

        if (playerData.getGangID() != 0)
            playerData.getGang().sendLoginAlert(plugin.getMessageFromConfig("gang.loginAlert", Map.of("%player_name%", playerData.getName())));

        Document document = plugin.getMongoDBInstance().getDocument("stats", "STATS", true);

        // Update the player count in the database for the discord bot
        int playersOnline = Bukkit.getOnlinePlayers().size();
        int staffOnline = document.getInteger("STAFF_ONLINE");

        if (plugin.getPlayerData(player).getStaff() != Staff.NONE) staffOnline += 1;

        Bson newValues = new Document("STATS", true)
                .append("PLAYERS_ONLINE", playersOnline)
                .append("STAFF_ONLINE", staffOnline);
        Bson updateDocument = new Document("$set", newValues);

        plugin.getMongoDBInstance().getColletion("stats").updateMany(document, updateDocument);

        playerData.setTabList(new TabList(player));
    }


}
