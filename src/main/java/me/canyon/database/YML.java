package me.canyon.database;

import me.canyon.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class YML {
    private FileConfiguration config;

    public YML(Main plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".yml");

        plugin.saveResource(fileName + ".yml", true);

        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
