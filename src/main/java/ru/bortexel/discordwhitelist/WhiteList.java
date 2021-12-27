package ru.bortexel.discordwhitelist;

import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bortexel.discordwhitelist.config.WhitelistConfig;
import ru.ruscalworld.bortexel4j.models.account.Account;
import ru.ruscalworld.bortexel4j.models.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class WhiteList {
    private static final Logger logger = LogManager.getLogger();

    private final DiscordWhitelist mod;
    private final List<Role> whitelistedRoles = new ArrayList<>();
    private final ConcurrentHashMap<String, String> whitelistedMembers = new ConcurrentHashMap<>();

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
        for (String memberID : this.getWhitelistedMembers().keySet()) {
            boolean found = false;

            for (Member member : members) if (member.getId().equals(memberID)) {
                found = true;
                break;
            }

            if (found) continue;
            String name = this.removePlayer(memberID);
            logger.info("Removing {} from the whitelist", name);
        }

        // Add members that are whitelisted now
        for (Member member : members) {
            if (this.getWhitelistedMembers().containsKey(member.getId())) continue;
            Account account = Account.getByDiscordID(member.getId(), this.getMod().getClient()).execute();
            Account.AccountUsers users = account.getUsers(this.getMod().getClient()).execute();
            for (User user : users.getUsers()) {
                this.addPlayer(member.getId(), user.getUsername());
                logger.info("Adding {} ({}) to the whitelist", user.getUsername(), member.getUser().getAsTag());
            }
        }
    }

    public synchronized void addPlayer(String discordID, String username) {
        this.getWhitelistedMembers().put(discordID, username);
    }

    public synchronized String removePlayer(String discordID) {
        return this.getWhitelistedMembers().remove(discordID);
    }

    public boolean isWhitelisted(String name) {
        return this.getWhitelistedMembers().containsValue(name);
    }

    public boolean isWhitelisted(GameProfile profile) {
        return this.isWhitelisted(profile.getName());
    }

    public Guild getMainGuild() {
        WhitelistConfig config = this.getMod().getWhitelistConfig();
        return this.getMod().getJDA().getGuildById(config.getGuildID());
    }

    private ConcurrentHashMap<String, String> getWhitelistedMembers() {
        return whitelistedMembers;
    }

    public DiscordWhitelist getMod() {
        return mod;
    }

    public List<Role> getWhitelistedRoles() {
        return whitelistedRoles;
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
