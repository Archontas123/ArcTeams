package org.mythofy.mythofyteams;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TeamCommand implements CommandExecutor {

    private final Map<String, Team> teams = new HashMap<>();
    private final Map<Player, Team> playerTeams = new HashMap<>();
    private final Map<Player, Team> pendingInvites = new HashMap<>();
    private final Set<Player> teamChatEnabled = new HashSet<>();
    private final Map<Player, Integer> playerTeamCount = new HashMap<>();
    private static final int MAX_TEAMS_PER_PLAYER = 1;

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

        if (playerTeamCount.getOrDefault(player, 0) >= MAX_TEAMS_PER_PLAYER) {
            sendErrorMessage(player, "You have reached the maximum number of teams you can create.");
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

        Team team = new Team(teamName, teamTag, player);
        teams.put(teamName, team);
        playerTeams.put(player, team);
        playerTeamCount.put(player, playerTeamCount.getOrDefault(player, 0) + 1);

        player.sendMessage(ChatColor.GREEN + "Team " + teamName + " created with tag " + teamTag + ".");
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

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sendErrorMessage(player, "Player not found.");
            return true;
        }

        if (target.equals(player)) {
            sendErrorMessage(player, "You cannot invite yourself to your own team.");
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
        return true;
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

    public boolean isTeamChatEnabled(Player player) {
        return teamChatEnabled.contains(player);
    }

    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player);
    }
}
