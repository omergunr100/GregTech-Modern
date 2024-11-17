package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MagicEnergyAbsorberMachine extends TieredEnergyMachine implements IControllable {

    public static final long BASE_EU_PER_FEATURE = 32;
    public static final long AMPLIFIER_MULTIPLIER = 4;

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MagicEnergyAbsorberMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    @DescSynced
    @Persisted
    @Getter
    @Setter
    protected boolean isWorkingEnabled;
    @DescSynced
    @Persisted
    @Getter
    @Setter
    protected boolean hasAmplifier;
    @Getter
    protected IntList connectedFeatures;

    // subscriptions
    protected TickableSubscription onServerTickSubscription;

    public MagicEnergyAbsorberMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);
        this.isWorkingEnabled = true;
        this.connectedFeatures = new IntArrayList();
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected boolean isEnergyEmitter() {
        return true;
    }

    @Override
    protected long getMaxInputOutputAmperage() {
        return 4;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            onServerTickSubscription = this.subscribeServerTick(() -> onServerTick(false));
            onServerTick(true);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!isRemote()) {
            resetConnectedFeatures();
            if (onServerTickSubscription != null)
                this.unsubscribe(onServerTickSubscription);
        }
    }

    protected void onServerTick(boolean force) {
        if (!(getLevel() instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.END) return;

        if (force || getOffsetTimer() % 20 == 0)
            updateAmplifierStatus();

        if (force || getOffsetTimer() % 200 == 0)
            updateConnectedFeatures();

        if (force || !serverLevel.getDragons().isEmpty())
            updateCrystalTargets();

        if (connectedFeatures.isEmpty()) return;

        long energyPer = BASE_EU_PER_FEATURE * (hasAmplifier ? AMPLIFIER_MULTIPLIER : 1);
        long energyGenerated = 0;
        // double check end crystals
        for (int i = 0; i < connectedFeatures.size(); i++) {
            if (getLevel().getEntity(connectedFeatures.getInt(i)) instanceof EndCrystal)
                energyGenerated += energyPer;
        }

        if (energyGenerated > 0)
            energyContainer.changeEnergy(energyGenerated);
    }

    protected void updateAmplifierStatus() {
        if (getLevel() == null) {
            this.hasAmplifier = false;
            return;
        }

        BlockState blockState = getLevel().getBlockState(getPos().above());
        this.hasAmplifier = blockState.getBlock() instanceof DragonEggBlock;
    }

    protected void updateConnectedFeatures() {
        this.connectedFeatures.clear();
        if (getLevel() == null) return;

        // get all natural end crystals
        final double maxDistance = 4096; // (64^2)
        List<EndCrystal> endCrystals = SpikeFeature.getSpikesForLevel((WorldGenLevel) getLevel()).stream()
                .map(spike -> getLevel().getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(),
                        crystal -> crystal.distanceToSqr(getPos().getCenter()) < maxDistance))
                .reduce(new ArrayList<>(), (lst, crystalList) -> {
                    lst.addAll(crystalList);
                    return lst;
                });

        // set end crystals beam target to this generator
        endCrystals.forEach(endCrystal -> {
            BlockPos beamTarget = endCrystal.getBeamTarget();
            if (getPos().equals(beamTarget))
                this.connectedFeatures.add(endCrystal.getId());
            else if (beamTarget == null) {
                endCrystal.setBeamTarget(getPos());
                this.connectedFeatures.add(endCrystal.getId());
            }
        });
    }

    protected void updateCrystalTargets() {
        if (getLevel() == null) return;
        // ender dragon check
        List<? extends EnderDragon> dragonsInRange = ((ServerLevel) getLevel()).getDragons();

        for (EnderDragon dragon : dragonsInRange) {
            if (dragon.nearestCrystal != null && connectedFeatures.contains(dragon.nearestCrystal.getId())) {
                dragon.nearestCrystal = null;

                if (dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.HOLDING_PATTERN) {
                    dragon.hurt(dragon.damageSources().explosion(dragon, dragon), 10.0f);
                    dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
                    ((DragonChargePlayerPhase) dragon.getPhaseManager().getCurrentPhase())
                            .setTarget(getPos().getCenter());
                }
            }
        }
    }

    protected void resetConnectedFeatures() {
        if (getLevel() == null) {
            connectedFeatures.clear();
            return;
        }

        for (int id : connectedFeatures) {
            Entity entity = getLevel().getEntity(id);

            if (!(entity instanceof EndCrystal endCrystal)) continue;

            if (getPos().equals(endCrystal.getBeamTarget()))
                endCrystal.setBeamTarget(null);
        }

        connectedFeatures.clear();
    }
}
