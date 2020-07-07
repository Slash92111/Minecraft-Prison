package me.canyon.game.player.listener;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Attack implements Listener {

    private Main plugin;

    public Attack(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerAttack(EntityDamageByEntityEvent event) {
        // Verify that both entity were players
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player))
            return;

        Player damager = (Player) event.getDamager();
        Player damaged = (Player) event.getEntity();

        // Get their playerData
        PlayerData damagerData = plugin.getPlayerData(damager);
        PlayerData damagedData = plugin.getPlayerData(damaged);

        // Verify both players are in a gang
        if (damagerData.getGangID() != 0 || damagedData.getGangID() != 0) {
            // If both players are in the same gang, cancel the damage event
            if (damagerData.getGangID() == damagedData.getGangID())
                event.setCancelled(true);

        }
    }
}
