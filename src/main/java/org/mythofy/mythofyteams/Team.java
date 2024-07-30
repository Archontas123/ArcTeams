package org.mythofy.mythofyteams;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class Team {

    private String name;
    private String tag;
    private Player owner;
    private Set<Player> members;
    private Set<Player> bannedMembers;
    private Set<Team> allies;
    private Set<Team> enemies;
    private boolean pvpEnabled;
    private boolean allyPvpEnabled;
    private Location home;
    private Map<Player, String> ranks;
    private Map<Player, Long> allowedPvpPlayers; // Added allowedPvpPlayers map

    public Team(String name, String tag, Player owner) {
        this.name = name;
        this.tag = tag;
        this.owner = owner;
        this.members = new HashSet<>();
        this.bannedMembers = new HashSet<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
        this.pvpEnabled = false;
        this.allyPvpEnabled = false;
        this.home = null;
        this.ranks = new HashMap<>();
        this.allowedPvpPlayers = new HashMap<>(); // Initialize allowedPvpPlayers
        if (owner != null) {
            this.members.add(owner);
            this.ranks.put(owner, "owner");
        }
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

    public void setOwner(Player owner) {
        this.owner = owner;
        this.ranks.put(owner, "owner");
    }

    public Set<Player> getMembers() {
        return members;
    }

    public Set<Player> getBannedMembers() {
        return bannedMembers;
    }

    public Set<Team> getAllies() {
        return allies;
    }

    public Set<Team> getEnemies() {
        return enemies;
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

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public void addMember(Player player) {
        members.add(player);
        ranks.put(player, "member");
    }

    public void removeMember(Player player) {
        members.remove(player);
        ranks.remove(player);
    }

    public void banMember(Player player) {
        bannedMembers.add(player);
    }

    public void unbanMember(Player player) {
        bannedMembers.remove(player);
    }

    public boolean isBanned(Player player) {
        return bannedMembers.contains(player);
    }

    public void addAlly(Team team) {
        allies.add(team);
    }

    public void removeAlly(Team team) {
        allies.remove(team);
    }

    public void addEnemy(Team team) {
        enemies.add(team);
    }

    public void removeEnemy(Team team) {
        enemies.remove(team);
    }

    public boolean isModOrAbove(Player player) {
        return ranks.containsKey(player) && (ranks.get(player).equals("mod") || isAdminOrAbove(player));
    }

    public boolean isAdminOrAbove(Player player) {
        return ranks.containsKey(player) && (ranks.get(player).equals("admin") || ranks.get(player).equals("coowner") || isOwner(player));
    }

    public boolean isAdmin(Player player) {
        return ranks.containsKey(player) && ranks.get(player).equals("admin");
    }

    public boolean isOwner(Player player) {
        return ranks.containsKey(player) && ranks.get(player).equals("owner");
    }

    public Set<Player> getCoOwners() {
        Set<Player> coOwners = new HashSet<>();
        for (Map.Entry<Player, String> entry : ranks.entrySet()) {
            if (entry.getValue().equals("coowner")) {
                coOwners.add(entry.getKey());
            }
        }
        return coOwners;
    }

    public Set<Player> getAdmins() {
        Set<Player> admins = new HashSet<>();
        for (Map.Entry<Player, String> entry : ranks.entrySet()) {
            if (entry.getValue().equals("admin")) {
                admins.add(entry.getKey());
            }
        }
        return admins;
    }

    public Set<Player> getMods() {
        Set<Player> mods = new HashSet<>();
        for (Map.Entry<Player, String> entry : ranks.entrySet()) {
            if (entry.getValue().equals("mod")) {
                mods.add(entry.getKey());
            }
        }
        return mods;
    }

    public boolean promoteMember(Player player) {
        String currentRank = ranks.get(player);
        if (currentRank.equals("member")) {
            ranks.put(player, "mod");
            return true;
        } else if (currentRank.equals("mod")) {
            ranks.put(player, "admin");
            return true;
        } else if (currentRank.equals("admin")) {
            ranks.put(player, "coowner");
            return true;
        } else {
            return false;
        }
    }

    public boolean demoteMember(Player player) {
        String currentRank = ranks.get(player);
        if (currentRank.equals("coowner")) {
            ranks.put(player, "admin");
            return true;
        } else if (currentRank.equals("admin")) {
            ranks.put(player, "mod");
            return true;
        } else if (currentRank.equals("mod")) {
            ranks.put(player, "member");
            return true;
        } else {
            return false;
        }
    }

    public String getRank(Player player) {
        return ranks.get(player);
    }

    public void setRank(Player player, String rank) {
        ranks.put(player, rank);
    }

    public void transferOwnership(Player newOwner) {
        if (owner != null) {
            ranks.put(owner, "member");
        }
        owner = newOwner;
        ranks.put(newOwner, "owner");
    }

    public void disband() {
        for (Player member : members) {
            member.sendMessage(ChatColor.RED + "Your team has been disbanded.");
        }
        members.clear();
        allies.clear();
        enemies.clear();
        ranks.clear();
        home = null;
    }

    public void allowPvpWith(Player player) {
        allowedPvpPlayers.put(player, System.currentTimeMillis());
    }

    public boolean canPvpWith(Player player) {
        return allowedPvpPlayers.containsKey(player);
    }
}
