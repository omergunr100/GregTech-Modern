package com.gregtechceu.gtceu.api.blockentity;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.*;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.misc.EnergyInfoProviderList;
import com.gregtechceu.gtceu.api.misc.LaserContainerList;
import com.gregtechceu.gtceu.api.pipenet.longdistance.ILDEndpoint;
import com.gregtechceu.gtceu.client.renderer.GTRendererProvider;
import com.gregtechceu.gtceu.common.datafixers.TagFixer;
import com.gregtechceu.gtceu.common.machine.owner.*;
import com.gregtechceu.gtceu.common.pipelike.fluidpipe.longdistance.LDFluidEndpointMachine;
import com.gregtechceu.gtceu.common.pipelike.item.longdistance.LDItemEndpointMachine;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.server.ServerLifecycleHooks;

import appeng.api.networking.IInWorldGridNodeHost;
import appeng.capabilities.Capabilities;
import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import dev.ftb.mods.ftbteams.api.Team;
import earth.terrarium.argonauts.api.guild.Guild;
import earth.terrarium.argonauts.common.handlers.guild.GuildHandler;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author KilaBash
 * @date 2023/2/17
 * @implNote MetaMachineBlockEntity
 */
public class MetaMachineBlockEntity extends BlockEntity implements IMachineBlockEntity {

    public final MultiManagedStorage managedStorage = new MultiManagedStorage();
    @Getter
    public final MetaMachine metaMachine;
    @Setter
    @Getter
    @Persisted
    @Nullable
    private UUID ownerUUID;
    private final long offset = GTValues.RNG.nextInt(20);

    protected MetaMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.metaMachine = getDefinition().createMetaMachine(this);
    }

    public static MetaMachineBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos,
                                                           BlockState blockState) {
        return new MetaMachineBlockEntity(type, pos, blockState);
    }

    public static void onBlockEntityRegister(BlockEntityType<BlockEntity> metaMachineBlockEntityBlockEntityType) {}

    @Override
    public MultiManagedStorage getRootStorage() {
        return managedStorage;
    }

    @Override
    public boolean triggerEvent(int id, int para) {
        if (id == 1) { // chunk re render
            if (level != null && level.isClientSide) {
                scheduleRenderUpdate();
            }
            return true;
        }
        return false;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        metaMachine.onUnload();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        metaMachine.onLoad();
    }

    @Override
    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        return metaMachine.shouldRenderGrid(player, pos, state, held, toolTypes);
    }

    @Override
    public ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                    Direction side) {
        return metaMachine.sideTips(player, pos, state, toolTypes, side);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        var result = getCapability(getMetaMachine(), cap, side);
        return result == null ? super.getCapability(cap, side) : result;
    }

    @Override
    public void setChanged() {
        if (getLevel() != null) {
            getLevel().blockEntityChanged(getBlockPos());
        }
    }

    public IMachineOwner getOwner() {
        var uuid = getOwnerUUID();
        if (uuid == null) {
            return null;
        }
        if (IMachineOwner.MachineOwnerType.FTB.isAvailable()) {
            Optional<Team> team = FTBTeamsAPIImpl.INSTANCE.getManager().getTeamForPlayerID(uuid);
            if (team.isPresent()) {
                return new FTBOwner(team.get(), uuid);
            }
        }
        if (IMachineOwner.MachineOwnerType.ARGONAUTS.isAvailable()) {
            var server = ServerLifecycleHooks.getCurrentServer();
            Guild guild = GuildHandler.read(server).get(server.getPlayerList().getPlayer(uuid));
            if (guild != null) {
                return new ArgonautsOwner(guild, uuid, server);
            }
        }
        return new PlayerOwner(uuid);
    }

    public PlayerOwner getPlayerOwner() {
        if (ownerUUID == null) {
            return null;
        }
        return new PlayerOwner(ownerUUID);
    }

    @Nullable
    public static <T> LazyOptional<T> getCapability(MetaMachine machine, @NotNull Capability<T> cap,
                                                    @Nullable Direction side) {
        if (cap == GTCapability.CAPABILITY_COVERABLE) {
            return GTCapability.CAPABILITY_COVERABLE.orEmpty(cap, LazyOptional.of(machine::getCoverContainer));
        } else if (cap == GTCapability.CAPABILITY_TOOLABLE) {
            return GTCapability.CAPABILITY_TOOLABLE.orEmpty(cap, LazyOptional.of(() -> machine));
        } else if (cap == GTCapability.CAPABILITY_WORKABLE) {
            if (machine instanceof IWorkable workable) {
                return GTCapability.CAPABILITY_WORKABLE.orEmpty(cap, LazyOptional.of(() -> workable));
            }
            for (MachineTrait trait : machine.getTraits()) {
                if (trait instanceof IWorkable workable) {
                    return GTCapability.CAPABILITY_WORKABLE.orEmpty(cap, LazyOptional.of(() -> workable));
                }
            }
        } else if (cap == GTCapability.CAPABILITY_CONTROLLABLE) {
            if (machine instanceof IControllable controllable) {
                return GTCapability.CAPABILITY_CONTROLLABLE.orEmpty(cap, LazyOptional.of(() -> controllable));
            }
            for (MachineTrait trait : machine.getTraits()) {
                if (trait instanceof IControllable controllable) {
                    return GTCapability.CAPABILITY_CONTROLLABLE.orEmpty(cap, LazyOptional.of(() -> controllable));
                }
            }
        } else if (cap == GTCapability.CAPABILITY_RECIPE_LOGIC) {
            for (MachineTrait trait : machine.getTraits()) {
                if (trait instanceof RecipeLogic recipeLogic) {
                    return GTCapability.CAPABILITY_RECIPE_LOGIC.orEmpty(cap, LazyOptional.of(() -> recipeLogic));
                }
            }
        } else if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER) {
            if (machine instanceof IEnergyContainer energyContainer) {
                return GTCapability.CAPABILITY_ENERGY_CONTAINER.orEmpty(cap, LazyOptional.of(() -> energyContainer));
            }
            var list = getCapabilitiesFromTraits(machine.getTraits(), side, IEnergyContainer.class);
            if (!list.isEmpty()) {
                return GTCapability.CAPABILITY_ENERGY_CONTAINER.orEmpty(cap,
                        LazyOptional.of(() -> list.size() == 1 ? list.get(0) : new EnergyContainerList(list)));
            }
        } else if (cap == GTCapability.CAPABILITY_ENERGY_INFO_PROVIDER) {
            if (machine instanceof IEnergyInfoProvider energyInfoProvider) {
                return GTCapability.CAPABILITY_ENERGY_INFO_PROVIDER.orEmpty(cap,
                        LazyOptional.of(() -> energyInfoProvider));
            }
            var list = getCapabilitiesFromTraits(machine.getTraits(), side, IEnergyInfoProvider.class);
            if (!list.isEmpty()) {
                return GTCapability.CAPABILITY_ENERGY_INFO_PROVIDER.orEmpty(cap,
                        LazyOptional.of(() -> list.size() == 1 ? list.get(0) : new EnergyInfoProviderList(list)));
            }
        } else if (cap == GTCapability.CAPABILITY_CLEANROOM_RECEIVER) {
            if (machine instanceof ICleanroomReceiver cleanroomReceiver) {
                return GTCapability.CAPABILITY_CLEANROOM_RECEIVER.orEmpty(cap,
                        LazyOptional.of(() -> cleanroomReceiver));
            }
        } else if (cap == GTCapability.CAPABILITY_MAINTENANCE_MACHINE) {
            if (machine instanceof IMaintenanceMachine maintenanceMachine) {
                return GTCapability.CAPABILITY_MAINTENANCE_MACHINE.orEmpty(cap,
                        LazyOptional.of(() -> maintenanceMachine));
            }
        } else if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (machine instanceof LDItemEndpointMachine itemEndpointMachine) {
                if (machine.getLevel().isClientSide) return null;
                ILDEndpoint endpoint = itemEndpointMachine.getLink();
                if (endpoint == null) return null;
                Direction outputFacing = endpoint.getOutputFacing();
                var h = GTTransferUtils.getAdjacentItemHandler(machine.getLevel(), endpoint.getPos(), outputFacing)
                        .map(LDItemEndpointMachine.ItemHandlerWrapper::new);
                if (h.isPresent()) {
                    return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, LazyOptional.of(h::get));
                }
            }
            var handler = machine.getItemHandlerCap(side, true);
            if (handler != null) {
                return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap,
                        LazyOptional.of(() -> handler));
            }
        } else if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (machine instanceof LDFluidEndpointMachine fluidEndpointMachine) {
                if (machine.getLevel().isClientSide) return null;
                ILDEndpoint endpoint = fluidEndpointMachine.getLink();
                if (endpoint == null) return null;
                Direction outputFacing = endpoint.getOutputFacing();
                var h = GTTransferUtils.getAdjacentFluidHandler(machine.getLevel(), endpoint.getPos(), outputFacing)
                        .map(LDFluidEndpointMachine.FluidHandlerWrapper::new);
                if (h.isPresent()) {
                    return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, LazyOptional.of(h::get));
                }
            }
            var handler = machine.getFluidHandlerCap(side, true);
            if (handler != null) {
                return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap,
                        LazyOptional.of(() -> handler));
            }
        } else if (cap == ForgeCapabilities.ENERGY) {
            if (machine instanceof IEnergyStorage energyStorage) {
                return ForgeCapabilities.ENERGY.orEmpty(cap,
                        LazyOptional.of(() -> energyStorage));
            }
            var list = getCapabilitiesFromTraits(machine.getTraits(), side, IEnergyStorage.class);
            if (!list.isEmpty()) {
                // TODO wrap list in the future
                return ForgeCapabilities.ENERGY.orEmpty(cap,
                        LazyOptional.of(() -> list.get(0)));
            }
        } else if (cap == GTCapability.CAPABILITY_LASER) {
            if (machine instanceof ILaserContainer energyContainer) {
                return GTCapability.CAPABILITY_LASER.orEmpty(cap, LazyOptional.of(() -> energyContainer));
            }
            var list = getCapabilitiesFromTraits(machine.getTraits(), side, ILaserContainer.class);
            if (!list.isEmpty()) {
                return GTCapability.CAPABILITY_LASER.orEmpty(cap,
                        LazyOptional.of(() -> list.size() == 1 ? list.get(0) : new LaserContainerList(list)));
            }
        } else if (cap == GTCapability.CAPABILITY_COMPUTATION_PROVIDER) {
            if (machine instanceof IOpticalComputationProvider computationProvider) {
                return GTCapability.CAPABILITY_COMPUTATION_PROVIDER.orEmpty(cap,
                        LazyOptional.of(() -> computationProvider));
            }
            var list = getCapabilitiesFromTraits(machine.getTraits(), side, IOpticalComputationProvider.class);
            if (!list.isEmpty()) {
                return GTCapability.CAPABILITY_COMPUTATION_PROVIDER.orEmpty(cap, LazyOptional.of(() -> list.get(0)));
            }
        } else if (cap == GTCapability.CAPABILITY_DATA_ACCESS) {
            if (machine instanceof IDataAccessHatch computationProvider) {
                return GTCapability.CAPABILITY_DATA_ACCESS.orEmpty(cap, LazyOptional.of(() -> computationProvider));
            }
            var list = getCapabilitiesFromTraits(machine.getTraits(), side, IDataAccessHatch.class);
            if (!list.isEmpty()) {
                return GTCapability.CAPABILITY_DATA_ACCESS.orEmpty(cap, LazyOptional.of(() -> list.get(0)));
            }
        }
        if (GTCEu.Mods.isAE2Loaded()) {
            if (cap == Capabilities.IN_WORLD_GRID_NODE_HOST) {
                if (machine instanceof IInWorldGridNodeHost nodeHost) {
                    return Capabilities.IN_WORLD_GRID_NODE_HOST.orEmpty(cap, LazyOptional.of(() -> nodeHost));
                }
                var list = getCapabilitiesFromTraits(machine.getTraits(), side, IInWorldGridNodeHost.class);
                if (!list.isEmpty()) {
                    // TODO wrap list in the future (or not.)
                    return Capabilities.IN_WORLD_GRID_NODE_HOST.orEmpty(cap, LazyOptional.of(() -> list.get(0)));
                }
            }
        }
        return null;
    }

    public static <T> List<T> getCapabilitiesFromTraits(List<MachineTrait> traits, Direction accessSide,
                                                        Class<T> capability) {
        if (traits.isEmpty()) return Collections.emptyList();
        List<T> list = new ArrayList<>();
        for (MachineTrait trait : traits) {
            if (trait.hasCapability(accessSide) && capability.isInstance(trait)) {
                list.add(capability.cast(trait));
            }
        }
        return list;
    }

    /**
     * Why, Forge, Why?
     * Why must you make me add a method for no good reason?
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public AABB getRenderBoundingBox() {
        GTRendererProvider instance = GTRendererProvider.getInstance();
        if (instance != null) {
            IRenderer renderer = instance.getRenderer(this);
            if (renderer != null) {
                if (renderer.getViewDistance() == 64 /* the default */) {
                    return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
                }

                int viewDistHalf = renderer.getViewDistance() / 2;
                return new AABB(worldPosition).inflate(viewDistHalf);
            }
        }
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var uuid = getOwnerUUID();
        if (uuid != null) {
            tag.putUUID("ownerUUID", uuid);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        TagFixer.fixFluidTags(tag);
        super.load(tag);
        if (tag.contains("ownerUUID")) {
            setOwnerUUID(tag.getUUID("ownerUUID"));
        } else {
            this.ownerUUID = null;
        }
    }
}
