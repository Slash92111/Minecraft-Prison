package me.canyon.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.canyon.Main;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.bukkit.ChatColor;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDB {

    private MongoCollection<Document> collection;
    private MongoDatabase mongoDatabase;

    private Main plugin;

    public MongoDB(Main plugin) {
        this.plugin = plugin;

        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.WARNING);

        connect();
    }

    private void connect() {
        String uri = plugin.getDatabaseConfig().getString("mongo.connection_string"); //54212
        MongoClientURI clientURI = new MongoClientURI(uri);
        MongoClient mongoClient = new MongoClient(clientURI);

        mongoDatabase = mongoClient.getDatabase(plugin.getDatabaseConfig().getString("mongo.database"));
        collection = mongoDatabase.getCollection("player");

        plugin.debug(ChatColor.GREEN + "[MongoDB] Connected to '" + "minecraft" + "' database!");
    }

    public Document getDocument(String key, Object value) {
        Document document = new Document(key, value);
        return collection.find(document).first();
    }

    public Document getDocument(String collection, String key, Object value) {
        Document document = new Document(key, value);
        return mongoDatabase.getCollection(collection).find(document).first();
    }

    public void insertDocument(Document document) {
        collection.insertOne(document);
    }

    public void insertDocument(String collection, Document document) { mongoDatabase.getCollection(collection).insertOne(document); }

    public MongoCollection<Document> getColletion() {
        return this.collection;
    }

    public MongoCollection<Document> getColletion(String collection) { return mongoDatabase.getCollection(collection); }
}
