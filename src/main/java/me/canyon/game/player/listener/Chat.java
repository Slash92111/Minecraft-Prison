package me.canyon.game.player.listener;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Rank;
import me.canyon.game.player.rank.Staff;
import me.canyon.game.player.toggle.ToggleEnum;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Chat implements Listener {

    private Main plugin;

    public Chat(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void publicChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!plugin.hasPlayerData(player))
            plugin.setPlayerData(player);

        PlayerData playerData = plugin.getPlayerData(player);

        //Check if they're muted
        boolean muted = playerData.getMute() != null;

        if (playerData.getMute() != null) {
            Date expires = playerData.getMute();

            if (!expires.before(new Date())) {
                String datePattern = "MM-dd-yyyy HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
                String date = simpleDateFormat.format(expires);

                String reason = playerData.getMuteReason();

                event.getPlayer().sendMessage(plugin.getMessageFromConfig("mute.currently", Map.of("%reason%", reason, "%expires%", date)));

                plugin.getSplunkInstance().log("chat", "Public", "Sender=" + player.getUniqueId().toString() + " | Message=" + event.getMessage() + " | Muted=" + muted);

                return;
            } else
                playerData.setMute(null, "");
        }

        Rank rank = plugin.getPlayerData(player).getRank();
        Staff staff = plugin.getPlayerData(player).getStaff();

        String prefix = "";
        ChatColor color = ChatColor.WHITE;

        if (staff != Staff.NONE) {
            color = staff.getColor();
            prefix = color + "[" + color + ChatColor.translateAlternateColorCodes('&', staff.getPrefix()) + color + "] ";
        } else if (rank != Rank.NONE) {
            color = rank.getColor();
            prefix = color + "[" + color + ChatColor.translateAlternateColorCodes('&', rank.getPrefix()) + color + "] ";
        }

        String nickname = color + playerData.getNickname();

        if (!nickname.equals(playerData.getName()))
            nickname = ChatColor.ITALIC + nickname;

        for (Player players : Bukkit.getOnlinePlayers()) {
            BaseComponent[] hoverText = new ComponentBuilder(playerData.getInformation()).create();
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/test " + player.getName());
            BaseComponent[] name = new ComponentBuilder(nickname).event(hoverEvent).event(clickEvent).create();
            BaseComponent[] message = new ComponentBuilder(color + "").reset().append(ChatColor.WHITE + ": " + "" + event.getMessage()).reset().create();
            BaseComponent[] builtMessage = new ComponentBuilder(prefix).append(name).append(message).reset().create();

            if (plugin.getPlayerData(players).getToggle().get(ToggleEnum.PUBLIC_CHAT))
                players.spigot().sendMessage(builtMessage);
        }

        plugin.getSplunkInstance().log("chat", "Public", "Sender=" + player.getUniqueId().toString() + " | Message=" + event.getMessage() + " | Muted=" + muted);
    }
}
