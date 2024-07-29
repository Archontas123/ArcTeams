package org.mythofy.mythofyteams;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TeamCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<Player, Team> playerTeams = new HashMap<>();
    private final Map<Player, Team> pendingInvites = new HashMap<>();
    private final Map<Player, Team> pendingAllyRequests = new HashMap<>();
    private final Map<Player, Team> pendingAllyPvpToggles = new HashMap<>();
    private final Set<Player> teamChatEnabled = new HashSet<>();
    private static final int MAX_TEAMS_PER_PLAYER = 1;

    private final Map<String, Long> lastAllyChange = new HashMap<>();

    private File teamsFile;
    private FileConfiguration teamsConfig;

    public TeamCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        loadTeamsConfig();
    }

    private void loadTeamsConfig() {
        teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!teamsFile.exists()) {
            teamsFile.getParentFile().mkdirs();
            plugin.saveResource("teams.yml", false);
        }

        teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        loadTeams();
    }

    private void loadTeams() {
        if (teamsConfig.contains("teams")) {
            for (String teamName : teamsConfig.getConfigurationSection("teams").getKeys(false)) {
                String tag = teamsConfig.getString("teams." + teamName + ".tag");
                String ownerName = teamsConfig.getString("teams." + teamName + ".owner");
                Player owner = Bukkit.getPlayer(ownerName);
                if (owner != null) {
                    Team team = new Team(teamName, tag, owner);
                    team.setPvpEnabled(teamsConfig.getBoolean("teams." + teamName + ".pvpEnabled"));
                    team.setAllyPvpEnabled(teamsConfig.getBoolean("teams." + teamName + ".allyPvpEnabled"));
                    if (teamsConfig.contains("teams." + teamName + ".home")) {
                        team.setHome(teamsConfig.getLocation("teams." + teamName + ".home"));
                    }
                    teams.put(teamName, team);
                    playerTeams.put(owner, team);
                    for (String memberName : teamsConfig.getStringList("teams." + teamName + ".members")) {
                        Player member = Bukkit.getPlayer(memberName);
                        if (member != null) {
                            team.addMember(member);
                            playerTeams.put(member, team);
                        }
                    }
                    for (String allyName : teamsConfig.getStringList("teams." + teamName + ".allies")) {
                        Team ally = teams.get(allyName);
                        if (ally != null) {
                            team.addAlly(ally);
                        }
                    }
                    for (String enemyName : teamsConfig.getStringList("teams." + teamName + ".enemies")) {
                        Team enemy = teams.get(enemyName);
                        if (enemy != null) {
                            team.addEnemy(enemy);
                        }
                    }
                }
            }
        }
    }

    private void saveTeamsConfig() {
        try {
            for (Team team : teams.values()) {
                teamsConfig.set("teams." + team.getName() + ".tag", team.getTag());
                teamsConfig.set("teams." + team.getName() + ".owner", team.getOwner().getName());
                teamsConfig.set("teams." + team.getName() + ".members", team.getMembers().stream().map(Player::getName).toArray(String[]::new));
                teamsConfig.set("teams." + team.getName() + ".allies", team.getAllies().stream().map(Team::getName).toArray(String[]::new));
                teamsConfig.set("teams." + team.getName() + ".enemies", team.getEnemies().stream().map(Team::getName).toArray(String[]::new));
                teamsConfig.set("teams." + team.getName() + ".pvpEnabled", team.isPvpEnabled());
                teamsConfig.set("teams." + team.getName() + ".allyPvpEnabled", team.isAllyPvpEnabled());
                if (team.getHome() != null) {
                    teamsConfig.set("teams." + team.getName() + ".home", team.getHome());
                }
            }
            teamsConfig.save(teamsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sendErrorMessage(player, "You must specify a subcommand.");
            return true;
        }

        String subcommand = args[0];

        switch (subcommand.toLowerCase()) {
            case "create":
                return handleCreate(player, args);
            case "invite":
                return handleInvite(player, args);
            case "leave":
                if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                    return handleLeaveConfirm(player);
                } else {
                    return handleLeave(player);
                }
            case "chat":
                return handleChat(player);
            case "accept":
                return handleAccept(player);
            case "deny":
                return handleDeny(player);
            case "list":
                return handleList(player);
            case "ally":
                if (args.length == 2) {
                    return handleAlly(player, args[1]);
                } else if (args.length == 3 && args[1].equalsIgnoreCase("list")) {
                    return handleAllyList(player);
                } else {
                    sendErrorMessage(player, "Usage: /team ally <team name> or /team ally list");
                    return true;
                }
            case "neutral":
                if (args.length == 2) {
                    return handleNeutral(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team neutral <team name>");
                    return true;
                }
            case "enemy":
                if (args.length == 2) {
                    return handleEnemy(player, args[1]);
                } else if (args[1].equalsIgnoreCase("list")) {
                    return handleEnemyList(player);
                } else {
                    sendErrorMessage(player, "Usage: /team enemy <team name> or /team enemy list");
                    return true;
                }
            case "allyaccept":
                if (args.length == 2) {
                    return handleAllyAccept(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team allyaccept <team name>");
                    return true;
                }
            case "allydeny":
                if (args.length == 2) {
                    return handleAllyDeny(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team allydeny <team name>");
                    return true;
                }
            case "pvptoggle":
                return handlePvpToggle(player);
            case "allypvptoggle":
                if (args.length == 2) {
                    return handleAllyPvpToggle(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team allypvptoggle <team name>");
                    return true;
                }
            case "allypvptoggleaccept":
                if (args.length == 2) {
                    return handleAllyPvpToggleAccept(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team allypvptoggleaccept <team name>");
                    return true;
                }
            case "allypvptoggledeny":
                if (args.length == 2) {
                    return handleAllyPvpToggleDeny(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team allypvptoggledeny <team name>");
                    return true;
                }
            case "promote":
                if (args.length == 2) {
                    return handlePromote(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team promote <username>");
                    return true;
                }
            case "demote":
                if (args.length == 2) {
                    return handleDemote(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team demote <username>");
                    return true;
                }
            case "disband":
                if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                    return handleDisbandConfirm(player);
                } else {
                    return handleDisband(player);
                }
            case "kick":
                if (args.length == 2) {
                    return handleKick(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team kick <username>");
                    return true;
                }
            case "ban":
                if (args.length == 2) {
                    return handleBan(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team ban <username>");
                    return true;
                }
            case "unban":
                if (args.length == 2) {
                    return handleUnban(player, args[1]);
                } else {
                    sendErrorMessage(player, "Usage: /team unban <username>");
                    return true;
                }
            case "transfer":
                return handleTransfer(player, args);
            case "homeset":
                return handleHomeSet(player);
            case "home":
                return handleHome(player);
            case "homedelete":
                return handleHomeDelete(player);
            default:
                sendErrorMessage(player, "Unknown subcommand.");
                return true;
        }
    }

    private void sendErrorMessage(Player player, String errorMessage) {
        player.sendMessage(ChatColor.RED + errorMessage);
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            sendErrorMessage(player, "Usage: /team create <team name> <team tag>");
            return true;
        }

        if (playerTeams.containsKey(player)) {
            sendErrorMessage(player, "You are already in a team. Leave your current team before creating a new one.");
            return true;
        }

        String teamName = args[1];
        String teamTag = args[2];

        if (teamTag.length() > 5) {
            sendErrorMessage(player, "Team tag must be 5 characters or less.");
            return true;
        }

        if (teams.containsKey(teamName)) {
            sendErrorMessage(player, "A team with that name already exists.");
            return true;
        }

        for (Team team : teams.values()) {
            if (team.getTag().equalsIgnoreCase(teamTag)) {
                sendErrorMessage(player, "A team with that tag already exists.");
                return true;
            }
        }

        Team team = new Team(teamName, teamTag, player);
        teams.put(teamName, team);
        playerTeams.put(player, team);

        player.sendMessage(ChatColor.GREEN + "Team " + teamName + " created with tag " + teamTag + ".");
        saveTeamsConfig();
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(player, "Usage: /team invite <username>");
            return true;
        }

        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.isModOrAbove(player)) {
            sendErrorMessage(player, "Only mods and above can invite players.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (target.equals(player)) {
            sendErrorMessage(player, "You cannot invite yourself to your own team.");
            return true;
        }

        if (team.isBanned(target)) {
            sendErrorMessage(player, "This player is banned from your team.");
            return true;
        }

        pendingInvites.put(target, team);
        TextComponent message = new TextComponent("You have been invited to join " + team.getName() + ".");
        message.setColor(net.md_5.bungee.api.ChatColor.GREEN);

        TextComponent acceptButton = new TextComponent("[Accept]");
        acceptButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptButton.setBold(true);
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team accept"));

        TextComponent denyButton = new TextComponent("[Deny]");
        denyButton.setColor(net.md_5.bungee.api.ChatColor.RED);
        denyButton.setBold(true);
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team deny"));

        message.addExtra(" ");
        message.addExtra(acceptButton);
        message.addExtra(" ");
        message.addExtra(denyButton);

        target.spigot().sendMessage(message);
        player.sendMessage(ChatColor.GREEN + "Invitation sent to " + target.getName() + ".");
        return true;
    }

    private boolean handleAccept(Player player) {
        Team team = pendingInvites.get(player);

        if (team == null) {
            sendErrorMessage(player, "You do not have any pending invitations.");
            return true;
        }

        team.addMember(player);
        playerTeams.put(player, team);
        pendingInvites.remove(player);

        player.sendMessage(ChatColor.GREEN + "You have joined the team " + team.getName() + ".");
        team.getOwner().sendMessage(ChatColor.GREEN + player.getName() + " has accepted the team invite.");
        saveTeamsConfig();
        return true;
    }

    private boolean handleDeny(Player player) {
        Team team = pendingInvites.get(player);

        if (team == null) {
            sendErrorMessage(player, "You do not have any pending invitations.");
            return true;
        }

        pendingInvites.remove(player);

        player.sendMessage(ChatColor.RED + "You have denied the invitation to join the team " + team.getName() + ".");
        return true;
    }

    private boolean handleLeave(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        TextComponent message = new TextComponent("Are you sure you want to leave the team " + team.getName() + "?");
        message.setColor(net.md_5.bungee.api.ChatColor.RED);

        TextComponent confirmButton = new TextComponent("[Confirm]");
        confirmButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        confirmButton.setBold(true);
        confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team leave confirm"));

        message.addExtra(" ");
        message.addExtra(confirmButton);

        player.spigot().sendMessage(message);
        return true;
    }

    private boolean handleLeaveConfirm(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        team.removeMember(player);
        playerTeams.remove(player);

        player.sendMessage(ChatColor.GREEN + "You have left the team " + team.getName() + ".");
        if (team.getOwner().equals(player)) {
            transferOwnershipToNextHighestRank(player, team);
        }
        saveTeamsConfig();
        return true;
    }

    private void transferOwnershipToNextHighestRank(Player leavingOwner, Team team) {
        List<Player> candidates = new ArrayList<>();
        if (!team.getCoOwners().isEmpty()) {
            candidates.addAll(team.getCoOwners());
        } else if (!team.getAdmins().isEmpty()) {
            candidates.addAll(team.getAdmins());
        } else if (!team.getMods().isEmpty()) {
            candidates.addAll(team.getMods());
        }

        if (!candidates.isEmpty()) {
            Player newOwner = candidates.get(new Random().nextInt(candidates.size()));
            team.transferOwnership(newOwner);
            leavingOwner.sendMessage(ChatColor.GREEN + "Ownership has been transferred to " + newOwner.getName());
            newOwner.sendMessage(ChatColor.GREEN + "You are now the owner of the team.");
        } else {
            team.disband();
            saveTeamsConfig();
        }
    }

    private boolean handleChat(Player player) {
        if (teamChatEnabled.contains(player)) {
            teamChatEnabled.remove(player);
            player.sendMessage(ChatColor.GREEN + "Team chat disabled.");
        } else {
            teamChatEnabled.add(player);
            player.sendMessage(ChatColor.GREEN + "Team chat enabled.");
        }

        return true;
    }

    private boolean handleList(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        StringBuilder teamInfo = new StringBuilder(ChatColor.GREEN + "Team Name: " + team.getName() + "\n");
        teamInfo.append(ChatColor.GREEN + "Team Tag: " + team.getTag() + "\n");
        teamInfo.append(ChatColor.GREEN + "Members: ");

        for (Player member : team.getMembers()) {
            teamInfo.append(member.getName()).append(" ");
        }

        player.sendMessage(teamInfo.toString());
        return true;
    }

    private boolean handleAlly(Player player, String targetTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team targetTeam = teams.get(targetTeamName);

        if (targetTeam == null) {
            sendErrorMessage(player, "The specified team does not exist.");
            return true;
        }

        if (team.equals(targetTeam)) {
            sendErrorMessage(player, "You cannot ally with your own team.");
            return true;
        }

        if (!canChangeAllyStatus(team, targetTeam)) {
            sendErrorMessage(player, "You must wait 24 hours between changing ally status.");
            return true;
        }

        pendingAllyRequests.put(targetTeam.getOwner(), team);

        Player targetLeader = targetTeam.getOwner();
        TextComponent message = new TextComponent("Team " + team.getName() + " wants to ally with your team.");
        message.setColor(net.md_5.bungee.api.ChatColor.GREEN);

        TextComponent acceptButton = new TextComponent("[Accept]");
        acceptButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptButton.setBold(true);
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team allyaccept " + team.getName()));

        TextComponent denyButton = new TextComponent("[Deny]");
        denyButton.setColor(net.md_5.bungee.api.ChatColor.RED);
        denyButton.setBold(true);
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team allydeny " + team.getName()));

        message.addExtra(" ");
        message.addExtra(acceptButton);
        message.addExtra(" ");
        message.addExtra(denyButton);

        targetLeader.spigot().sendMessage(message);
        player.sendMessage(ChatColor.GREEN + "Ally request sent to " + targetTeam.getName() + ".");
        return true;
    }

    private boolean handleAllyAccept(Player player, String requestingTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team requestingTeam = pendingAllyRequests.get(player);

        if (requestingTeam == null || !requestingTeam.getName().equals(requestingTeamName)) {
            sendErrorMessage(player, "No pending ally request from that team.");
            return true;
        }

        team.addAlly(requestingTeam);
        requestingTeam.addAlly(team);

        lastAllyChange.put(team.getName() + requestingTeam.getName(), System.currentTimeMillis());
        pendingAllyRequests.remove(player);

        player.sendMessage(ChatColor.YELLOW + "You have allied with " + requestingTeam.getName() + ".");
        Player requestingTeamLeader = requestingTeam.getOwner();
        requestingTeamLeader.sendMessage(ChatColor.YELLOW + "Your ally request to " + team.getName() + " has been accepted.");
        saveTeamsConfig();
        return true;
    }

    private boolean handleAllyDeny(Player player, String requestingTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team requestingTeam = pendingAllyRequests.get(player);

        if (requestingTeam == null || !requestingTeam.getName().equals(requestingTeamName)) {
            sendErrorMessage(player, "No pending ally request from that team.");
            return true;
        }

        pendingAllyRequests.remove(player);

        player.sendMessage(ChatColor.RED + "You have denied the ally request from " + requestingTeam.getName() + ".");
        Player requestingTeamLeader = requestingTeam.getOwner();
        requestingTeamLeader.sendMessage(ChatColor.RED + "Your ally request to " + team.getName() + " has been denied.");
        return true;
    }

    private boolean handleNeutral(Player player, String targetTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team targetTeam = teams.get(targetTeamName);

        if (targetTeam == null) {
            sendErrorMessage(player, "The specified team does not exist.");
            return true;
        }

        if (team.equals(targetTeam)) {
            sendErrorMessage(player, "You cannot set your own team as neutral.");
            return true;
        }

        team.removeAlly(targetTeam);
        team.removeEnemy(targetTeam);

        player.sendMessage(ChatColor.GRAY + "You have set " + targetTeam.getName() + " as neutral.");
        saveTeamsConfig();
        return true;
    }

    private boolean handleEnemy(Player player, String targetTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team targetTeam = teams.get(targetTeamName);

        if (targetTeam == null) {
            sendErrorMessage(player, "The specified team does not exist.");
            return true;
        }

        if (team.equals(targetTeam)) {
            sendErrorMessage(player, "You cannot set your own team as an enemy.");
            return true;
        }

        if (!canChangeAllyStatus(team, targetTeam)) {
            sendErrorMessage(player, "You must wait 24 hours between changing ally status.");
            return true;
        }

        team.addEnemy(targetTeam);
        targetTeam.addEnemy(team);

        lastAllyChange.put(team.getName() + targetTeam.getName(), System.currentTimeMillis());

        player.sendMessage(ChatColor.RED + "You have set " + targetTeam.getName() + " as an enemy.");
        saveTeamsConfig();
        return true;
    }

    private boolean handleAllyList(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        StringBuilder allyList = new StringBuilder(ChatColor.YELLOW + "Allies: ");
        for (Team ally : team.getAllies()) {
            allyList.append(ally.getName()).append(" ");
        }

        player.sendMessage(allyList.toString());
        return true;
    }

    private boolean handleEnemyList(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        StringBuilder enemyList = new StringBuilder(ChatColor.RED + "Enemies: ");
        for (Team enemy : team.getEnemies()) {
            enemyList.append(enemy.getName()).append(" ");
        }

        player.sendMessage(enemyList.toString());
        return true;
    }

    private boolean handlePvpToggle(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player)) {
            sendErrorMessage(player, "Only the team owner can toggle PvP settings.");
            return true;
        }

        team.setPvpEnabled(!team.isPvpEnabled());

        player.sendMessage(ChatColor.GREEN + "PvP between teammates is now " + (team.isPvpEnabled() ? "enabled" : "disabled") + ".");
        saveTeamsConfig();
        return true;
    }

    private boolean handleAllyPvpToggle(Player player, String targetTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team targetTeam = teams.get(targetTeamName);

        if (targetTeam == null) {
            sendErrorMessage(player, "The specified team does not exist.");
            return true;
        }

        if (!team.getAllies().contains(targetTeam)) {
            sendErrorMessage(player, "The specified team is not an ally.");
            return true;
        }

        pendingAllyPvpToggles.put(targetTeam.getOwner(), team);

        Player targetLeader = targetTeam.getOwner();
        TextComponent message = new TextComponent("Team " + team.getName() + " wants to toggle PvP settings with your team.");
        message.setColor(net.md_5.bungee.api.ChatColor.GREEN);

        TextComponent acceptButton = new TextComponent("[Accept]");
        acceptButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptButton.setBold(true);
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team allypvptoggleaccept " + team.getName()));

        TextComponent denyButton = new TextComponent("[Deny]");
        denyButton.setColor(net.md_5.bungee.api.ChatColor.RED);
        denyButton.setBold(true);
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team allypvptoggledeny " + team.getName()));

        message.addExtra(" ");
        message.addExtra(acceptButton);
        message.addExtra(" ");
        message.addExtra(denyButton);

        targetLeader.spigot().sendMessage(message);
        player.sendMessage(ChatColor.GREEN + "PvP toggle request sent to " + targetTeam.getName() + ".");
        return true;
    }

    private boolean handleAllyPvpToggleAccept(Player player, String requestingTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team requestingTeam = pendingAllyPvpToggles.get(player);

        if (requestingTeam == null || !requestingTeam.getName().equals(requestingTeamName)) {
            sendErrorMessage(player, "No pending PvP toggle request from that team.");
            return true;
        }

        team.setAllyPvpEnabled(!team.isAllyPvpEnabled());
        requestingTeam.setAllyPvpEnabled(team.isAllyPvpEnabled());

        pendingAllyPvpToggles.remove(player);

        player.sendMessage(ChatColor.GREEN + "PvP between your team and " + requestingTeam.getName() + " is now " + (team.isAllyPvpEnabled() ? "enabled" : "disabled") + ".");
        Player requestingTeamLeader = requestingTeam.getOwner();
        requestingTeamLeader.sendMessage(ChatColor.GREEN + "PvP between your team and " + team.getName() + " is now " + (team.isAllyPvpEnabled() ? "enabled" : "disabled") + ".");
        saveTeamsConfig();
        return true;
    }

    private boolean handleAllyPvpToggleDeny(Player player, String requestingTeamName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Team requestingTeam = pendingAllyPvpToggles.get(player);

        if (requestingTeam == null || !requestingTeam.getName().equals(requestingTeamName)) {
            sendErrorMessage(player, "No pending PvP toggle request from that team.");
            return true;
        }

        pendingAllyPvpToggles.remove(player);

        player.sendMessage(ChatColor.RED + "You have denied the PvP toggle request from " + requestingTeam.getName() + ".");
        Player requestingTeamLeader = requestingTeam.getOwner();
        requestingTeamLeader.sendMessage(ChatColor.RED + "Your PvP toggle request to " + team.getName() + " has been denied.");
        return true;
    }

    private boolean canChangeAllyStatus(Team team, Team targetTeam) {
        long currentTime = System.currentTimeMillis();
        long lastChangeTime = lastAllyChange.getOrDefault(team.getName() + targetTeam.getName(), 0L);

        return (currentTime - lastChangeTime) >= TimeUnit.HOURS.toMillis(24);
    }

    private boolean handlePromote(Player player, String targetPlayerName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.isAdmin(player)) {
            sendErrorMessage(player, "Only the team owner or admins can promote members.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (!team.getMembers().contains(targetPlayer)) {
            sendErrorMessage(player, "The specified player is not a member of your team.");
            return true;
        }

        if (team.promoteMember(targetPlayer)) {
            String rank = team.getRank(targetPlayer);
            player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " has been promoted to " + rank + ".");
            targetPlayer.sendMessage(ChatColor.GREEN + "You have been promoted to " + rank + ".");
            for (Player member : team.getMembers()) {
                if (!member.equals(player) && !member.equals(targetPlayer)) {
                    member.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " has been promoted to " + rank + ".");
                }
            }
            saveTeamsConfig();
            return true;
        } else {
            sendErrorMessage(player, "The specified player is already at the highest rank.");
            return true;
        }
    }

    private boolean handleDemote(Player player, String targetPlayerName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.isAdmin(player)) {
            sendErrorMessage(player, "Only the team owner or admins can demote members.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (!team.getMembers().contains(targetPlayer)) {
            sendErrorMessage(player, "The specified player is not a member of your team.");
            return true;
        }

        if (team.demoteMember(targetPlayer)) {
            String rank = team.getRank(targetPlayer);
            player.sendMessage(ChatColor.RED + targetPlayer.getName() + " has been demoted to " + rank + ".");
            targetPlayer.sendMessage(ChatColor.RED + "You have been demoted to " + rank + ".");
            for (Player member : team.getMembers()) {
                if (!member.equals(player) && !member.equals(targetPlayer)) {
                    member.sendMessage(ChatColor.RED + targetPlayer.getName() + " has been demoted to " + rank + ".");
                }
            }
            saveTeamsConfig();
            return true;
        } else {
            sendErrorMessage(player, "The specified player is already at the lowest rank.");
            return true;
        }
    }

    private boolean handleDisband(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player)) {
            sendErrorMessage(player, "Only the team owner can disband the team.");
            return true;
        }

        TextComponent message = new TextComponent("Are you sure you want to disband the team " + team.getName() + "?");
        message.setColor(net.md_5.bungee.api.ChatColor.RED);

        TextComponent confirmButton = new TextComponent("[Confirm]");
        confirmButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        confirmButton.setBold(true);
        confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team disband confirm"));

        message.addExtra(" ");
        message.addExtra(confirmButton);

        player.spigot().sendMessage(message);
        return true;
    }

    private boolean handleDisbandConfirm(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player)) {
            sendErrorMessage(player, "Only the team owner can disband the team.");
            return true;
        }

        for (Player member : team.getMembers()) {
            playerTeams.remove(member);
            member.sendMessage(ChatColor.RED + "Your team has been disbanded.");
        }

        teams.remove(team.getName());
        saveTeamsConfig();

        player.sendMessage(ChatColor.GREEN + "You have disbanded your team.");
        return true;
    }

    private boolean handleKick(Player player, String targetPlayerName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.isModOrAbove(player)) {
            sendErrorMessage(player, "Only the team owner or mods can kick members.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (!team.getMembers().contains(targetPlayer)) {
            sendErrorMessage(player, "The specified player is not a member of your team.");
            return true;
        }

        team.removeMember(targetPlayer);
        playerTeams.remove(targetPlayer);

        player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " has been kicked from the team.");
        targetPlayer.sendMessage(ChatColor.RED + "You have been kicked from the team.");
        for (Player member : team.getMembers()) {
            member.sendMessage(ChatColor.RED + targetPlayer.getName() + " has been kicked from the team.");
        }
        saveTeamsConfig();
        return true;
    }

    private boolean handleBan(Player player, String targetPlayerName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.isAdminOrAbove(player)) {
            sendErrorMessage(player, "Only the team owner or admins can ban members.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (!team.getMembers().contains(targetPlayer)) {
            sendErrorMessage(player, "The specified player is not a member of your team.");
            return true;
        }

        team.removeMember(targetPlayer);
        team.banMember(targetPlayer);
        playerTeams.remove(targetPlayer);

        player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " has been banned from the team.");
        targetPlayer.sendMessage(ChatColor.RED + "You have been banned from the team.");
        for (Player member : team.getMembers()) {
            member.sendMessage(ChatColor.RED + targetPlayer.getName() + " has been banned from the team.");
        }
        saveTeamsConfig();
        return true;
    }

    private boolean handleUnban(Player player, String targetPlayerName) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.isAdminOrAbove(player)) {
            sendErrorMessage(player, "Only the team owner or admins can unban members.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (!team.isBanned(targetPlayer)) {
            sendErrorMessage(player, "The specified player is not banned from your team.");
            return true;
        }

        team.unbanMember(targetPlayer);

        player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " has been unbanned from the team.");
        targetPlayer.sendMessage(ChatColor.GREEN + "You have been unbanned from the team.");
        saveTeamsConfig();
        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(player, "Usage: /team transfer <username>");
            return true;
        }

        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player)) {
            sendErrorMessage(player, "Only the team owner can transfer ownership.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);

        if (targetPlayer == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (!team.getMembers().contains(targetPlayer)) {
            sendErrorMessage(player, "The specified player is not a member of your team.");
            return true;
        }

        team.transferOwnership(targetPlayer);
        player.sendMessage(ChatColor.GREEN + "You have transferred ownership to " + targetPlayer.getName() + ".");
        targetPlayer.sendMessage(ChatColor.GREEN + "You are now the owner of the team.");
        saveTeamsConfig();
        return true;
    }

    private boolean handleHomeSet(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.getCoOwners().contains(player)) {
            sendErrorMessage(player, "Only the team owner or co-owners can set the team home.");
            return true;
        }

        Location location = player.getLocation();
        team.setHome(location);
        saveTeamsConfig();

        player.sendMessage(ChatColor.GREEN + "Team home set to your current location.");
        return true;
    }

    private boolean handleHome(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        Location home = team.getHome();

        if (home == null) {
            sendErrorMessage(player, "Your team does not have a home set.");
            return true;
        }

        player.teleport(home);
        player.sendMessage(ChatColor.GREEN + "Teleported to your team home.");
        return true;
    }

    private boolean handleHomeDelete(Player player) {
        Team team = playerTeams.get(player);

        if (team == null) {
            sendErrorMessage(player, "You are not in a team.");
            return true;
        }

        if (!team.getOwner().equals(player) && !team.getCoOwners().contains(player)) {
            sendErrorMessage(player, "Only the team owner or co-owners can delete the team home.");
            return true;
        }

        team.setHome(null);
        saveTeamsConfig();

        player.sendMessage(ChatColor.GREEN + "Team home deleted.");
        return true;
    }

    public boolean isTeamChatEnabled(Player player) {
        return teamChatEnabled.contains(player);
    }

    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player);
    }
}
