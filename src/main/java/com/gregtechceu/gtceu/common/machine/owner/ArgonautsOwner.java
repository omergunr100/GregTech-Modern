package com.gregtechceu.gtceu.common.machine.owner;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.server.ServerLifecycleHooks;

import earth.terrarium.argonauts.api.client.guild.GuildClientApi;
import earth.terrarium.argonauts.api.guild.Guild;
import earth.terrarium.argonauts.api.guild.GuildApi;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings({ "UnstableApiUsage", "removal", "deprecation" })
@NoArgsConstructor
@AllArgsConstructor
public final class ArgonautsOwner implements IMachineOwner {

    @Getter
    private UUID playerUUID;

    @Override
    public void save(CompoundTag tag) {
        tag.putUUID("playerUUID", playerUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        this.playerUUID = tag.getUUID("playerUUID");
    }

    public @Nullable Guild getPlayerGuild(UUID playerUUID) {
        if (GTCEu.isClientThread()) {
            return GuildClientApi.API.getPlayerGuild(playerUUID);
        } else {
            return GuildApi.API.getPlayerGuild(ServerLifecycleHooks.getCurrentServer(), playerUUID);
        }
    }

    public @Nullable Guild getGuild() {
        return getPlayerGuild(playerUUID);
    }

    @Override
    public boolean isPlayerInTeam(Player player) {
        if (player.getUUID().equals(this.playerUUID)) return true;
        var otherGuild = getPlayerGuild(player.getUUID());
        return otherGuild != null && otherGuild.equals(getGuild());
    }

    @Override
    public boolean isPlayerFriendly(Player player) {
        var guild = getGuild();
        if (guild == null) {
            return playerUUID.equals(player.getUUID());
        }
        return guild.isPublic() || guild.members().isMember(player.getUUID()) ||
                guild.members().isAllied(player.getUUID());
    }

    @Override
    public UUID getUUID() {
        var guild = getGuild();
        return guild != null ? guild.id() : EMPTY;
    }

    @Override
    public String getName() {
        var guild = getGuild();
        return guild != null ? guild.displayName().getString() : "Not Available";
    }

    @Override
    public void displayInfo(List<Component> compList) {
        compList.add(Component.translatable("behavior.portable_scanner.machine_ownership", type().getName()));
        compList.add(
                Component.translatable("behavior.portable_scanner.guild_name", getName()));
        final var playerName = UsernameCache.getLastKnownUsername(playerUUID);
        String online;
        if (GTCEu.isClientThread()) {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                online = String.valueOf(connection.getOnlinePlayerIds().contains(playerUUID));
            } else {
                online = "Not Available";
            }
        } else {
            online = String
                    .valueOf(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID) != null);
        }
        compList.add(Component.translatable("behavior.portable_scanner.player_name", playerName, online));
    }

    @Override
    public MachineOwnerType type() {
        return MachineOwnerType.ARGONAUTS;
    }
}
