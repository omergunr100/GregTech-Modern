package com.gregtechceu.gtceu.common.machine.owner;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public sealed interface IMachineOwner permits PlayerOwner, ArgonautsOwner, FTBOwner {

    UUID EMPTY = new UUID(0, 0);

    void save(CompoundTag tag);

    void load(CompoundTag tag);

    MachineOwnerType type();

    void displayInfo(List<Component> compList);

    static @Nullable IMachineOwner create(CompoundTag tag) {
        MachineOwnerType type = MachineOwnerType.VALUES[tag.getInt("type")];
        if (!type.isAvailable()) {
            GTCEu.LOGGER.warn("Machine ownership system: {} is not available", type.name());
            return null;
        }
        IMachineOwner owner = switch (type) {
            case PLAYER -> new PlayerOwner();
            case FTB -> new FTBOwner();
            case ARGONAUTS -> new ArgonautsOwner();
        };
        owner.load(tag);
        return owner;
    }

    default CompoundTag write() {
        var tag = new CompoundTag();
        tag.putInt("type", type().ordinal());
        save(tag);
        return tag;
    }

    boolean isPlayerInTeam(Player player);

    boolean isPlayerFriendly(Player player);

    static boolean canOpenOwnerMachine(Player player, MetaMachine machine) {
        if (!ConfigHolder.INSTANCE.machines.onlyOwnerGUI) return true;
        if (player.hasPermissions(ConfigHolder.INSTANCE.machines.ownerOPBypass)) return true;
        var owner = machine.getOwner();
        if (owner == null) return true;
        return owner.isPlayerInTeam(player) || owner.isPlayerFriendly(player);
    }

    static boolean canBreakOwnerMachine(Player player, MetaMachine machine) {
        if (!ConfigHolder.INSTANCE.machines.onlyOwnerBreak) return true;
        if (player.hasPermissions(ConfigHolder.INSTANCE.machines.ownerOPBypass)) return true;
        var owner = machine.getOwner();
        if (owner == null) return true;
        return owner.isPlayerInTeam(player);
    }

    UUID getUUID();

    String getName();

    enum MachineOwnerType {

        PLAYER(() -> true, "Player"),
        FTB(GTCEu.Mods::isFTBTeamsLoaded, "FTB Teams"),
        ARGONAUTS(GTCEu.Mods::isArgonautsLoaded, "Argonauts Guild");

        public static final MachineOwnerType[] VALUES = values();

        private BooleanSupplier availabilitySupplier;
        private boolean available;

        @Getter
        private final String name;

        MachineOwnerType(BooleanSupplier availabilitySupplier, String name) {
            this.availabilitySupplier = availabilitySupplier;
            this.name = name;
        }

        public boolean isAvailable() {
            if (availabilitySupplier != null) {
                this.available = availabilitySupplier.getAsBoolean();
                this.availabilitySupplier = null;
            }
            return available;
        }
    }
}
