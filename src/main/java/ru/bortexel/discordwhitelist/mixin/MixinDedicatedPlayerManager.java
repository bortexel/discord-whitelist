package ru.bortexel.discordwhitelist.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import net.minecraft.server.dedicated.DedicatedPlayerManager;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.WorldSaveHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.bortexel.discordwhitelist.DiscordWhitelist;

import java.net.SocketAddress;

@Mixin(DedicatedPlayerManager.class)
public abstract class MixinDedicatedPlayerManager extends PlayerManager {
    public MixinDedicatedPlayerManager(MinecraftServer server, DynamicRegistryManager.Impl registryManager, WorldSaveHandler saveHandler, int maxPlayers) {
        super(server, registryManager, saveHandler, maxPlayers);
    }

    /**
     * @reason Use discord whitelist with vanilla one
     * @author DiscordWhitelist
     */
    @Overwrite
    public boolean isWhitelisted(GameProfile profile) {
        return !this.isWhitelistEnabled() ||
                ((MixinServerConfigList) this.getOpList()).contains(profile) ||
                ((MixinServerConfigList) this.getWhitelist()).contains(profile) ||
                DiscordWhitelist.getInstance().getWhiteList().isWhitelisted(profile);
    }
}
