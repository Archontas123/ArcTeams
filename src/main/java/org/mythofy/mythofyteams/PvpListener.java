package org.mythofy.mythofyteams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvpListener implements Listener {
    private final MythofyTeams plugin;
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, UUID> lastHitTarget = new HashMap<>();
    private final Map<UUID, String> lastHitStatus = new HashMap<>();
    private static final long COOLDOWN = 300000; // 5 minutes in milliseconds

    public PvpListener(MythofyTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        Team damagedTeam = plugin.getTeamCommand().getPlayerTeam(damaged);
        Team damagerTeam = plugin.getTeamCommand().getPlayerTeam(damager);

        long currentTime = System.currentTimeMillis();
        UUID damagerUUID = damager.getUniqueId();
        UUID damagedUUID = damaged.getUniqueId();
        String status = "neutral";

        if (damagedTeam != null && damagerTeam != null) {
            if (damagedTeam.equals(damagerTeam)) {
                status = "teammate";
            } else if (damagerTeam.getAllies().contains(damagedTeam)) {
                status = "ally";
            } else if (damagerTeam.getEnemies().contains(damagedTeam)) {
                status = "enemy";
            }
        }

        if (!lastHitTime.containsKey(damagerUUID) || !lastHitTarget.containsKey(damagerUUID) ||
                currentTime - lastHitTime.get(damagerUUID) > COOLDOWN || !lastHitTarget.get(damagerUUID).equals(damagedUUID) ||
                !lastHitStatus.get(damagerUUID).equals(status)) {

            if (status.equals("teammate")) {
                damager.sendMessage(ChatColor.GREEN + "This player is your teammate.");
            } else if (status.equals("ally")) {
                damager.sendMessage(ChatColor.BLUE + "This player is your ally.");
            } else if (status.equals("enemy")) {
                damager.sendMessage(ChatColor.RED + "This player is your enemy.");
            } else {
                damager.sendMessage(ChatColor.WHITE + "This player is neutral.");
            }

            lastHitTime.put(damagerUUID, currentTime);
            lastHitTarget.put(damagerUUID, damagedUUID);
            lastHitStatus.put(damagerUUID, status);
        }

        if (damagedTeam != null && damagerTeam != null) {
            if (damagedTeam.equals(damagerTeam)) {
                // Same team
                if (!damagedTeam.isPvpEnabled() && !damagedTeam.canPvpWith(damager)) {
                    event.setCancelled(true);
                }
            } else if (damagedTeam.getAllies().contains(damagerTeam)) {
                // Ally team
                if (!damagedTeam.isAllyPvpEnabled() && !damagedTeam.canPvpWith(damager)) {
                    event.setCancelled(true);
                }
            } else if (damagedTeam.getEnemies().contains(damagerTeam)) {
                // Enemy team
                // Allow PvP
            } else {
                // Neutral teams or no relation
                // Allow PvP
            }
        } else {
            // At least one player is not in a team
            // Allow PvP
        }

        // Allow mutual PvP if hit
        if (!event.isCancelled()) {
            if (damagedTeam != null) {
                damagedTeam.allowPvpWith(damager);
            }
            if (damagerTeam != null) {
                damagerTeam.allowPvpWith(damaged);
            }
        }
    }
}
