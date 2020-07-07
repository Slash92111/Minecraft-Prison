package me.canyon.game.player.tablist;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.canyon.Main;
import me.canyon.game.gang.Gang;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Rank;
import me.canyon.game.player.rank.Staff;
import me.canyon.util.Utilities;
import net.md_5.bungee.chat.SelectorComponentSerializer;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.util.*;

public class TabList {

    private Main plugin = Main.getInstance();
    private Player player;
    private PlayerData playerData;

    private HashMap<Integer, EntityPlayer> fakePlayers = new HashMap<>();

    public TabList(Player player) {
        this.player = player;
        this.playerData = plugin.getPlayerData(player);

        set();

        //Update the player tab list every second
        new BukkitRunnable() {
            public void run() {
                if (player.isOnline())
                    update();
                else
                    this.cancel();
            }
        }.runTaskTimer(this.plugin, 0, 20);
    }

    private List<String> getColumn(int column) {
        List<String> list = new ArrayList<>();
        switch(column) {
            case 1: //Gang stuff
                list.add(Utilities.getCenteredTabListText(ChatColor.GREEN + "" + ChatColor.BOLD + "GANG"));
                list.add (" ");

                if (playerData.getGangID() != 0) {
                    Gang gang = playerData.getGang();
                    HashMap<String, String> allMembers = gang.getAllMembers();

                    if (gang.getLeader() != null) {
                        ChatColor color = ChatColor.GREEN;

                        try {
                            if (!Bukkit.getPlayer(gang.getLeader()).isOnline())
                                color = ChatColor.GRAY;

                        } catch (NullPointerException ex) {
                            color = ChatColor.GRAY;
                        }

                        list.add(color + " **" + allMembers.get(gang.getLeader().toString()));
                    }

                    for (String id : gang.getOfficers()) {
                        ChatColor color = ChatColor.GREEN;

                        try {
                            if (!Bukkit.getPlayer(UUID.fromString(id)).isOnline())
                                color = ChatColor.GRAY;

                        } catch (NullPointerException ex) {
                            color = ChatColor.GRAY;
                        }

                        list.add(color + " *" + allMembers.get(id));
                    }

                    for (String id : gang.getMembers()) {
                        ChatColor color = ChatColor.GREEN;

                        try {
                            if (!Bukkit.getPlayer(UUID.fromString(id)).isOnline())
                                color = ChatColor.GRAY;

                        } catch (NullPointerException ex) {
                            color = ChatColor.GRAY;
                        }

                        list.add(color  + " " + allMembers.get(id));
                    }
                } else {
                    list.add(ChatColor.GRAY + " Use " + ChatColor.GREEN + "/gang create <name> <description>");
                    list.add(ChatColor.GRAY + " to create your own gang!");
                }

                break;
            case 2:
                list.add(Utilities.getCenteredTabListText(ChatColor.AQUA + "" + ChatColor.BOLD + "FRIENDS"));
                list.add(" ");

                if (playerData.getFriends().size() > 0) {
                    HashMap<String, String> friends = playerData.getFriends();

                        ChatColor color = ChatColor.GREEN;

                        for (String id : friends.keySet()) {
                            try {
                                if (!Bukkit.getPlayer(friends.get(id)).isOnline())
                                    color = ChatColor.GRAY;

                            } catch (NullPointerException ex) {
                                color = ChatColor.GRAY;
                            }

                            list.add(color + " " + friends.get(id));
                        }
                } else {
                    list.add(ChatColor.GRAY + " Type " + ChatColor.AQUA + "/friend add <name>" + ChatColor.GRAY + " to");
                    list.add(ChatColor.GRAY + " add someone to your friends list!");
                }
                break; //Friends
            case 3:
                list.add(Utilities.getCenteredTabListText(ChatColor.YELLOW + "" + ChatColor.BOLD + "STATS"));
                list.add(" ");
                list.add(ChatColor.GRAY + " Level: " + ChatColor.WHITE + playerData.getLevel());
                list.add(" ");
                list.add(ChatColor.GRAY + " XP: " + ChatColor.WHITE + playerData.getExperience());

                if (playerData.getGangID() != 0) {
                    list.add(" ");
                    list.add(ChatColor.GRAY + " Gang: " + ChatColor.WHITE + playerData.getGang().getName());
                }

                if (playerData.getRank() != Rank.NONE) {
                    list.add(" ");
                    list.add(ChatColor.GRAY + " Rank: " + ChatColor.WHITE + playerData.getRank().getColor() + playerData.getRank().getPrefix());
                }

                if (playerData.getStaff() != Staff.NONE) {
                    list.add(" ");
                    list.add(ChatColor.GRAY + " Staff: " + ChatColor.RED + playerData.getStaff().getPrefix());
                }

                list.add(" ");
                list.add(ChatColor.GRAY + " Balance: " + ChatColor.GREEN + "$" + NumberFormat.getNumberInstance(Locale.US).format(playerData.getBalance()));
                list.add(" ");
                list.add(ChatColor.GRAY + " Play Time: " + ChatColor.WHITE + playerData.getPlayTimeFormatted());

                if (playerData.getPrestige() > 0) {
                    list.add(" ");
                    list.add(ChatColor.GRAY + " Prestige : " + ChatColor.AQUA + Utilities.integerToRoman(Integer.parseInt(playerData.getPrestige() + "")));
                }

                break; //Player info
            case 4:
                list.add(Utilities.getCenteredTabListText(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "LEVEL"));
                list.add(" ");
                list.add(ChatColor.GRAY + " " + (playerData.getExperience() + " ") + "(" + ChatColor.GREEN + "0%" + ChatColor.GRAY + ") to " + ChatColor.WHITE + "2");
                break;
        }

        if (list.size() < 20)
            while (list.size() < 20)
                list.add(" ");

        return list;
    }

    public void set() {
        clear();

        WorldServer world = ((CraftWorld) Bukkit.getWorld("world")).getHandle();
        MinecraftServer server = ((CraftServer) plugin.getServer()).getServer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            int i = 0;
            for (String string : getColumn(1)) {
                String slot = "";

                if (i < 10)
                    slot = "0";

                GameProfile gameProfile = new GameProfile(UUID.randomUUID(), slot + i);
                EntityPlayer fakePlayer = new EntityPlayer(server, world, gameProfile, new PlayerInteractManager(world));

                fakePlayer.ping = 100000000;

                String skin = plugin.getSettingsConfig().getString("graySkinTexture");
                String signature = plugin.getSettingsConfig().getString("graySkinSignature");
                gameProfile.getProperties().put("textures", new Property("textures", skin, signature));

                fakePlayer.listName = new ChatMessage(string);

                fakePlayers.put(i, fakePlayer);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fakePlayer);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                i++;
            }

            for (String string : getColumn(2)) {
                GameProfile gameProfile = new GameProfile(UUID.randomUUID(), i + "");
                EntityPlayer fakePlayer = new EntityPlayer(server, world, gameProfile, new PlayerInteractManager(world));

                fakePlayer.ping = 100000000;

                String skin = plugin.getSettingsConfig().getString("graySkinTexture");
                String signature = plugin.getSettingsConfig().getString("graySkinSignature");
                gameProfile.getProperties().put("textures", new Property("textures", skin, signature));

                fakePlayer.listName = new ChatMessage(string);

                fakePlayers.put(i, fakePlayer);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fakePlayer);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                i++;
            }

            for (String string : getColumn(3)) {
                GameProfile gameProfile = new GameProfile(UUID.randomUUID(), i + "");
                EntityPlayer fakePlayer = new EntityPlayer(server, world, gameProfile, new PlayerInteractManager(world));

                fakePlayer.ping = 100000000;

                String skin = plugin.getSettingsConfig().getString("graySkinTexture");
                String signature = plugin.getSettingsConfig().getString("graySkinSignature");
                gameProfile.getProperties().put("textures", new Property("textures", skin, signature));

                fakePlayer.listName = new ChatMessage(string);

                fakePlayers.put(i, fakePlayer);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fakePlayer);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                i++;
            }

            for (String string : getColumn(4)) {
                GameProfile gameProfile = new GameProfile(UUID.randomUUID(), i + "");
                EntityPlayer fakePlayer = new EntityPlayer(server, world, gameProfile, new PlayerInteractManager(world));

                fakePlayer.ping = 100000000;

                String skin = plugin.getSettingsConfig().getString("graySkinTexture");
                String signature = plugin.getSettingsConfig().getString("graySkinSignature");
                gameProfile.getProperties().put("textures", new Property("textures", skin, signature));

                fakePlayer.listName = new ChatMessage(string);

                fakePlayers.put(i, fakePlayer);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fakePlayer);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                i++;
            }
        }, 10);
    }

    public void update() {
        int i = 0;
        for (String string : getColumn(1)) {
            if (fakePlayers.get(i) != null) {
                EntityPlayer fakePlayer = fakePlayers.get(i);
                fakePlayers.get(i).listName = new ChatMessage(string);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, fakePlayer);
                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
            }

            i++;
        }

        for (String string : getColumn(2)) {
            if (fakePlayers.get(i) != null) {
                EntityPlayer fakePlayer = fakePlayers.get(i);
                fakePlayers.get(i).listName = new ChatMessage(string);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, fakePlayer);
                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
            }

            i++;
        }

        for (String string : getColumn(3)) {
            if (fakePlayers.get(i) != null) {
                EntityPlayer fakePlayer = fakePlayers.get(i);
                fakePlayers.get(i).listName = new ChatMessage(string);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, fakePlayer);
                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
            }

            i++;
        }

        for (String string : getColumn(4)) {
            if (fakePlayers.get(i) != null) {
                EntityPlayer fakePlayer = fakePlayers.get(i);
                fakePlayers.get(i).listName = new ChatMessage(string);

                PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, fakePlayer);
                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
            }

            i++;
        }
    }

    private void clear() {
        for (Player players : Bukkit.getOnlinePlayers()) {
            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer) players).getHandle());
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
        }
    }
}
