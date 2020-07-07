package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class CommandReport extends BaseCommand {
    //TODO add a cooldown to using the command. 1 minutes?

    public CommandReport(String command, String usage, String description) {
        super(command, usage, description);
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/report <report information>")));
            return false;
        }

        MongoDB mongoDB = plugin.getMongoDBInstance();

        if (mongoDB.getDocument("report", "REPORT_STATS", true) != null) {
            int reportID = mongoDB.getDocument("report", "REPORT_STATS", true).getInteger("TOTAL_REPORTS") + 1;

            String report = String.join(" ", Arrays.asList(args));

            //Create the report into the database
            Document document = new Document("ID", reportID);
            document.append("REPORTER", plugin.getPlayerData(player).getDiscordID());
            document.append("REPORT", report);
            document.append("DATE", new Date());
            document.append("CREATED", false);
            document.append("RESOLVED", false);
            document.append("RESOLUTION", "");
            mongoDB.insertDocument("report", document);

            //Update the total reports number
            Bson filter= new Document("REPORT_STATS", true);
            Document newValues = new Document("TOTAL_REPORTS", reportID);
            Bson updateDocument = new Document("$set", newValues);
            mongoDB.getColletion("report").updateMany(filter, updateDocument);

            if (!plugin.getPlayerData(player).getDiscordID().equals(""))
                player.sendMessage(plugin.getMessageFromConfig("report.reportedWithDiscord", Map.of("%report_id%", reportID + "")));
            else
                player.sendMessage(plugin.getMessageFromConfig("report.reported", Map.of("%report_id%", reportID + "")));

        }

        return true;
    }
}
