package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Capability system for storing dirtiness data on players.
 * Uses Forge Capability API for persistent per-player data.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class DirtinessCapability {
    
    public static final ResourceLocation DIRTINESS_CAP_ID = 
            new ResourceLocation(HotBath.MOD_ID, "dirtiness");
    
    public static final Capability<DirtinessData> DIRTINESS = 
            CapabilityManager.get(new CapabilityToken<>() {});
    
    /**
     * Get dirtiness data from a player.
     * @param player The player to get data from
     * @return LazyOptional containing the dirtiness data, or empty if not available
     */
    public static LazyOptional<DirtinessData> get(Player player) {
        return player.getCapability(DIRTINESS);
    }
    
    /**
     * Get dirtiness data from a player, returning null if not available.
     * @param player The player to get data from
     * @return The dirtiness data, or null if not available
     */
    @Nullable
    public static DirtinessData getOrNull(Player player) {
        return player.getCapability(DIRTINESS).orElse(null);
    }
    
    /**
     * Attach capability to players when they are created
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(DIRTINESS).isPresent()) {
                event.addCapability(DIRTINESS_CAP_ID, new DirtinessProvider());
            }
        }
    }
    
    /**
     * Provider class that holds the actual data and handles serialization
     */
    public static class DirtinessProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        
        private DirtinessData data = null;
        private final LazyOptional<DirtinessData> optional = LazyOptional.of(this::createData);
        
        private DirtinessData createData() {
            if (data == null) {
                data = new DirtinessData();
            }
            return data;
        }
        
        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == DIRTINESS) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }
        
        @Override
        public CompoundTag serializeNBT() {
            return createData().serializeNBT();
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            createData().deserializeNBT(nbt);
        }
    }
    
    /**
     * Register capability event handler (for MOD bus)
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(DirtinessData.class);
        }
    }
}
