package com.gregtechceu.gtceu.common.machine.owner;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

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
        return team != null ? team.getName().getString() :
                Component.translatable("gtceu.tooltip.status.trinary.unknown").getString();
    }

    @Override
    public void displayInfo(List<Component> compList) {
        compList.add(Component.translatable("behavior.portable_scanner.machine_ownership", type().getName()));
        compList.add(Component.translatable("behavior.portable_scanner.team_name", getName()));
        IMachineOwner.displayPlayerInfo(compList, playerUUID);
    }

    @Override
    public MachineOwnerType type() {
        return MachineOwnerType.FTB;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FTBOwner that)) return false;

        return playerUUID.equals(that.playerUUID);
    }

    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
}
