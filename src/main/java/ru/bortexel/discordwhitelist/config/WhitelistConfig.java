package ru.bortexel.discordwhitelist.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class WhitelistConfig {
    private final List<String> allowedRoles;
    private final String guildID;

    public WhitelistConfig() throws IOException {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("whitelist.properties");
        Properties properties = new Properties();
        properties.load(Files.newInputStream(path));

        String[] roles = properties.getProperty("allowed-roles", "").split(", ");
        this.allowedRoles = Arrays.asList(roles);

        this.guildID = properties.getProperty("main-guild", "");
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public String getGuildID() {
        return guildID;
    }
}
