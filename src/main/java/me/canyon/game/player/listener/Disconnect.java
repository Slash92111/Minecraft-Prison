package me.canyon.game.player.listener;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Staff;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.Date;

public class Disconnect implements Listener {

    private Main plugin;

    public Disconnect(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PlayerData playerData = plugin.getPlayerData(player);

        //Update the server stats
        Document document = plugin.getMongoDBInstance().getDocument("stats", "STATS", true);

        int playersOnline = Bukkit.getOnlinePlayers().size() - 1;
        int staffOnline = document.getInteger("STAFF_ONLINE");

        if (plugin.getPlayerData(player).getStaff() != Staff.NONE) staffOnline -= 1;

        Bson newValues = new Document("STATS", true)
                .append("PLAYERS_ONLINE", playersOnline)
                .append("STAFF_ONLINE", staffOnline);
        Bson updateDocument = new Document("$set", newValues);

        plugin.getMongoDBInstance().getColletion("stats").updateMany(document, updateDocument);

        //Update play time
        long playTime = playerData.getPlayTime();
        Date lastLogin = playerData.getLastLogin();
        Date currentTime = new Date();

        playerData.setPlayTime(((currentTime.getTime() - lastLogin.getTime()) / 1000) + playTime); //gets difference between last login and right now in seconds, adds it to the total and sets it

        plugin.getPlayerData(player).upload();

        plugin.removePlayerData(player);
    }
}
