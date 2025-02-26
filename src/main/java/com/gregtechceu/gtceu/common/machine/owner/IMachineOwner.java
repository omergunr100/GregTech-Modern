package com.gregtechceu.gtceu.common.machine.owner;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public sealed interface IMachineOwner permits PlayerOwner, ArgonautsOwner, FTBOwner {

    UUID EMPTY = new UUID(0, 0);
    Map<UUID, IMachineOwner> CACHE = new Object2ObjectOpenHashMap<>();
    Map<UUID, PlayerOwner> PLAYER_OWNERS = new Object2ObjectOpenHashMap<>();

    void save(CompoundTag tag);

    void load(CompoundTag tag);

    MachineOwnerType type();

    void displayInfo(List<Component> compList);

    static @Nullable IMachineOwner getOwner(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        if (CACHE.containsKey(playerUUID)) {
            return CACHE.get(playerUUID);
        }
        IMachineOwner owner;
        if (IMachineOwner.MachineOwnerType.FTB.isAvailable()) {
            owner = new FTBOwner(playerUUID);
        } else if (IMachineOwner.MachineOwnerType.ARGONAUTS.isAvailable()) {
            owner = new ArgonautsOwner(playerUUID);
        } else {
            owner = getPlayerOwner(playerUUID);
        }
        CACHE.put(playerUUID, owner);
        return owner;
    }

    static @Nullable PlayerOwner getPlayerOwner(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        if (PLAYER_OWNERS.containsKey(playerUUID)) {
            return PLAYER_OWNERS.get(playerUUID);
        }
        var owner = new PlayerOwner(playerUUID);
        PLAYER_OWNERS.put(playerUUID, owner);
        return owner;
    }

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
