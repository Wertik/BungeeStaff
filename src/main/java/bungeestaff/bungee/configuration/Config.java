package bungeestaff.bungee.configuration;

import bungeestaff.bungee.BungeeStaffPlugin;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class Config {

    private final BungeeStaffPlugin plugin;

    // Name with extension
    @Getter
    private final String name;

    @Getter
    private final File file;
    @Getter
    private final ConfigurationProvider configurationProvider;

    @Getter
    private Configuration configuration;

    public Config(BungeeStaffPlugin plugin, String name) {
        this.plugin = plugin;

        String finalName = name.contains(".yml") ? name : name.concat(".yml");
        this.name = finalName;
        this.file = new File(plugin.getDataFolder(), finalName);

        this.configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
    }

    public boolean load() {

        if (!file.exists()) {

            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
                return false;

            try {
                InputStream in = plugin.getResourceAsStream(name);
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                plugin.getProxy().getLogger().severe("Could not create file " + name);
                e.printStackTrace();
                return false;
            }
        }

        try {
            configuration = configurationProvider.load(file);
        } catch (IOException e) {
            plugin.getProxy().getLogger().severe("Could not load " + name);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void delete() {

        if (!file.exists())
            return;

        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        delete();
        load();
    }

    public boolean save() {
        try {
            configurationProvider.save(configuration, file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
