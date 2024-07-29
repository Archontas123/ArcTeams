package org.mythofy.mythofyteams;

import org.bukkit.plugin.java.JavaPlugin;

public class MythofyTeams extends JavaPlugin {

    private TeamCommand teamCommand;

    @Override
    public void onEnable() {
        teamCommand = new TeamCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PvpListener(this), this);
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Any cleanup logic
    }

    public TeamCommand getTeamCommand() {
        return teamCommand;
    }

    public static MythofyTeams getInstance() {
        return getPlugin(MythofyTeams.class);
    }
}
