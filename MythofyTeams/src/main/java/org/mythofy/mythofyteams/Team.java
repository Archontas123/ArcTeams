package org.mythofy.mythofyteams;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Team {
    private final String name;
    private final String tag;
    private final Player owner;
    private final Set<Player> members = new HashSet<>();

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

    public void addMember(Player player) {
        members.add(player);
    }

    public void removeMember(Player player) {
        members.remove(player);
    }
}
