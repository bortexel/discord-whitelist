package ru.bortexel.discordwhitelist;

import com.mojang.authlib.GameProfile;
import io.netty.util.internal.ConcurrentSet;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bortexel.discordwhitelist.config.WhitelistConfig;
import ru.ruscalworld.bortexel4j.models.account.Account;
import ru.ruscalworld.bortexel4j.models.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;

public class WhiteList {
    private static final Logger logger = LogManager.getLogger();

    private final DiscordWhitelist mod;
    private final List<Role> whitelistedRoles = new ArrayList<>();
    private final ConcurrentSet<Entry> whitelistedMembers = new ConcurrentSet<>();

    public WhiteList(DiscordWhitelist mod) {
        this.mod = mod;

        List<String> allowedRoles = mod.getWhitelistConfig().getAllowedRoles();
        Guild guild = this.getMainGuild();

        // Fill list of whitelisted roles
        for (String roleID : allowedRoles) {
            Role role = guild.getRoleById(roleID);
            if (role == null) continue;
            this.getWhitelistedRoles().add(role);
        }
    }

    public Updater makeUpdater() {
        return new Updater(this);
    }

    public void update() {
        this.getMainGuild().loadMembers().get();
        List<Member> members = new ArrayList<>();
        for (Role role : this.getWhitelistedRoles()) members.addAll(this.getMainGuild().getMembersWithRoles(role));

        // Remove members that are no longer whitelisted
        for (Entry entry : this.getWhitelistedMembers()) {
            boolean found = false;

            for (Member member : members) if (member.getId().equals(entry.getDiscordID())) {
                found = true;
                break;
            }

            if (found) continue;
            this.removePlayer(entry.getDiscordID());
            logger.info("Removing {} from the whitelist", entry.getDiscordID());
        }

        // Add members that are whitelisted now
        for (Member member : members) {
            if (this.isWhitelisted(member)) continue;
            try {
                Account account = Account.getByDiscordID(member.getId(), this.getMod().getClient()).execute();
                Account.AccountUsers users = account.getUsers(this.getMod().getClient()).execute();
                for (User user : users.getUsers()) {
                    this.addPlayer(member.getId(), user.getUUID());
                    logger.info("Adding {} ({}) to the whitelist", user.getUsername(), member.getUser().getAsTag());
                }
            } catch (Exception e) {
                logger.warn("Unable to add {} to whitelist", member.getUser().getAsTag(), e);
            }
        }
    }

    public synchronized void addPlayer(String discordID, UUID uuid) {
        this.getWhitelistedMembers().add(new Entry(discordID, uuid));
    }

    public synchronized void removePlayer(String discordID) {
        this.getWhitelistedMembers().removeIf(entry -> entry.getDiscordID().equalsIgnoreCase(discordID));
    }

    public boolean isWhitelisted(Member member) {
        for (Entry entry : this.getWhitelistedMembers()) if (entry.getDiscordID().equals(member.getId())) return true;
        return false;
    }

    public boolean isWhitelisted(UUID uuid) {
        for (Entry entry : this.getWhitelistedMembers()) if (entry.getUUID().equals(uuid)) return true;
        return false;
    }

    public boolean isWhitelisted(GameProfile profile) {
        return this.isWhitelisted(profile.getId());
    }

    public Guild getMainGuild() {
        WhitelistConfig config = this.getMod().getWhitelistConfig();
        return this.getMod().getJDA().getGuildById(config.getGuildID());
    }

    private ConcurrentSet<Entry> getWhitelistedMembers() {
        return whitelistedMembers;
    }

    public DiscordWhitelist getMod() {
        return mod;
    }

    public List<Role> getWhitelistedRoles() {
        return whitelistedRoles;
    }

    public static class Entry {
        private final String discordID;
        private final UUID uuid;

        public Entry(String discordID, UUID uuid) {
            this.discordID = discordID;
            this.uuid = uuid;
        }

        public String getDiscordID() {
            return discordID;
        }

        public UUID getUUID() {
            return uuid;
        }
    }

    public static class Updater extends TimerTask {
        private final WhiteList whiteList;

        public Updater(WhiteList whiteList) {
            this.whiteList = whiteList;
        }

        @Override
        public void run() {
            this.getWhiteList().update();
        }

        public WhiteList getWhiteList() {
            return whiteList;
        }
    }
}
