package me.canyon.game.player;

import me.canyon.database.MongoDB;
import me.canyon.Main;
import me.canyon.game.gang.Gang;
import me.canyon.game.player.backpack.Backpack;
import me.canyon.game.player.rank.Rank;
import me.canyon.game.player.rank.Staff;
import me.canyon.game.player.tablist.TabList;
import me.canyon.game.player.toggle.Toggle;
import me.canyon.util.Utilities;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;

public class PlayerData {

    private Main plugin = Main.getInstance();
    private Player player;
    private UUID id;
    private String ign, nickname, ip, tempBanReason, tempBanByWho, muteReason, permBanReason, permBanByWho, discordID;
    private Long balance, xp, level, prestige, auctionExpiration;
    private long playTime;
    private Date lastLogin, ban, mute;
    private Rank rank;
    private Staff staff;
    private boolean permBan;
    private int totalBackpackRows, maxBackpackRows, hardcapBackpackRows, gangID, maxHomes, totalHomes, totalAuctions, maxAuctions;
    private Backpack backpack;
    private List<String> backpackList, toggleList;
    private List<Gang> gangInvites = new ArrayList<>(); //NOT saved to database
    private HashMap<String, Location> homes = new HashMap<>();
    private HashMap<String, Date> kitCooldowns = new HashMap<>();
    private HashMap<String, Integer> kitLevels = new HashMap<>();
    private HashMap<String, String> friends = new HashMap<>();
    private HashMap<String, String> tempStrings = new HashMap<>();
    private List<Player> pendingFriends = new ArrayList<>(); //NOT saved to database
    private Toggle toggle;
    private TabList tabList;

    public PlayerData(Player player, boolean temp) {
        this(player.getUniqueId(), temp);
    }

    public PlayerData(Player player) {
        this(player.getUniqueId(), false);
    }

    public PlayerData(UUID uuid, boolean temp) {
        this.player = Bukkit.getPlayer(uuid);
        this.id = uuid;

        verify();
        Document playerDocument = plugin.getMongoDBInstance().getDocument("UUID", this.id.toString());

        if (!temp && player != null && player.getAddress() != null) {
            this.ip = player.getAddress().toString().split(":")[0].split("/")[1]; //IP comes in like '/0.0.0.0:00000'
        } else
            this.ip = playerDocument.getString("IP");

        if (player != null)
            this.ign = player.getName();
        else
            this.ign = playerDocument.getString("IGN");

        this.nickname = playerDocument.getString("NICKNAME");
        this.balance = playerDocument.getLong("BALANCE");
        this.lastLogin = new Date();
        this.tempBanReason = playerDocument.getString("TEMP_BAN_REASON");
        this.tempBanByWho = playerDocument.getString("TEMP_BAN_BY_WHO");
        this.muteReason = playerDocument.getString("MUTE_REASON");
        this.playTime = playerDocument.getLong("PLAY_TIME");
        this.rank = Rank.valueOf(playerDocument.getString("RANK"));
        this.staff = Staff.valueOf(playerDocument.getString("STAFF"));
        this.permBan = playerDocument.getBoolean("PERM_BAN");
        this.permBanReason = playerDocument.getString("PERM_BAN_REASON");
        this.permBanByWho = playerDocument.getString("PERM_BAN_BY_WHO");
        this.xp = playerDocument.getLong("EXPERIENCE");
        this.level = playerDocument.getLong("LEVEL");
        this.prestige = playerDocument.getLong("PRESTIGE");
        this.totalBackpackRows = playerDocument.getInteger("BACKPACK_TOTAL_ROWS");
        this.maxBackpackRows = playerDocument.getInteger("BACKPACK_MAX_ROWS");
        this.hardcapBackpackRows = playerDocument.getInteger("BACKPACK_HARDCAP_ROWS");
        this.backpackList = playerDocument.getList("BACKPACK", String.class);
        this.backpack = new Backpack(this);
        this.discordID = playerDocument.getString("DISCORD_ID");
        this.gangID = playerDocument.getInteger("GANG_ID");
        this.maxHomes = playerDocument.getInteger("HOMES_MAX");
        this.totalHomes = playerDocument.getInteger("HOMES_TOTAL");
        this.toggleList = playerDocument.getList("TOGGLE", String.class);
        this.toggle = new Toggle(this);
        this.maxAuctions = playerDocument.getInteger("AUCTION_MAX");
        this.totalAuctions = playerDocument.getInteger("AUCTION_TOTAL");
        this.auctionExpiration = playerDocument.getLong("AUCTION_EXPIRATION");

        if (!plugin.hasGangData(this.gangID)) //load the gang if it's not already loaded
            new Gang(gangID);

        if (playerDocument.get("TEMP_BAN") != null)
            this.ban = playerDocument.getDate("TEMP_BAN");
        else
            this.ban = null;

        if (playerDocument.get("MUTE") != null)
            this.mute = playerDocument.getDate("MUTE");
        else
            this.mute = null;

        for (String friend : playerDocument.getList("FRIENDS", String.class)) {
            if (!friend.equals("")) {
                String ID = friend.split(":")[0];
                String IGN = friend.split(":")[1];

                this.friends.put(ID, IGN);
            }
        }

        for (String home : playerDocument.getList("HOMES", String.class)) {
            if (!home.equals("")) {
                String name = home.split(":")[0];
                String[] cords = home.split(":")[1].split(",");
                Location location = new Location(Bukkit.getWorld(cords[0]), Double.parseDouble(cords[1]), Double.parseDouble(cords[2]), Double.parseDouble(cords[3]));

                this.homes.put(name, location);
            }
        }

        for (String cooldown : playerDocument.getList("KIT_COOLDOWNS", String.class)) {
            if (!cooldown.equals("")) {
                String kitID = cooldown.split(":")[0];
                Date expires = new Date(Long.parseLong(cooldown.split(":")[1]));

                this.kitCooldowns.put(kitID, expires);
            }
        }

        for (String kit : playerDocument.getList("KIT_LEVELS", String.class)) {
            if (!kit.equals("")) {
                String kitID = kit.split(":")[0];
                int level = Integer.parseInt(kit.split(":")[1]);

                this.kitLevels.put(kitID, level);
            }
        }

        updateDisplayName();

        plugin.debug(ChatColor.GOLD + "Successfully loaded " + this.ign + " data!");
    }

    private void verify() {
       MongoDB mongoDB = plugin.getMongoDBInstance();

        plugin.debug(ChatColor.GOLD + "Verifying data...");

       if (mongoDB.getDocument("UUID", this.id.toString()) == null) {
           if (player != null && player.isOnline()) {
               plugin.debug(ChatColor.GOLD + "Player " + player.getName() + " data doesn't exist");
               FileConfiguration defaults = plugin.getPlayerConfig();
               Document document = new Document("UUID", this.id.toString());

               document.append("IGN", player.getName());
               document.append("IP", "");
               document.append("NICKNAME", player.getName());
               document.append("BALANCE", defaults.getLong("BALANCE"));
               document.append("LOCATION", defaults.getString("LOCATION"));
               document.append("FIRST_LOGIN", new Date());
               document.append("LAST_LOGIN", new Date());
               document.append("PERM_BAN", false);
               document.append("PERM_BAN_REASON", "");
               document.append("PERM_BAN_BY_WHO", "");
               document.append("TEMP_BAN", null);
               document.append("TEMP_BAN_REASON", "");
               document.append("TEMP_BAN_BY_WHO", "");
               document.append("MUTE", null);
               document.append("MUTE_REASON", "");
               document.append("PLAY_TIME", 0L);
               document.append("RANK", Rank.NONE.toString());
               document.append("STAFF", Staff.NONE.toString());
               document.append("EXPERIENCE", 0L);
               document.append("LEVEL", 1L);
               document.append("PRESTIGE", 0L);
               document.append("BACKPACK_TOTAL_ROWS", 1);
               document.append("BACKPACK_MAX_ROWS", 5);
               document.append("BACKPACK_HARDCAP_ROWS", 10);
               document.append("BACKPACK", new ArrayList<String>());
               document.append("DISCORD_ID", "");
               document.append("GANG_ID", 0);
               document.append("HOMES_MAX", 1);
               document.append("HOMES_TOTAL", 0);
               document.append("HOMES", new ArrayList<String>());
               document.append("KIT_COOLDOWNS", new ArrayList<String>());
               document.append("KIT_LEVELS", new ArrayList<String>());
               document.append("TOGGLE", new ArrayList<String>());
               document.append("FRIENDS", new ArrayList<String>());
               document.append("AUCTION_MAX", 1);
               document.append("AUCTION_TOTAL", 0);
               document.append("AUCTION_EXPIRATION", 28800000L); // 8 hours

               mongoDB.insertDocument(document);

               plugin.debug(ChatColor.GOLD + "Created player " + player.getName() + " data");
           }
       }
    }

    public void upload() {
        plugin.debug(ChatColor.GOLD + "Uploading " + this.ign + " data...");

        MongoDB mongoDB = plugin.getMongoDBInstance();

       Bson filter = new Document("UUID", this.id.toString());
       Document newValues = new Document("IGN", this.ign)
               .append("IP", this.ip)
               .append("NICKNAME", this.nickname)
               .append("BALANCE", this.balance)
               .append("LAST_LOGIN", this.lastLogin)
               .append("PERM_BAN", this.permBan)
               .append("PERM_BAN_REASON", this.permBanReason)
               .append("PERM_BAN_BY_WHO", this.permBanByWho)
               .append("TEMP_BAN", this.ban)
               .append("TEMP_BAN_REASON", this.tempBanReason)
               .append("TEMP_BAN_BY_WHO", this.tempBanByWho)
               .append("MUTE", this.mute)
               .append("MUTE_REASON", this.muteReason)
               .append("PLAY_TIME", this.playTime)
               .append("RANK", this.rank.toString())
               .append("STAFF", this.staff.toString())
               .append("EXPERIENCE", this.xp)
               .append("LEVEL", this.level)
               .append("PRESTIGE", this.prestige)
               .append("BACKPACK_TOTAL_ROWS", this.backpack.getTotalRows())
               .append("BACKPACK", this.backpack.toList())
               .append("BACKPACK_MAX_ROWS", this.maxBackpackRows)
               .append("BACKPACK_HARDCAP_ROWS", this.hardcapBackpackRows)
               .append("DISCORD_ID", this.discordID)
               .append("GANG_ID", this.gangID)
               .append("TOGGLE", this.toggle.toList());

       List<String> friends = new ArrayList<>();

       for (String friend : this.friends.keySet())
           friends.add(friend + ":" + this.friends.get(friend));

       newValues.append("FRIENDS", friends);

       List<String> homes = new ArrayList<>();

       for (String home : this.homes.keySet()) {
           Location location = this.homes.get(home);
           String cords = location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
           homes.add(home + ":" + cords);
       }

       newValues.append("HOMES", homes)
                .append("HOMES_TOTAL", totalHomes)
                .append("HOMES_MAX", maxHomes);

       List<String> cooldowns = new ArrayList<>();

       for (String kitID : this.kitCooldowns.keySet())
           cooldowns.add(kitID + ":" + this.kitCooldowns.get(kitID).getTime());

       newValues.append("KIT_COOLDOWNS", cooldowns);

       List<String> kitLevels = new ArrayList<>();

       for (String kitID : this.kitLevels.keySet())
           kitLevels.add(kitID + ":" + this.kitLevels.get(kitID));

       newValues.append("KIT_LEVELS", kitLevels);

       Bson updateDocument = new Document("$set", newValues);

       mongoDB.getColletion().updateMany(filter, updateDocument);

        plugin.debug(ChatColor.GOLD + "Successfully uploaded  " + this.ign + " data!");
    }

    public String getName() { return this.ign; }

    public Player getPlayer() { return this.player; }

    public void setTempBan(Date ban, String reason, String byWho) {
        setTempBan(ban);
        setTempBanReason(reason);
        setTempBanByWho(byWho);
    }

    public Date getTempBan() { return this.ban; }

    private void setTempBan(Date ban) { this.ban = ban; }

    public String getTempBanReason() { return this.tempBanReason; }

    private void setTempBanReason(String tempBanReason) { this.tempBanReason = tempBanReason; }

    public String getTempBanByWho() { return this.tempBanByWho; }

    private void setTempBanByWho(String tempBanByWho) { this.tempBanByWho = tempBanByWho; }

    public void setMute(Date mute, String reason) {
        setMute(mute);
        setMuteReason(reason);
    }

    public void setPermBan(boolean permBan, String permBanReason, String permBanByWho) {
        this.permBan = permBan;
        setPermBanReason(permBanReason);
        setPermBanByWho(permBanByWho);
    }

    public boolean getPermBan() { return this.permBan; }

    public String getPermBanReason() { return this.permBanReason; }

    private void setPermBanReason(String permBanReason) { this.permBanReason = permBanReason; }

    public String getPermBanByWho() { return this.permBanByWho; }

    private void setPermBanByWho(String permBanByWho) { this.permBanByWho = permBanByWho; }

    public Date getMute() { return this.mute; }

    private void setMute(Date mute) { this.mute = mute; }

    public String getMuteReason() { return this.muteReason; }

    private void setMuteReason(String muteReason) { this.muteReason = muteReason; }

    public Long getBalance() {
        return this.balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getNickname() { return this.nickname; }

    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPlayTimeFormatted() {
        long playTime = getPlayTime();
        Date lastLogin = getLastLogin();
        Date currentTime = new Date();

        long difference = (currentTime.getTime() - lastLogin.getTime()) / 1000;

        Duration duration = Duration.ofSeconds(playTime + difference);

        long days = duration.getSeconds() / 86400;
        long hours = (duration.getSeconds() % 86400) / 3600;
        long minutes= ((duration.getSeconds() % 86400) % 3600) / 60;
        long seconds = ((duration.getSeconds() % 86400) % 3600) % 60;

        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    public long getPlayTime() { return this.playTime; }

    public void setPlayTime(long playTime) { this.playTime = playTime; }

    public Date getLastLogin() { return this.lastLogin; }

    public Rank getRank() {
        return this.rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public Staff getStaff() { return this.staff; }

    public void setStaff(Staff staff) { this.staff = staff; }

    public UUID getUUID() {
        return this.id;
    }

    public long getExperience() { return this.xp; }

    public void setExperience(long xp) { this.xp = xp; }

    public long getLevel() { return this.level; }

    public void setLevel(long level) { this.level = level; }

    public long getPrestige() { return this.prestige; }

    public void setPrestige(long prestige) { this.prestige = prestige; }

    public String getInformation() {
        String information = "";

        ChatColor color = this.rank.getColor();

        if (this.staff != Staff.NONE)
            color = ChatColor.RED;

        information += color + this.ign;
        information +=  "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Level: " + ChatColor.WHITE + this.level;
        information += "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "XP: " + ChatColor.WHITE + NumberFormat.getNumberInstance(Locale.US).format(this.xp);

        if (this.gangID != 0)
            information += "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Gang: " + ChatColor.WHITE + getGang().getName();

        if (this.rank != Rank.NONE)
            information += "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Donator: " + this.rank.getColor() + this.rank.getPrefix();

        if (this.staff != Staff.NONE)
            information += "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Staff: " + ChatColor.RED + this.staff.getPrefix();

        information += "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Balance: " + ChatColor.GREEN + "$" + NumberFormat.getNumberInstance(Locale.US).format(this.balance);

        information +="\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Play Time: " + ChatColor.WHITE + getPlayTimeFormatted();

        if (this.prestige > 0)
            information += "\n" + color + "" + ChatColor.BOLD + "* " + ChatColor.RESET + ChatColor.GRAY + "Prestige: " + ChatColor.AQUA + Utilities.integerToRoman(Integer.parseInt(this.prestige + ""));

        return information;
    }

    public void updateDisplayName() {
        if (player == null)
            return;

        String gangName = "";

        if (this.gangID != 0)
            gangName = "[" + getGang().getName() + "] ";

        ChatColor color = this.rank.getColor();

        if (this.staff != Staff.NONE)
            color = ChatColor.RED;

        this.player.setCustomName(color + gangName + getName());
        this.player.setCustomNameVisible(true);
    }

    public int getTotalBackpackRows() {
        return this.totalBackpackRows;
    }

    public void setTotalBackpackRows(int totalBackpackRows) { this.totalBackpackRows = totalBackpackRows; }

    public Backpack getBackpack() {
        return this.backpack;
    }

    public int getMaxBackpackRows() { return this.maxBackpackRows; }

    public void setMaxBackpackRows(int maxBackpackRows) { this.maxBackpackRows = maxBackpackRows; }

    public int getHardcapBackpackRows() { return this.hardcapBackpackRows; }

    public void setHardcapBackpackRows(int hardcapBackpackRows) { this.hardcapBackpackRows = hardcapBackpackRows; }

    public List<String> getBackpackList() { return this.backpackList; }

    public void setDiscordID(String discordID) {
        this.discordID = discordID;
    }

    public String getDiscordID() { return this.discordID; }

    public Gang getGang() {
        if (this.gangID != 0)
            return plugin.getGangData(getGangID());

        return null;
    }

    public void setGang(Gang gang) {
        if (gang == null)
            this.gangID = 0;
        else
            this.gangID = gang.getID();

        updateDisplayName();
    }

    public void removeGang() {
        this.gangID = 0;
    }

    public void invitedToGang(Gang gang) { this.gangInvites.add(gang); }

    public void removeInviteToGang(Gang gang) { this.gangInvites.remove(gang); }

    public List<Gang> getGangInvites() { return this.gangInvites; }

    public int getGangID() { return this.gangID; }

    public boolean checkDiscordCode(int code) {
        MongoDB mongoDB = plugin.getMongoDBInstance();

        String[] discordInfo = mongoDB.getDocument("UUID", getUUID().toString()).getString("DISCORD_CODE").split(":");

        int botCode = Integer.parseInt(discordInfo[0]);

        if (code == botCode)
            setDiscordID(discordInfo[1]);
        else
            return false;

        Bson filter = new Document("UUID", this.id.toString());
        Document newValues = new Document("DISCORD_CODE", "");
        Bson updateDocument = new Document("$set", newValues);
        mongoDB.getColletion().updateMany(filter, updateDocument);

        return true;
    }

    public void addHome(String name, Location location) {
        if (this.homes.containsKey(name))
            this.homes.replace(name, location);
        else
            this.homes.put(name, location);

        this.totalHomes = this.homes.size();
    }

    public HashMap<String, Location> getHomes() { return this.homes; }

    public Location getHome(String name) {
        if (this.homes.containsKey(name))
            return this.homes.get(name);

        return null;
    }

    public void removeHome(String name) {
        this.homes.remove(name);
        this.totalHomes -= 1;
    }

    public int getTotalHomes() { return this.totalHomes; }

    public void setTotalHomes(int totalHomes) { this.totalHomes = totalHomes; }

    public int getMaxHomes() { return this.maxHomes; }

    public void setMaxHomes(int maxHomes) { this.maxHomes = maxHomes; }

    public HashMap<String, Date> getKitCooldowns() { return this.kitCooldowns; }

    public Date getKitCooldown(String kitID) {
        if (this.kitCooldowns.containsKey(kitID))
            return this.kitCooldowns.get(kitID);

        return new Date();
    }

    public void setKitCooldown(String kitID, Date expires) {
        if (this.kitCooldowns.containsKey(kitID))
            this.kitCooldowns.replace(kitID, expires);
        else
            this.kitCooldowns.put(kitID, expires);
    }

    public HashMap<String, Integer> getKitLevels() { return this.kitLevels; }

    public int getKitLevel(String kitID) {
        if (this.kitLevels.containsKey(kitID))
            return this.kitLevels.get(kitID);

            return 0;
    }

    public void setKitLevel(String kitID, int level) {
        if (this.kitLevels.containsKey(kitID))
            this.kitLevels.replace(kitID, level);
        else
            this.kitLevels.put(kitID, level);
    }

    public Toggle getToggle() { return this.toggle;}

    public List<String> getToggleList() { return this.toggleList; }

    public void setToggleList(List<String> toggleList) { this.toggleList = toggleList; }

    public TabList getTabList() { return this.tabList; }

    public void setTabList(TabList tabList) { this.tabList = tabList; }

    public HashMap<String, String> getTempStrings() { return this.tempStrings; }

    public HashMap<String, String> getFriends() { return this.friends; }

    public void addFriend(Player friend) {
        if (!this.friends.containsKey(friend.getUniqueId().toString()))
            this.friends.put(friend.getUniqueId().toString(), friend.getName());
    }

    public void removeFriend(Player friend) {
        this.friends.remove(friend.getUniqueId().toString());
    }

    public List<Player> getPendingFriends() { return this.pendingFriends; }

    public void addPendingFriend(Player friend) {
        if (!this.pendingFriends.contains(friend))
            this.pendingFriends.add(friend);
    }

    public void removePendingFriend(Player friend) {
        this.pendingFriends.remove(friend);
    }

    public int getMaxAuctions() { return this.maxAuctions; }

    public void setMaxAuctions(int maxAuctions) { this.maxAuctions = maxAuctions; }

    public int getTotalAuctions() { return this.totalAuctions; }

    public void setTotalAuctions(int totalAuctions) { this.totalAuctions = totalAuctions; }

    public Long getAuctionExpiration() { return this.auctionExpiration; }

    public void setAuctionExpiration(Long auctionExpiration) { this.auctionExpiration = auctionExpiration; }
}
