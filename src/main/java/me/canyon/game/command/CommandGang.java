package me.canyon.game.command;

import com.mongodb.BasicDBObject;
import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.gang.Gang;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.toggle.ToggleEnum;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.*;

public class CommandGang extends BaseCommand {

    private int staffLevel;
    private Main plugin = Main.getInstance();

    public CommandGang(String command, String usage, String description, List<String> aliases, int staffLevel) {
        super(command, usage, description, aliases);
        this.staffLevel = staffLevel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        PlayerData playerData = plugin.getPlayerData(player);

        if (args.length == 0) {
            if (playerData.getGangID() == 0)
                player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
            else
                player.sendMessage(playerData.getGang().getInformation());
        } else {
            String subCommand = args[0].toLowerCase();

            if (subCommand.matches("create")) {
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang create <name> <description>")));
                    return false;
                } else {
                    if (playerData.getGangID() != 0) {
                        player.sendMessage(plugin.getMessageFromConfig("gang.mustLeaveToCreate"));
                        return false;
                    }

                    if (plugin.getMongoDBInstance().getDocument("gang", "NAME", args[1]) != null) {
                        player.sendMessage(plugin.getMessageFromConfig("gang.alreadyExists"));
                        return false;
                    }

                    String description = String.join(" ", Arrays.asList(args));
                    description = description.substring(args[0].length() + args[1].length() + 2);

                    playerData.setGang(new Gang(player, args[1], description));
                    player.sendMessage(plugin.getMessageFromConfig("gang.create", Map.of("%name%", args[1], "%description%", description)));
                }
            }
            else if (subCommand.matches("invite")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang invite <player>")));
                    return false;
                }

                Player target = Bukkit.getPlayer(args[1]);

                if (target != null) {
                    Gang gang = playerData.getGang();

                    PlayerData targetData = plugin.getPlayerData(target);

                    if (!targetData.getToggle().get(ToggleEnum.GANG_INVITE)) {
                        player.sendMessage(plugin.getMessageFromConfig("gang.notTakingInvites", Map.of("%player_name%", args[1])));
                        return false;
                    }

                    if (gang.getOfficers().contains(player.getUniqueId().toString()) || gang.getLeader().equals(player.getUniqueId())) { //Make sure an officer is inviting a player

                        targetData.invitedToGang(gang);
                        target.sendMessage(plugin.getMessageFromConfig("gang.invitedToJoin", Map.of("%gang_name%", gang.getName())));
                        player.sendMessage(plugin.getMessageFromConfig("gang.invitedPlayer", Map.of("%player_name%", args[1])));
                    } else {
                        player.sendMessage(plugin.getMessageFromConfig("gang.mustBeOfficerToInvite"));
                    }
                } else {
                    player.sendMessage(plugin.getMessageFromConfig("player.playerIsntOnline", Map.of("%player_name%", args[1])));
                    return false;
                }
            }
            else if (subCommand.matches("uninvite")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang uninvite <player>")));
                    return false;
                }

                Player target = Bukkit.getPlayer(args[1]);

                if (target != null) {
                    Gang gang = playerData.getGang();

                    if (gang.getOfficers().contains(player.getUniqueId().toString()) || gang.getLeader().equals(player.getUniqueId())) { //Make sure an officer is canceling the invite
                        if (!plugin.hasPlayerData(target))
                            plugin.setPlayerData(target);

                        PlayerData targetData = plugin.getPlayerData(target);

                        targetData.removeInviteToGang(gang);
                    } else {
                        player.sendMessage(plugin.getMessageFromConfig("gang.mustBeOfficerToUninvite"));
                    }
                } else {
                    player.sendMessage(plugin.getMessageFromConfig("player.playerIsntOnline", Map.of("%player_name%", args[1])));
                    return false;
                }
            }
            else if (subCommand.matches("join")) {
                if (playerData.getGangID() != 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.mustLeaveToJoin"));
                    return false;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang join <gang>")));
                    return false;
                }

                String gangName = args[1].toLowerCase();

                for (Gang gang : playerData.getGangInvites()) {
                    if (gang.getName().toLowerCase().matches(gangName)) {
                        playerData.setGang(gang);
                        playerData.getGangInvites().remove(gang);
                        gang.getMembers().add(player.getUniqueId().toString());
                        gang.getAllMembers().put(player.getUniqueId().toString(), player.getName());
                        gang.sendMessage(plugin.getMessageFromConfig("gang.hasJoined", Map.of("%player_name%", player.getName())));
                        return true;
                    }
                }

                player.sendMessage(plugin.getMessageFromConfig("gang.noInvite"));
                return false;
            }
            else if (subCommand.matches("decline")) {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang decline <gang>")));
                    return false;
                }

                String gangName = args[1];

                for (Gang gang : playerData.getGangInvites()) {
                    if (gang.getName().toLowerCase().matches(gangName)) {
                        playerData.removeInviteToGang(gang);
                        player.sendMessage(plugin.getMessageFromConfig("gang.declinedToJoin", Map.of("%gang_name%", gangName)));
                        return true;
                    }
                }

                player.sendMessage(plugin.getMessageFromConfig("gang.noInvite"));
                return false;
            }
            else if (subCommand.matches("kick")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang kick <player>")));
                    return false;
                }

                Gang gang = playerData.getGang();

                if (gang.getOfficers().contains(player.getUniqueId().toString()) || gang.getLeader().equals(player.getUniqueId())) {
                    Player target = Bukkit.getPlayer(args[1]);

                    MongoDB mongoDB = plugin.getMongoDBInstance();
                    Document document = mongoDB.getDocument("IGN", args[1]);

                    if (document != null) {
                        UUID uuid;

                        if (target != null) {
                            if (!plugin.hasPlayerData(target))
                                plugin.setPlayerData(target, true);

                            uuid = target.getUniqueId();
                        } else {
                            uuid = UUID.fromString(document.getString("UUID"));
                            if (!plugin.hasPlayerData(uuid))
                                plugin.setPlayerData(uuid, true);
                        }

                        PlayerData targetData = plugin.getPlayerData(uuid);

                        if (gang.getOfficers().contains(targetData.getUUID().toString())) {
                            if (gang.getLeader().equals(player.getUniqueId())) {
                                targetData.setGang(null);
                                gang.getOfficers().remove(targetData.getUUID().toString());
                                gang.getAllMembers().remove(targetData.getUUID().toString());
                                gang.sendMessage(plugin.getMessageFromConfig("gang.kickedFrom", Map.of("%player_name%", args[1])));
                                return true;
                            } else {
                                player.sendMessage(plugin.getMessageFromConfig("gang.mustBeLeaderToKickOfficer"));
                                return false;
                            }
                        } else if (gang.getMembers().contains(targetData.getUUID().toString())) {
                            targetData.setGang(null);
                            gang.getMembers().remove(targetData.getUUID().toString());
                            gang.getAllMembers().remove(targetData.getUUID().toString());
                            gang.sendMessage(plugin.getMessageFromConfig("gang.kickedFrom", Map.of("%player_name%", args[1])));
                            return true;
                        } else {
                            player.sendMessage(plugin.getMessageFromConfig("gang.isntInGang", Map.of("%player_name%", args[1])));
                            return false;
                        }
                    }
                } else {
                    player.sendMessage(plugin.getMessageFromConfig("gang.mustBeOfficerToKick"));
                    return false;
                }
            }
            else if (subCommand.matches("promote")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang promote <player>")));
                    return false;
                }

                Gang gang = playerData.getGang();

                if (gang.getLeader().equals(player.getUniqueId())) {
                    Player target = Bukkit.getPlayer(args[1]);

                    MongoDB mongoDB = plugin.getMongoDBInstance();
                    Document document = mongoDB.getDocument("IGN", args[1]);

                    if (document != null) {
                        UUID uuid;

                        if (target != null) {
                            if (!plugin.hasPlayerData(target))
                                plugin.setPlayerData(target, true);

                            uuid = target.getUniqueId();
                        } else {
                            uuid = UUID.fromString(document.getString("UUID"));
                            if (!plugin.hasPlayerData(uuid))
                                plugin.setPlayerData(uuid, true);
                        }

                        PlayerData targetData = plugin.getPlayerData(uuid);

                        if (targetData.getGangID() == gang.getID() && !targetData.getUUID().equals(player.getUniqueId())) {
                            if (gang.getOfficers().contains(targetData.getUUID().toString())) {
                                player.sendMessage(plugin.getMessageFromConfig("gang.alreadyOfficer", Map.of("%player_name%", args[1])));
                                return false;
                            } else {
                                gang.getOfficers().add(targetData.getUUID().toString());
                                gang.getMembers().remove(targetData.getUUID().toString());
                                gang.sendMessage(plugin.getMessageFromConfig("gang.hasBeenPromoted", Map.of("%player_name%", args[1])));
                            }
                        } else {
                            player.sendMessage(plugin.getMessageFromConfig("gang.isntInGang", Map.of("%player_name%", args[1])));
                        }
                    }
                } else {
                    player.sendMessage(plugin.getMessageFromConfig("gang.mustBeLeaderToPromote"));
                    return false;
                }
            }
            else if (subCommand.matches("demote")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang demote <player>")));
                    return false;
                }

                Gang gang = playerData.getGang();

                if (gang.getLeader().equals(player.getUniqueId())) {
                    Player target = Bukkit.getPlayer(args[1]);

                    MongoDB mongoDB = plugin.getMongoDBInstance();
                    Document document = mongoDB.getDocument("IGN", args[1]);

                    if (document != null) {
                        UUID uuid;

                        if (target != null) {
                            if (!plugin.hasPlayerData(target))
                                plugin.setPlayerData(target, true);

                            uuid = target.getUniqueId();
                        } else {
                            uuid = UUID.fromString(document.getString("UUID"));
                            if (!plugin.hasPlayerData(uuid))
                                plugin.setPlayerData(uuid, true);
                        }

                        PlayerData targetData = plugin.getPlayerData(uuid);

                        if (targetData.getGangID() == gang.getID() && !targetData.getUUID().equals(player.getUniqueId())) {
                            if (gang.getOfficers().contains(targetData.getUUID().toString())) {
                                gang.getOfficers().remove(targetData.getUUID().toString());
                                gang.getMembers().add(targetData.getUUID().toString());
                                gang.sendMessage(plugin.getMessageFromConfig("gang.hasBeenDemoted", Map.of("%player_name%", args[1])));
                            } else {
                                player.sendMessage(plugin.getMessageFromConfig("gang.notOfficerAlready", Map.of("%player_name%", args[1])));
                                return false;
                            }
                        } else {
                            player.sendMessage(plugin.getMessageFromConfig("gang.isntInGang", Map.of("%player_name%", args[1])));
                        }
                    }
                } else {
                    player.sendMessage(plugin.getMessageFromConfig("gang.mustBeLeaderToDemote"));
                    return false;
                }
            }
            else if (subCommand.matches("info")) {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang info <gang>")));
                    return false;
                }

                if (plugin.getMongoDBInstance().getDocument("gang", "NAME", args[1]) != null) {
                    int gangID = plugin.getMongoDBInstance().getDocument("gang", "NAME", args[1]).getInteger("ID");

                    if (!plugin.hasGangData(gangID))
                        new Gang(gangID);

                    player.sendMessage(plugin.getGangData(gangID).getInformation());
                } else {
                    player.sendMessage(plugin.getMessageFromConfig("gang.gangDoesntExist", Map.of("%gang_name%", args[1])));
                    return false;
                }
            }
            else if (subCommand.matches("set")) {
                if (playerData.getGangID() != 0) {
                    Gang gang = playerData.getGang();

                    if (gang.getLeader().equals(player.getUniqueId())) {
                        if (args.length < 3) {
                            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang set <name, description> <value>")));
                            return false;
                        }

                        if (args[1].toLowerCase().matches("name")) {
                            gang.setName(args[2]);
                            gang.sendMessage(plugin.getMessageFromConfig("gang.setName", Map.of("%name%", args[2])));
                        } else if (args[1].toLowerCase().matches("description")) {
                            String description = String.join(" ", Arrays.asList(args));
                            description = description.substring(args[0].length() + args[1].length() + 2);
                            gang.sendMessage(plugin.getMessageFromConfig("gang.setDescription", Map.of("%description%", description)));
                            gang.setDescription(description);
                        } else {
                            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/gang set <name, description> <value>")));
                            return false;
                        }
                    }
                }
            }
            else if (subCommand.matches("leave")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (playerData.getGang().getLeader().equals(playerData.getUUID())) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.mustLeaveToDisban"));
                    return false;
                }

                player.sendMessage(plugin.getMessageFromConfig("gang.leave", Map.of("%gang_name%", playerData.getGang().getName())));

                Gang gang = playerData.getGang();

                gang.getAllMembers().remove(player.getUniqueId().toString());
                gang.getMembers().remove(player.getUniqueId().toString());
                gang.getOfficers().remove(player.getUniqueId().toString());

                gang.sendMessage(plugin.getMessageFromConfig("gang.left", Map.of("%player_name%", player.getName())));

                playerData.setGang(null);

            }
            else if (subCommand.matches("disban")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                if (playerData.getGang().getLeader().equals(playerData.getUUID())) { // TODO Create a confirmation?
                    Gang gang = playerData.getGang();

                    gang.sendMessage(plugin.getMessageFromConfig("gang.hasDisbanned", Map.of("%gang_name%", gang.getName())));

                    MongoDB mongoDB = plugin.getMongoDBInstance();

                    for (String memberUUID : gang.getAllMembers().keySet()) {
                        Player target = Bukkit.getPlayer(UUID.fromString(memberUUID));
                        Document document = mongoDB.getDocument("UUID", memberUUID);

                        if (document != null) {
                            UUID uuid;

                            if (target != null) {
                                if (!plugin.hasPlayerData(target))
                                    plugin.setPlayerData(target, true);

                                uuid = target.getUniqueId();
                            } else {
                                uuid = UUID.fromString(document.getString("UUID"));
                                if (!plugin.hasPlayerData(uuid))
                                    plugin.setPlayerData(uuid, true);
                            }

                            PlayerData targetData = plugin.getPlayerData(uuid);

                            targetData.setGang(null);
                        }
                    }

                    // Removes the gang from the database
                    BasicDBObject query = new BasicDBObject();
                    query.put("ID", gang.getID());
                    mongoDB.getColletion("gang").deleteMany(query);

                } else {
                    player.sendMessage(plugin.getMessageFromConfig("gang.mustBeLeaderToDisban"));
                    return false;
                }

            }
            else if (subCommand.matches("help")) {
                player.sendMessage(plugin.getMessageFromConfig("gang.help"));
            }
            else if (subCommand.matches("deposit")) {
                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                long amount = Long.parseLong(args[1]);

                if (playerData.getBalance() >= amount) {
                    Gang gang = plugin.getGangData(playerData.getGangID());

                    gang.setBalance(gang.getBalance() + amount);
                    playerData.setBalance(playerData.getBalance() - amount);

                    gang.sendMessage(plugin.getMessageFromConfig("gang.deposited", Map.of("%player_name%", player.getName(), "%amount%", "$" + NumberFormat.getNumberInstance(Locale.US).format(amount))));
                }
            }
            else if (subCommand.matches("vault")) {
                if (args.length > 1) {
                    if (plugin.hasPlayerData(player))
                        if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
                            return false;

                    MongoDB mongoDB = plugin.getMongoDBInstance();
                    Document document = mongoDB.getDocument("gang", "NAME", args[1]);

                    if (document != null) {
                        int ID = document.getInteger("ID");

                        if (!plugin.hasGangData(ID))
                            plugin.setGangData(ID, new Gang(ID));

                        Gang gang = plugin.getGangData(ID);
                        gang.getVault().open(1, player);

                        plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=gang vault | Target=" + args[1]);

                        return true;
                    } else
                        return false;
                }

                if (playerData.getGangID() == 0) {
                    player.sendMessage(plugin.getMessageFromConfig("gang.notInGang"));
                    return false;
                }

                Gang gang = plugin.getGangData(playerData.getGangID());
                gang.getVault().open(1, player);
            }
            else { // Gang talk
                if (playerData.getGangID() == 0)
                    return false;

                String message = String.join(" ", Arrays.asList(args));

                boolean muted = playerData.getMute() != null;

                plugin.getSplunkInstance().log("chat", "Gang", "Sender=" + player.getUniqueId().toString() + " | Message=" + message + " | Muted=" + muted);

                if (!muted && playerData.getToggle().get(ToggleEnum.GANG_CHAT)) {
                    String rank = "";

                    if (playerData.getGang().getLeader().equals(playerData.getUUID()))
                        rank = "**";
                    else if (playerData.getGang().getOfficers().contains(playerData.getUUID().toString()))
                        rank = "*";

                    playerData.getGang().sendMessage(plugin.getMessageFromConfig("gang.message", Map.of("%player_name%", rank + player.getName(), "%message%", message)));
                }

            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player cmdSender = (Player) sender;
        PlayerData playerData = plugin.getPlayerData(cmdSender);

        if (cmd.getName().toLowerCase().matches("gang|g")) {
            if (args.length <= 1)
                return Arrays.asList("invite", "uninvite", "join", "decline", "kick", "promote", "demote", "info", "create", "set", "leave", "disban", "help", "deposit");
            else if (args.length == 2) {
                String subCommand = args[1].toLowerCase();

                if (subCommand.matches("invite|uninvite")) {
                    List<String> players = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
                    return players;
                } else if (subCommand.matches("join|decline")) {
                    List<String> invites = new ArrayList<>();
                    playerData.getGangInvites().forEach(gang -> invites.add(gang.getName()));
                    return invites;
                } else if (subCommand.matches("kick")) {
                    if (playerData.getGangID() != 0) {
                        if (playerData.getGang().getLeader().toString().equals(playerData.getUUID().toString())) {
                            List<String> members = new ArrayList<>();
                            for (String uuid : playerData.getGang().getAllMembers().keySet())
                                members.add(plugin.getMongoDBInstance().getDocument("UUID", uuid).getString("IGN"));
                            return members;
                        }
                    }
                } else if (subCommand.matches("promote")) {
                    if (playerData.getGangID() != 0) {
                        if (playerData.getGang().getLeader().toString().equals(playerData.getUUID().toString())) {
                            List<String> members = new ArrayList<>();
                            for (String uuid : playerData.getGang().getMembers())
                                members.add(plugin.getMongoDBInstance().getDocument("UUID", uuid).getString("IGN"));
                            return members;
                        }
                    }
                } else if (subCommand.matches("demote")) {
                    if (playerData.getGangID() != 0) {
                        if (playerData.getGang().getLeader().toString().equals(playerData.getUUID().toString())) {
                            List<String> officer = new ArrayList<>();
                            for (String uuid : playerData.getGang().getOfficers())
                                officer.add(plugin.getMongoDBInstance().getDocument("UUID", uuid).getString("IGN"));
                            return officer;
                        }
                    }
                } else if (subCommand.matches("create"))
                    return Collections.singletonList("<name>");
                else if (subCommand.matches("set"))
                    return Arrays.asList("name", "description");
                else if (subCommand.matches("deposit"))
                    return Collections.singletonList("<amount>");
            } else if (args.length == 3) {
                String subCommand = args[1].toLowerCase();

                if (subCommand.matches("create"))
                    return Collections.singletonList("<description>");
                else if (subCommand.matches("set"))
                    return Collections.singletonList("<" + args[2] + ">");
            }
        }

        return null;
    }
}
