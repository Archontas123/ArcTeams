package org.mythofy.mythofyteams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final MythofyTeams plugin;

    public ChatListener(MythofyTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getTeamCommand().isTeamChatEnabled(player)) {
            Team team = plugin.getTeamCommand().getPlayerTeam(player);

            if (team != null) {
                event.setCancelled(true);

                for (Player member : team.getMembers()) {
                    member.sendMessage(ChatColor.BLUE + "[Team] " + player.getName() + ": " + event.getMessage());
                }
            }
        }
    }
}
