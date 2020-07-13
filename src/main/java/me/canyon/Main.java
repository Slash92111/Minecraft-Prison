package me.canyon;

import me.canyon.database.MongoDB;
import me.canyon.database.Splunk;
import me.canyon.database.YML;
import me.canyon.game.World;
import me.canyon.game.auction.AuctionHouse;
import me.canyon.game.auction.AuctionListener;
import me.canyon.game.command.*;
import me.canyon.game.gang.Gang;
import me.canyon.game.command.CommandGang;
import me.canyon.game.command.CommandBackpack;
import me.canyon.game.gang.vault.ListenerVault;
import me.canyon.game.item.ItemListener;
import me.canyon.game.item.kit.Kit;
import me.canyon.game.item.kit.KitListener;
import me.canyon.game.player.listener.Attack;
import me.canyon.game.player.listener.Chat;
import me.canyon.game.player.backpack.ListenerBackpack;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.listener.Disconnect;
import me.canyon.game.player.listener.Join;
import me.canyon.game.player.rank.Rank;
import me.canyon.game.player.rank.Staff;
import me.canyon.game.command.CommandRank;
import me.canyon.game.command.CommandRanks;
import me.canyon.game.player.toggle.ToggleListener;
import me.canyon.game.staff.StaffListener;
import me.canyon.game.staff.StaffUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin {

    private static Main instance;

    //Config files
    private FileConfiguration playerConfig;
    private FileConfiguration messageConfig;
    private FileConfiguration settingsConfig;
    private FileConfiguration databaseConfig;
    private FileConfiguration permissionsConfig;
    private FileConfiguration itemsConfig;
    private FileConfiguration kitsConfig;
    private FileConfiguration punishmentConfig;
    private FileConfiguration defaultsConfig;

    private HashMap<UUID, PlayerData> playerData = new HashMap<>();
    private HashMap<Integer, Gang> gangData = new HashMap<>();

    private MongoDB mongoDB;
    private Splunk splunk;
    private Kit kit;
    private AuctionHouse auctionHouse;

    private ListenerVault listenerVault;
    private ListenerBackpack listenerBackpack;
    private AuctionListener auctionListener;
    private StaffUI staffUI;

    public void onEnable() {
        instance = this;

        playerConfig = new YML(this,"PlayerDefaults").getConfig();
        messageConfig = new YML(this, "messages").getConfig();
        settingsConfig = new YML(this, "settings").getConfig();
        databaseConfig = new YML(this, "database").getConfig();
        permissionsConfig = new YML(this, "permissions").getConfig();
        itemsConfig = new YML(this, "items").getConfig();
        kitsConfig = new YML(this, "kits").getConfig();
        punishmentConfig = new YML(this, "punishment").getConfig();
        defaultsConfig = new YML(this, "defaults").getConfig();

        mongoDB = new MongoDB(this);
        splunk = new Splunk(this);
        kit = new Kit(this);

        listenerVault = new ListenerVault(this);
        listenerBackpack = new ListenerBackpack(this);
        auctionListener = new AuctionListener(this);
        staffUI = new StaffUI(this);

        auctionHouse = new AuctionHouse(this);

        CommandManager commandManager = new CommandManager();

        commandManager.registerCommand(new CommandReloadPlayer("reloadplayer", "/<command> [args]", "Reloads a players local data", Arrays.asList("rp", "reloadp", "rplayer"), permissionsConfig.getInt("staff.reloadplayer")));
        commandManager.registerCommand(new CommandMessage("message", "/<command>", "Messages a player", Arrays.asList("msg", "tell", "t", "whisper", "w", "m")));
        commandManager.registerCommand(new CommandTempBan("tempban", "/<command> [args]", "Temp bans a player", Arrays.asList("tban", "tempb"), permissionsConfig.getInt("staff.tempban")));
        commandManager.registerCommand(new CommandUnban("unban", "/<command> [args]", "Unbans a player that was previously banned", permissionsConfig.getInt("staff.unban")));
        commandManager.registerCommand(new CommandMute("mute", "/<command> [args]", "Temp mutes a player", permissionsConfig.getInt("staff.mute")));
        commandManager.registerCommand(new CommandUnmute("unmute", "/<command> [args]", "Unmutes a player that was previously muted", permissionsConfig.getInt("staff.unmute")));
        commandManager.registerCommand(new CommandRank("rank", "/<command> [args]", "Manage a players rank", permissionsConfig.getInt("staff.rank")));
        commandManager.registerCommand(new CommandBan("ban", "/<command> [args]", "Perm bans a player", permissionsConfig.getInt("staff.ban")));
        commandManager.registerCommand(new CommandUpload("upload", "/<command> [args]", "Uploads a players data", permissionsConfig.getInt("staff.upload")));
        commandManager.registerCommand(new CommandBackpack("backpack", "/<command> [args]", "Opens backpack", permissionsConfig.getInt("staff.backpack")));
        commandManager.registerCommand(new CommandReport("report", "/<command> [args]", "Report a player or bugs"));
        commandManager.registerCommand(new CommandGang("gang", "/<command> [args]", "Gang stuff", Arrays.asList("g", "gan"), permissionsConfig.getInt("staff.vault")));
        commandManager.registerCommand(new CommandDiscord("discord", "/command [args]", "Link discord"));
        commandManager.registerCommand(new CommandHome("home", "/command [args]", "Homes"));
        commandManager.registerCommand(new CommandBackup("backup", "/command", "Backsup all local data", permissionsConfig.getInt("staff.backup")));
        commandManager.registerCommand(new CommandTest("test"));
        commandManager.registerCommand(new CommandReboot("stop", "/command", "Shuts the server down", Collections.singletonList("reboot"), permissionsConfig.getInt("staff.stop")));
        commandManager.registerCommand(new CommandItem("item", "/command", "Create item specified", permissionsConfig.getInt("staff.item")));
        commandManager.registerCommand(new CommandRankKit("rankkit", "/command", "Opens the rank kit UI"));
        commandManager.registerCommand(new CommandGameKit("gamekit", "/command", "Opens the game kit UI"));
        commandManager.registerCommand(new CommandToggle("toggle", "/command", "Opens the toggle UI"));
        commandManager.registerCommand(new CommandFriend("friend", "/command", "Manage friends list"));
        commandManager.registerCommand(new CommandPay("pay", "/pay PLAYER AMOUNT", "Send another player money in-game."));
        commandManager.registerCommand(new CommandAuction("auction", "/auction", "Opens AH"));

        for (Rank rank : Rank.values())
            commandManager.registerCommand(new CommandRanks(rank.toString().toLowerCase(), "/<command> [args]", "Manage rank data", 3));

        for (Staff staff : Staff.values())
            commandManager.registerCommand(new CommandRanks(staff.toString().toLowerCase(), "/<command> [args]", "Manage staff rank data", 3));

        getServer().getPluginManager().registerEvents(new Join(this), this);
        getServer().getPluginManager().registerEvents(new Disconnect(this), this);
        getServer().getPluginManager().registerEvents(new Chat(this), this);
        getServer().getPluginManager().registerEvents(listenerBackpack, this);
        getServer().getPluginManager().registerEvents(new Attack(this), this);
        getServer().getPluginManager().registerEvents(listenerVault, this);
        getServer().getPluginManager().registerEvents(new World(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new KitListener(this), this);
        getServer().getPluginManager().registerEvents(new ToggleListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffListener(this), this);
        getServer().getPluginManager().registerEvents(auctionListener, this);
    }

    public void onDisable() {

    }

    public Gang getGangData(int ID) {
        if (gangData.containsKey(ID))
            return gangData.get(ID);

        return null;
    }

    public void setGangData(int ID, Gang gang) {
        if (gangData.containsKey(ID)) {
            gangData.get(ID).upload();
            gangData.replace(ID, gang);
        } else
            gangData.put(ID, gang);
    }

    public boolean hasGangData(int ID) {
        return gangData.containsKey(ID);
    }

    public HashMap<Integer, Gang> getAllGangData() { return this.gangData; }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (!playerData.containsKey(uuid))
            setPlayerData(uuid);

        return playerData.get(uuid);
    }

    public HashMap<UUID, PlayerData> getAllPlayerData() { return this.playerData; }

    public void setPlayerData(Player player) {
        setPlayerData(player, false);
    }

    public void setPlayerData(Player player, boolean temp) {
        setPlayerData(player.getUniqueId(), temp);
    }

    public void setPlayerData(UUID uuid) {
        setPlayerData(uuid, false);
    }

    public void setPlayerData(UUID uuid, boolean temp) {
        if (!this.playerData.containsKey(uuid))
            this.playerData.put(uuid, new PlayerData(uuid, temp));
        else {
            this.playerData.get(uuid).upload();
            this.playerData.replace(uuid, new PlayerData(uuid, temp));
        }

        if (temp) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                Player player = Bukkit.getPlayer(uuid);

                if (player == null || !player.isOnline())
                    if (this.playerData.containsKey(uuid)) {
                        getPlayerData(uuid).upload();
                        removePlayerData(uuid);
                        return;
                    }
            }, (settingsConfig.getInt("playerDataTimeout") * 60) * 20); //(minutes * 60 [seconds]) * 20 [ticks]
        }
    }

    public void removePlayerData(Player player) {
        this.playerData.remove(player.getUniqueId());
    }

    public void removePlayerData(UUID uuid) {
        this.playerData.remove(uuid);
    }

    public boolean hasPlayerData(Player player) {
        return this.playerData.containsKey(player.getUniqueId());
    }

    public boolean hasPlayerData(UUID uuid) {
        return this.playerData.containsKey(uuid);
    }

    public FileConfiguration getPlayerConfig() {
        return playerConfig;
    }

    public FileConfiguration getMessageConfig() {
        return messageConfig;
    }

    public FileConfiguration getSettingsConfig() {
        return settingsConfig;
    }

    public FileConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    public FileConfiguration getPermissionsConfig() { return permissionsConfig; }

    public FileConfiguration getItemsConfig() { return itemsConfig; }

    public FileConfiguration getKitsConfig() { return kitsConfig; }

    public FileConfiguration getPunishmentConfig() { return punishmentConfig; }

    public FileConfiguration getDefaultsConfig() { return defaultsConfig; }

    public void debug(String string) {
        getServer().getConsoleSender().sendMessage(string);
    }

    public static Main getInstance() {
        return instance;
    }

    public MongoDB getMongoDBInstance() {
        return mongoDB;
    }

    public Splunk getSplunkInstance() {
        return splunk;
    }

    public Kit getKitInstance() { return kit; }

    public AuctionHouse getAuctionHouseInstance() { return auctionHouse; }

    public ListenerVault getListenerVaultInstance() { return listenerVault; }

    public ListenerBackpack getListenerBackpackInstance() { return listenerBackpack; }

    public AuctionListener getAuctionListenerInstance() { return auctionListener; }

    public StaffUI getStaffUI() { return staffUI; }

    public String getMessageFromConfig(String id) {
        return getMessageFromConfig(id, new HashMap<>());
    }

    //Example plugin.getMessageFromConfig("correctUsage", Map.of("%correct_usage%", "/item <item>"))
    public String getMessageFromConfig(String id, Map<String, String> variables) {
        String message = this.messageConfig.getString(id);

        if (message == null)
            return "null";

        message = ChatColor.translateAlternateColorCodes('&', message);

        for (String variable : variables.keySet())
            if (message.contains(variable))
                message = message.replace(variable, variables.get(variable));

        return message;
    }
}
