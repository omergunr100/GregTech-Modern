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

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public final class FTBOwner implements IMachineOwner {

    @Getter
    private Team team;
    @Getter
    private UUID playerUUID;

    @Override
    public void save(CompoundTag tag) {
        if (team != null)
            tag.putUUID("teamUUID", team.getTeamId());
        tag.putUUID("playerUUID", playerUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        try {
            if (tag.contains("teamUUID")) {
                if (FTBTeamsAPI.api().isManagerLoaded()) {
                    this.team = FTBTeamsAPI.api().getManager().getTeamByID(tag.getUUID("teamUUID")).orElse(null);
                } else if (FTBTeamsAPI.api().isClientManagerLoaded()) {
                    this.team = FTBTeamsAPI.api().getClientManager().getTeamByID(tag.getUUID("teamUUID")).orElse(null);
                } else {
                    team = null;
                }
            } else {
                this.team = null;
            }
        } catch (NullPointerException e) {
            this.team = null;
        }

        this.playerUUID = tag.getUUID("playerUUID");
    }

    @Override
    public boolean isPlayerInTeam(Player player) {
        if (player.getUUID().equals(this.playerUUID)) return true;
        return FTBTeamsAPI.api().getManager().arePlayersInSameTeam(player.getUUID(), this.playerUUID);
    }

    @Override
    public boolean isPlayerFriendly(Player player) {
        return team.getRankForPlayer(player.getUUID()).isAllyOrBetter();
    }

    @Override
    public UUID getUUID() {
        return team.getId();
    }

    @Override
    public String getName() {
        return team.getName().getString();
    }

    @Override
    public void displayInfo(List<Component> compList) {
        compList.add(Component.translatable("behavior.portable_scanner.machine_ownership", type().getName()));
        compList.add(Component.translatable("behavior.portable_scanner.team_name", team.getName()));
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
