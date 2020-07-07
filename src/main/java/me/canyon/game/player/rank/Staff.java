package me.canyon.game.player.rank;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.ChatColor;

public enum Staff {
    NONE(0, "NONE"),
    JR_HELPER(1, "JR_HELPER"),
    HELPER(2, "HELPER"),
    MODERATOR(3, "MODERATOR"),
    ADMIN(4, "ADMIN"),
    DEVELOPER(4, "DEVELOPER"),
    OWNER(4, "OWNER");

    private int order;
    private String name, prefix;
    private ChatColor color;

    private Main plugin = Main.getInstance();

    Staff(int order, String name) {
        this.order = order;
        this.name = name;

        verify();

        this.prefix = plugin.getMongoDBInstance().getDocument("rank", "RANK", name).getString("PREFIX");
        this.color = ChatColor.valueOf(plugin.getMongoDBInstance().getDocument("rank", "RANK", name).getString("COLOR"));
    }

    public static Staff getRankFromOrder(int order) {
        for (Staff staff : Staff.values()) {
            if (staff.getOrder() == order)
                return staff;
        }

        return NONE;
    }

    public void verify() {
        MongoDB mongoDB = plugin.getMongoDBInstance();
        if (mongoDB.getDocument("rank", "RANK", name) == null) {
            Document document = new Document("RANK", name);

            document.append("ORDER", order);
            document.append("PREFIX", name);
            document.append("COLOR", ChatColor.AQUA.name());

            mongoDB.insertDocument("rank", document);
        }
    }

    public String getPrefix() { return this.prefix; }

    public ChatColor getColor() { return this.color; }

    public void setPrefix(String prefix) {
        this.prefix = prefix;

        MongoDB mongoDB = plugin.getMongoDBInstance();

        //Upload the change
        Bson filter = new Document("RANK", name);
        Document newValues = new Document("PREFIX", prefix);
        Bson updateDocument = new Document("$set", newValues);
        mongoDB.getColletion("rank").updateMany(filter, updateDocument);
    }

    public void setColor(ChatColor color) {
        this.color = color;

        MongoDB mongoDB = plugin.getMongoDBInstance();

        Bson filter = new Document("RANK", name);
        Document newValues = new Document("COLOR", color.name());
        Bson updateDocument = new Document("$set", newValues);
        mongoDB.getColletion("rank").updateMany(filter, updateDocument);}

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return name;
    }
}
