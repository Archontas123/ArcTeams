package org.mythofy.mythofyteams;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Team {
    private final String name;
    private final String tag;
    private Player owner;
    private final Set<Player> members = new HashSet<>();
    private final Set<Team> allies = new HashSet<>();
    private final Set<Team> enemies = new HashSet<>();
    private final Set<String> pvpAllowedPlayers = new HashSet<>();
    private boolean pvpEnabled = true;
    private boolean allyPvpEnabled = true;
    private final Set<Player> mods = new HashSet<>();
    private final Set<Player> admins = new HashSet<>();
    private final Set<Player> coOwners = new HashSet<>();
    private final Set<Player> bannedMembers = new HashSet<>();
    private Location home;

    public Team(String name, String tag, Player owner) {
        this.name = name;
        this.tag = tag;
        this.owner = owner;
        this.members.add(owner);
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public Player getOwner() {
        return owner;
    }

    public Set<Player> getMembers() {
        return members;
    }

    public Set<Team> getAllies() {
        return allies;
    }

    public Set<Team> getEnemies() {
        return enemies;
    }

    public Set<String> getPvpAllowedPlayers() {
        return pvpAllowedPlayers;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public boolean isAllyPvpEnabled() {
        return allyPvpEnabled;
    }

    public void setAllyPvpEnabled(boolean allyPvpEnabled) {
        this.allyPvpEnabled = allyPvpEnabled;
    }

    public void addMember(Player player) {
        members.add(player);
    }

    public void removeMember(Player player) {
        members.remove(player);
        mods.remove(player);
        admins.remove(player);
        coOwners.remove(player);
    }

    public void addAlly(Team team) {
        allies.add(team);
        enemies.remove(team);
    }

    public void removeAlly(Team team) {
        allies.remove(team);
    }

    public void addEnemy(Team team) {
        enemies.add(team);
        allies.remove(team);
    }

    public void removeEnemy(Team team) {
        enemies.remove(team);
    }

    public void allowPvpWith(Player player) {
        pvpAllowedPlayers.add(player.getUniqueId().toString());
    }

    public boolean canPvpWith(Player player) {
        return pvpAllowedPlayers.contains(player.getUniqueId().toString());
    }

    public boolean isModOrAbove(Player player) {
        return mods.contains(player) || admins.contains(player) || coOwners.contains(player) || owner.equals(player);
    }

    public boolean isAdminOrAbove(Player player) {
        return admins.contains(player) || coOwners.contains(player) || owner.equals(player);
    }

    public boolean isAdmin(Player player) {
        return admins.contains(player);
    }

    public boolean promoteMember(Player player) {
        if (mods.contains(player)) {
            mods.remove(player);
            admins.add(player);
            return true;
        } else if (admins.contains(player)) {
            admins.remove(player);
            coOwners.add(player);
            return true;
        } else if (!coOwners.contains(player)) {
            mods.add(player);
            return true;
        }
        return false;
    }

    public boolean demoteMember(Player player) {
        if (coOwners.contains(player)) {
            coOwners.remove(player);
            admins.add(player);
            return true;
        } else if (admins.contains(player)) {
            admins.remove(player);
            mods.add(player);
            return true;
        } else if (mods.contains(player)) {
            mods.remove(player);
            return true;
        }
        return false;
    }

    public String getRank(Player player) {
        if (coOwners.contains(player)) {
            return "Co-Owner";
        } else if (admins.contains(player)) {
            return "Admin";
        } else if (mods.contains(player)) {
            return "Mod";
        } else if (members.contains(player)) {
            return "Member";
        }
        return "Unknown";
    }

    public void banMember(Player player) {
        bannedMembers.add(player);
    }

    public boolean isBanned(Player player) {
        return bannedMembers.contains(player);
    }

    public void unbanMember(Player player) {
        bannedMembers.remove(player);
    }

    public void transferOwnership(Player player) {
        owner = player;
    }

    public Set<Player> getCoOwners() {
        return coOwners;
    }

    public Set<Player> getAdmins() {
        return admins;
    }

    public Set<Player> getMods() {
        return mods;
    }

    public void disband() {
        for (Player member : members) {
            member.sendMessage(ChatColor.RED + "Your team has been disbanded.");
        }
        members.clear();
        mods.clear();
        admins.clear();
        coOwners.clear();
        bannedMembers.clear();
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }
}
