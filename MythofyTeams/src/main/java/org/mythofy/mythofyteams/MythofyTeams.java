package org.mythofy.mythofyteams;

import org.bukkit.plugin.java.JavaPlugin;

public class MythofyTeams extends JavaPlugin {

    private TeamCommand teamCommand;

    @Override
    public void onEnable() {
        teamCommand = new TeamCommand();
        getCommand("team").setExecutor(teamCommand);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        // Any cleanup logic
    }

    public TeamCommand getTeamCommand() {
        return teamCommand;
    }
}
