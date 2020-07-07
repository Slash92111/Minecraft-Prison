package me.canyon.game;

import me.canyon.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

public class World implements Listener {

    private Main plugin;

    public World(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void denyLeafDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }
}
