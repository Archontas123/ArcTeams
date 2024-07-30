package org.mythofy.mythofyteams;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;

public class MythofyTeams extends JavaPlugin implements Listener {

    private TeamCommand teamCommand;

    @Override
    public void onEnable() {
        teamCommand = new TeamCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        teamCommand.saveTeamsConfig();
    }
    public TeamCommand getTeamCommand() {
        return teamCommand;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teamCommand.loadPlayerTeam(event.getPlayer());
    }
}
