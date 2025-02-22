package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.common.machine.owner.ArgonautsOwner;
import com.gregtechceu.gtceu.common.machine.owner.FTBOwner;
import com.gregtechceu.gtceu.common.machine.owner.IMachineOwner;
import com.gregtechceu.gtceu.common.machine.owner.PlayerOwner;
import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import earth.terrarium.argonauts.api.client.guild.GuildClientApi;
import earth.terrarium.argonauts.api.guild.Guild;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@NoArgsConstructor
public class MachineOwnerPayload  extends ObjectTypedPayload<IMachineOwner> {

    @Override
    public void writePayload(FriendlyByteBuf buf) {
        buf.writeVarInt(payload.type().ordinal());
        buf.writeUUID(payload.getUUID());
        if(payload instanceof ArgonautsOwner argoPayload) {
            buf.writeUUID(argoPayload.getPlayerUUID());
        }
        else if(payload instanceof FTBOwner ftbPayload) {
            buf.writeUUID(ftbPayload.getPlayerUUID());
        }
    }

    @Override
    public void readPayload(FriendlyByteBuf buf) {
        IMachineOwner.MachineOwnerType type = IMachineOwner.MachineOwnerType.VALUES[buf.readVarInt()];
        UUID uuid = buf.readUUID();
        if(type == IMachineOwner.MachineOwnerType.ARGONAUTS) {
            UUID playerUUID = buf.readUUID();
            Guild guild = GuildClientApi.API.get(uuid);
            payload = new ArgonautsOwner(guild, playerUUID);
        } else if (type == IMachineOwner.MachineOwnerType.FTB) {
            UUID playerUUID = buf.readUUID();
            Team team = FTBTeamsAPI.api().getManager().getTeamByID(uuid).get();
            payload = new FTBOwner(team, playerUUID);
        }
        else if(type == IMachineOwner.MachineOwnerType.PLAYER) {
            payload = new PlayerOwner(uuid);
        }
    }

    @Nullable
    @Override
    public Tag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("owner", payload.write());
        return tag;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if(tag instanceof CompoundTag cTag && cTag.contains("owner"))
            payload = IMachineOwner.create(cTag.getCompound("owner"));
    }
}
