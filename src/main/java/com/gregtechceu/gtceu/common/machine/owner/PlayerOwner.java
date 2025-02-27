package com.gregtechceu.gtceu.common.machine.owner;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.server.ServerLifecycleHooks;

import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
public final class PlayerOwner implements IMachineOwner {

    private UUID playerUUID;

    public PlayerOwner(UUID player) {
        this.playerUUID = player;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putUUID("UUID", playerUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        this.playerUUID = tag.getUUID("UUID");
    }

    @Override
    public boolean isPlayerInTeam(Player player) {
        return true;
    }

    @Override
    public boolean isPlayerFriendly(Player player) {
        return true;
    }

    @Override
    public UUID getUUID() {
        return playerUUID;
    }

    @Override
    public String getName() {
        return UsernameCache.getLastKnownUsername(playerUUID);
    }

    @Override
    public void displayInfo(List<Component> compList) {
        compList.add(Component.translatable("behavior.portable_scanner.machine_ownership", type().getName()));
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
        return MachineOwnerType.PLAYER;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PlayerOwner that)) return false;

        return playerUUID.equals(that.playerUUID);
    }

    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
}
