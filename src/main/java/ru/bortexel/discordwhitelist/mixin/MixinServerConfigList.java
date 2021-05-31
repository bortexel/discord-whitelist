package ru.bortexel.discordwhitelist.mixin;

import net.minecraft.server.ServerConfigList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerConfigList.class)
public interface MixinServerConfigList {
    @Invoker("contains")
    <K> boolean contains(K profile);
}
