package me.canyon.game.auction;

import com.mongodb.BasicDBObject;
import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.util.Utilities;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.Date;
import java.util.UUID;

public class Auction {

    private Main plugin = Main.getInstance();

    private int id;
    private ItemStack item;
    private String sterilizedItem;
    private UUID ownerID;
    private String ownerIGN;
    private Date ends;
    private long cost;

    public Auction(int id, ItemStack item, UUID ownerID, String ownerIGN, Date ends, long cost, boolean newAuction) {
        this.id = id;
        this.item = item;
        this.sterilizedItem = Utilities.sterilize(item);
        this.ownerID = ownerID;
        this.ownerIGN = ownerIGN;
        this.ends = ends;
        this.cost = cost;

        if (newAuction)
            addToDatabase();
    }

    public int getID() { return this.id; }

    public ItemStack getItem() { return this.item; }

    public String getSterilizedItem() { return this.sterilizedItem; }

    public UUID getOwnerID() { return this.ownerID; }

    public String getOwnerIGN() { return this.ownerIGN; }

    public Date getEnd() { return this.ends; }

    public long getCost() { return this.cost; }

    private void addToDatabase() {
        MongoDB mongoDB = plugin.getMongoDBInstance();

        if (mongoDB.getDocument("auction", "ID", this.id) == null) {
            plugin.debug(ChatColor.GREEN + "Adding a new item to the auction");
            Document document = new Document("ID", this.id);

            document.append("ITEM", this.sterilizedItem);
            document.append("OWNER_ID", this.ownerID.toString());
            document.append("OWNER_IGN", this.ownerIGN);
            document.append("ENDS", this.ends);
            document.append("COST", this.cost);

            mongoDB.insertDocument("auction", document);
            plugin.debug(ChatColor.GREEN + "Added a new item to the auction DB");
        }
    }

    public void removeFromDatabase() {
        MongoDB mongoDB = plugin.getMongoDBInstance();

        if (mongoDB.getDocument("auction", "ID", this.id) != null) {
            BasicDBObject query = new BasicDBObject();
            query.put("ID", this.id);
            mongoDB.getColletion("auction").deleteMany(query);
        }
    }
}
