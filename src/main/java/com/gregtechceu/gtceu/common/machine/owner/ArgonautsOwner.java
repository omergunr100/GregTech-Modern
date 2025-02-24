package com.gregtechceu.gtceu.common.machine.owner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

import earth.terrarium.argonauts.api.guild.Guild;
import earth.terrarium.argonauts.common.handlers.guild.GuildHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public final class ArgonautsOwner implements IMachineOwner {

    @Getter
    private Guild guild;
    @Getter
    private UUID playerUUID;
    private MinecraftServer server;

    @Override
    public void save(CompoundTag tag) {
        tag.putUUID("guildUUID", guild.id());
        tag.putUUID("playerUUID", playerUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        this.playerUUID = tag.getUUID("playerUUID");
        this.server = ServerLifecycleHooks.getCurrentServer();
        var handler = GuildHandler.read(server);
        this.guild = GuildHandler.API.get(server, tag.getUUID("guildUUID"));
    }

    @Override
    public boolean isPlayerInTeam(Player player) {
        if (player.getUUID().equals(this.playerUUID)) return true;
        var otherGuild = GuildHandler.read(server).get(server, player.getUUID());
        return otherGuild != null && otherGuild.equals(this.guild);
    }

    @Override
    public boolean isPlayerFriendly(Player player) {
        return guild.isPublic() || guild.members().isMember(player.getUUID()) ||
                guild.members().isAllied(player.getUUID());
    }

    @Override
    public UUID getUUID() {
        return guild.id();
    }

    @Override
    public String getName() {
        return guild.displayName().getString();
    }

    @Override
    public void displayInfo(List<Component> compList) {
        compList.add(Component.translatable("behavior.portable_scanner.machine_ownership", type().getName()));
        compList.add(Component.translatable("behavior.portable_scanner.guild_name", guild.displayName().getString()));
        var serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
        final String[] playerName = new String[1];
        boolean isOnline;
        if (serverPlayer != null) {
            playerName[0] = serverPlayer.getDisplayName().getString();
            isOnline = true;
        } else {
            var cache = ServerLifecycleHooks.getCurrentServer().getProfileCache();
            if (cache != null) {
                cache.get(playerUUID).ifPresent(value -> playerName[0] = value.getName());
            }
            isOnline = false;
        }
        compList.add(Component.translatable("behavior.portable_scanner.player_name", playerName[0], isOnline));
    }

    @Override
    public MachineOwnerType type() {
        return MachineOwnerType.ARGONAUTS;
    }
}
