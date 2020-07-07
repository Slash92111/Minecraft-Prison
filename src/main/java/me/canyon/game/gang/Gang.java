package me.canyon.game.gang;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.gang.vault.Vault;
import me.canyon.game.player.toggle.ToggleEnum;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.*;

public class Gang {

    private Main plugin = Main.getInstance();

    private int ID;
    private String name, description;
    private UUID leader;
    private List<String> members, officers, vaultList;
    private HashMap<String, String> allMembers;
    private Vault vault;
    private int totalVaultRows, maxVaultPages;
    private long balance;

    public Gang(int ID) { //Use if gang exists
        this.ID = ID;

        if (!plugin.hasGangData(ID)) {
            Document document = plugin.getMongoDBInstance().getDocument("gang", "ID", ID);
            if (document != null) {
                this.name = document.getString("NAME");
                this.description = document.getString("DESCRIPTION");
                this.leader = UUID.fromString(document.getString("LEADER"));
                this.officers = document.getList("OFFICERS", String.class);
                this.members = document.getList("MEMBERS", String.class);
                this.balance = document.getLong("BALANCE");
                this.vaultList = document.getList("VAULT", String.class);
                this.totalVaultRows = document.getInteger("VAULT_TOTAL_ROWS");
                this.maxVaultPages = document.getInteger("VAULT_MAX_PAGES");
                this.vault = new Vault(this);

                this.allMembers = new HashMap<>();
                this.allMembers.put(leader.toString(), plugin.getMongoDBInstance().getDocument("UUID", leader.toString()).getString("IGN"));
                this.officers.forEach(id -> this.allMembers.put(id, plugin.getMongoDBInstance().getDocument("UUID", id).getString("IGN")));
                this.members.forEach(id -> this.allMembers.put(id, plugin.getMongoDBInstance().getDocument("UUID", id).getString("IGN")));


                plugin.setGangData(ID, this);
            }
        }
    }

    public Gang(Player player, String name, String description) { //Use if creating a gang
        MongoDB mongoDB = plugin.getMongoDBInstance();

        if (mongoDB.getDocument("gang", "GANG_STATS", true) != null) {
            this.ID = mongoDB.getDocument("gang", "GANG_STATS", true).getInteger("TOTAL_GANGS") + 1;
            this.name = name;
            this.description = description;
            this.leader = player.getUniqueId();
            this.officers = new ArrayList<>();
            this.members = new ArrayList<>();
            this.balance = 0L;
            this.totalVaultRows = 1;
            this.maxVaultPages = 1;
            this.vaultList = null;
            this.vault = new Vault(this);

            this.allMembers = new HashMap<>();
            this.allMembers.put(leader.toString(), player.getName());

            Document document = new Document("ID", this.ID);
            document.append("NAME", this.name);
            document.append("DESCRIPTION", this.description);
            document.append("LEADER", player.getUniqueId().toString());
            document.append("OFFICERS", this.officers);
            document.append("MEMBERS", this.members);
            document.append("BALANCE", this.balance);
            document.append("VAULT_TOTAL_ROWS", 1);
            document.append("VAULT_MAX_PAGES", 1);
            document.append("VAULT", new ArrayList<String>());

            mongoDB.insertDocument("gang", document);

            //update total gangs
            Bson filter= new Document("GANG_STATS", true);
            Document newValues = new Document("TOTAL_GANGS", this.ID);
            Bson updateDocument = new Document("$set", newValues);
            mongoDB.getColletion("gang").updateMany(filter, updateDocument);

            plugin.setGangData(ID, this);
        }
    }

    public void upload() {
        MongoDB mongoDB = plugin.getMongoDBInstance();

        Bson filter = new Document("ID", this.ID);
        Document newValues = new Document("OFFICERS", this.officers)
                .append("MEMBERS", this.members)
                .append("BALANCE", this.balance)
                .append("NAME", this.name)
                .append("DESCRIPTION", this.description)
                .append("VAULT_TOTAL_ROWS", this.totalVaultRows)
                .append("VAULT_MAX_PAGES", this.maxVaultPages)
                .append("VAULT", this.vault.toList());

        Bson updateDocument = new Document("$set", newValues);
        mongoDB.getColletion("gang").updateMany(filter, updateDocument);
    }

    public int getID() { return this.ID; }

    public UUID getLeader() { return this.leader; }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public List<String> getOfficers() { return this.officers; }

    public void removeOfficer(UUID uuid) {
        getOfficers().remove(uuid.toString());
    }

    public void addOfficer(UUID uuid) {
        if (!getOfficers().contains(uuid.toString()))
            getOfficers().add(uuid.toString());
    }

    public List<String> getMembers() { return this.members; }

    public void removeMember(UUID uuid) {
        getMembers().remove(uuid.toString());
    }

    public void addMember(UUID uuid) {
        if (!getMembers().contains(uuid.toString()))
            getMembers().add(uuid.toString());
    }

    public long getBalance() { return this.balance; }

    public void setBalance(long balance) { this.balance = balance; }

    public void sendMessage(Player sender, String message) {
        for (String uuid : allMembers.keySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));

            if (player != null && plugin.getPlayerData(player).getToggle().get(ToggleEnum.GANG_CHAT))
                if (player != sender)
                    player.sendMessage(plugin.getMessageFromConfig("gang.message", Map.of("%player_name%", sender.getName(), "%message%", message)));
        }
    }

    public void sendMessage(String message) {
        for (String uuid : allMembers.keySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));

            if (player != null && plugin.getPlayerData(player).getToggle().get(ToggleEnum.GANG_CHAT))
                player.sendMessage(message);
        }
    }

    public void sendLoginAlert(String message) {
        for (String uuid : allMembers.keySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));

            if (player != null && plugin.getPlayerData(player).getToggle().get(ToggleEnum.GANG_LOGIN_ALERT))
                player.sendMessage(message);
        }
    }

    //UUID IGN
    public HashMap<String, String> getAllMembers() {
        return this.allMembers;
    }

    public String getInformation() {
        String leader = plugin.getMongoDBInstance().getDocument("UUID", getLeader().toString()).getString("IGN");

        String officers = "";
        String members = "";
        String balance = "$" + NumberFormat.getNumberInstance(Locale.US).format(getBalance());

        for (String uuid : getOfficers())
            officers += " *" + plugin.getMongoDBInstance().getDocument("UUID", uuid).getString("IGN");


        for (String uuid : getMembers())
            members += " " + plugin.getMongoDBInstance().getDocument("UUID", uuid).getString("IGN");


        return plugin.getMessageFromConfig("gang.information", Map.of("%gang_name%", getName(), "%description%", getDescription(), "%leader%", "**" + leader, "%officers%", officers.toString(), "%members%", members.toString(), "%balance%", balance));
    }

    public List<String> getVaultList() {
        return this.vaultList;
    }

    public int getTotalVaultRows() { return this.totalVaultRows; }

    public void setTotalVaultRows(int totalVaultRows) { this.totalVaultRows = totalVaultRows; }

    public int getMaxVaultPages() { return this.maxVaultPages; }

    public void setMaxVaultPages(int maxVaultPages) { this.maxVaultPages = maxVaultPages; }

    public Vault getVault() { return this.vault; }
}
