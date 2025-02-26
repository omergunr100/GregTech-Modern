package com.gregtechceu.gtceu.common.machine.owner;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.server.ServerLifecycleHooks;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public final class FTBOwner implements IMachineOwner {

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

    public @Nullable Team getPlayerTeam(UUID playerUUID) {
        if (FTBTeamsAPI.api().isManagerLoaded()) {
            return FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(playerUUID).orElse(null);
        } else if (FTBTeamsAPI.api().isClientManagerLoaded()) {
            return FTBTeamsAPI.api().getClientManager().getTeams().stream()
                    .filter(t -> t.getMembers().contains(playerUUID))
                    .findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public @Nullable Team getTeam() {
        return getPlayerTeam(playerUUID);
    }

    @Override
    public boolean isPlayerInTeam(Player player) {
        if (player.getUUID().equals(this.playerUUID)) return true;
        if (FTBTeamsAPI.api().isManagerLoaded()) {
            return FTBTeamsAPI.api().getManager().arePlayersInSameTeam(player.getUUID(), this.playerUUID);
        } else if (FTBTeamsAPI.api().isClientManagerLoaded()) {
            var ownTeam = getPlayerTeam(this.playerUUID);
            if (ownTeam == null) {
                return false;
            }
            var otherTeam = getPlayerTeam(player.getUUID());
            return otherTeam != null && ownTeam.getTeamId().equals(otherTeam.getTeamId());
        } else {
            return true;
        }
    }

    @Override
    public boolean isPlayerFriendly(Player player) {
        var team = getTeam();
        if (team == null) {
            return playerUUID.equals(player.getUUID());
        }
        return team.getRankForPlayer(player.getUUID()).isAllyOrBetter();
    }

    @Override
    public UUID getUUID() {
        var team = getTeam();
        return team != null ? team.getId() : EMPTY;
    }

    @Override
    public String getName() {
        var team = getTeam();
        return team != null ? team.getName().getString() : "Not Available";
    }

    @Override
    public void displayInfo(List<Component> compList) {
        compList.add(Component.translatable("behavior.portable_scanner.machine_ownership", type().getName()));
        compList.add(Component.translatable("behavior.portable_scanner.team_name", getName()));
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
        return MachineOwnerType.FTB;
    }
}
