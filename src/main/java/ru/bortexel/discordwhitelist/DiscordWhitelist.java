package ru.bortexel.discordwhitelist;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.TimeArgumentType;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import ru.bortexel.discordwhitelist.config.BortexelConfig;
import ru.bortexel.discordwhitelist.config.WhitelistConfig;
import ru.ruscalworld.bortexel4j.Bortexel4J;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DiscordWhitelist implements ModInitializer {
    private static DiscordWhitelist instance;

    private final OkHttpClient httpClient = new OkHttpClient();
    private WhitelistConfig whitelistConfig;
    private BortexelConfig bortexelConfig;
    private WhiteList whiteList;
    private Bortexel4J client;
    private JDA jda;

    @Override
    public void onInitialize() {
        try {
            // Initialize configs
            this.setWhitelistConfig(new WhitelistConfig());
            this.setBortexelConfig(new BortexelConfig());
            this.setClient(Bortexel4J.login(this.getBortexelConfig().getApiToken(), this.getBortexelConfig().getApiUrl(), this.getHttpClient()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Initialize Discord bot
            JDABuilder builder = JDABuilder.createDefault(this.getBortexelConfig().getBotToken());
            builder.setCompression(Compression.NONE);
            builder.setStatus(OnlineStatus.INVISIBLE);
            builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            JDA jda = builder.build();
            jda.awaitReady();
            this.setJDA(jda);
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }

        // Initialize whitelist
        this.setWhiteList(new WhiteList(this));

        // Update whitelist each 5 minutes
        Timer timer = new Timer();
        timer.schedule(this.getWhiteList().makeUpdater(), 0, 10 * 1000);

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            // Cancel update task
            timer.cancel();

            // Shutdown OkHttp
            OkHttpClient httpClient = this.getHttpClient();
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();

            try {
                Cache cache = httpClient.cache();
                if (cache != null) cache.close();
            } catch (IOException ignored) { }

            // Shutdown JDA
            this.getJDA().shutdown();
            this.setJDA(null);
        });

        instance = this;
    }

    public BortexelConfig getBortexelConfig() {
        return bortexelConfig;
    }

    public void setBortexelConfig(BortexelConfig bortexelConfig) {
        this.bortexelConfig = bortexelConfig;
    }

    public Bortexel4J getClient() {
        return client;
    }

    public void setClient(Bortexel4J client) {
        this.client = client;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    public JDA getJDA() {
        return jda;
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    public static DiscordWhitelist getInstance() {
        if (instance == null) throw new IllegalStateException("DiscordWhitelist has not initialized yet");
        return instance;
    }

    public WhitelistConfig getWhitelistConfig() {
        return whitelistConfig;
    }

    public void setWhitelistConfig(WhitelistConfig whitelistConfig) {
        this.whitelistConfig = whitelistConfig;
    }

    public WhiteList getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(WhiteList whiteList) {
        this.whiteList = whiteList;
    }
}
