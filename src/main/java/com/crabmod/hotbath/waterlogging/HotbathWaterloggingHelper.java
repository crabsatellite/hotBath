package com.crabmod.hotbath.waterlogging;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for tracking which fluid is waterlogged in blocks.
 * Since the vanilla WATERLOGGED property is just a boolean, we need a way to
 * remember which fluid was actually placed in the block.
 * 
 * <p>Defensive Design Notes:
 * <ul>
 *   <li>When mod is unloaded, waterlogged blocks will fall back to vanilla water behavior</li>
 *   <li>Invalid/missing fluids are gracefully handled and cleaned up</li>
 *   <li>Data is periodically validated against actual world state</li>
 * </ul>
 */
public class HotbathWaterloggingHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotbathWaterloggingHelper.class);
    private static final String DATA_KEY = HotBath.MOD_ID + "_waterlogging";
    
    // Client-side cache for fluid types (synced from server via packets)
    private static final Map<Long, ResourceLocation> clientFluidCache = new ConcurrentHashMap<>();

    /**
     * Store the fluid type for a waterlogged position
     */
    public static void storeFluidType(LevelAccessor level, BlockPos pos, Fluid fluid) {
        if (fluid == null || pos == null) return;
        
        if (level instanceof ServerLevel serverLevel) {
            WaterloggingData data = getOrCreateData(serverLevel);
            data.setFluid(pos, fluid);
            // Update client cache as well (for singleplayer)
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            ResourceLocation waterId = BuiltInRegistries.FLUID.getKey(Fluids.WATER);
            if (fluidId != null && !fluidId.equals(waterId)) {
                clientFluidCache.put(pos.asLong(), fluidId);
                // Sync to all clients
                WaterloggingNetworking.syncToAllPlayers(pos, fluid);
            }
        } else if (level instanceof Level clientLevel && clientLevel.isClientSide()) {
            // Store in client cache
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            ResourceLocation waterId = BuiltInRegistries.FLUID.getKey(Fluids.WATER);
            if (fluidId != null && !fluidId.equals(waterId)) {
                clientFluidCache.put(pos.asLong(), fluidId);
            }
        }
    }

    /**
     * Get the stored fluid type for a position.
     * Returns null if no custom fluid is stored, or if the stored fluid is no longer registered
     * (e.g., after mod uninstall - this allows graceful fallback to vanilla water).
     */
    @Nullable
    public static Fluid getStoredFluidType(LevelAccessor level, BlockPos pos) {
        if (pos == null) return null;
        
        if (level instanceof ServerLevel serverLevel) {
            WaterloggingData data = getOrCreateData(serverLevel);
            Fluid fluid = data.getFluid(pos);
            // Validate the fluid still exists
            if (fluid != null && fluid != Fluids.EMPTY) {
                return fluid;
            }
            // Clean up invalid entry
            if (fluid == Fluids.EMPTY) {
                data.removeFluid(pos);
            }
            return null;
        }
        // For client side, use the cache
        return getFluidFromCache(pos);
    }
    
    /**
     * Get stored fluid type for client-side rendering.
     * This is called from the BlockRenderDispatcher mixin.
     * Optimized: Only uses cache lookup, no reflection.
     */
    @Nullable
    public static Fluid getStoredFluidTypeClient(BlockAndTintGetter level, BlockPos pos) {
        if (pos == null) return null;
        // Only check the client cache - this is the fast path for rendering
        return getFluidFromCache(pos);
    }
    
    /**
     * Get stored fluid type for client-side rendering, without needing a Level reference.
     * This is specifically designed for Sodium compatibility, where the render thread
     * doesn't have access to the Level object in the same way.
     * 
     * <p>Note: This method only checks the client cache. The cache must be populated
     * via network sync from the server.</p>
     * 
     * @param pos The block position to query
     * @return The stored fluid, or null if none is stored
     */
    @Nullable
    public static Fluid getStoredFluidTypeClientDirect(BlockPos pos) {
        if (pos == null) return null;
        // Directly check the client cache - optimized for Sodium's render thread
        return getFluidFromCache(pos);
    }
    
    /**
     * Get fluid from client cache.
     * Safely handles missing/unregistered fluids.
     */
    @Nullable
    private static Fluid getFluidFromCache(BlockPos pos) {
        ResourceLocation fluidId = clientFluidCache.get(pos.asLong());
        if (fluidId != null) {
            // Use getOptional to safely handle missing fluids (mod uninstalled)
            Optional<Fluid> optionalFluid = BuiltInRegistries.FLUID.getOptional(fluidId);
            if (optionalFluid.isPresent()) {
                Fluid fluid = optionalFluid.get();
                // Don't return EMPTY or vanilla WATER
                if (fluid != Fluids.EMPTY && fluid != Fluids.WATER) {
                    return fluid;
                }
            }
            // Clean up invalid cache entry
            clientFluidCache.remove(pos.asLong());
        }
        return null;
    }
    
    /**
     * Update client cache (called when syncing from server or placing fluid)
     */
    public static void updateClientCache(BlockPos pos, Fluid fluid) {
        if (pos == null) return;
        
        if (fluid == null || fluid == Fluids.WATER || fluid == Fluids.EMPTY) {
            clientFluidCache.remove(pos.asLong());
        } else {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            if (fluidId != null) {
                clientFluidCache.put(pos.asLong(), fluidId);
            }
        }
    }
    
    /**
     * Remove from client cache
     */
    public static void removeFromClientCache(BlockPos pos) {
        if (pos != null) {
            clientFluidCache.remove(pos.asLong());
        }
    }
    
    /**
     * Clear the client cache (called on world unload)
     */
    public static void clearClientCache() {
        clientFluidCache.clear();
    }

    /**
     * Remove the stored fluid type for a position
     */
    public static void removeFluidType(LevelAccessor level, BlockPos pos) {
        if (pos == null) return;
        
        if (level instanceof ServerLevel serverLevel) {
            WaterloggingData data = getOrCreateData(serverLevel);
            data.removeFluid(pos);
            // Also sync removal to clients
            WaterloggingNetworking.syncRemoveToAllPlayers(pos);
        }
        // Also remove from local cache
        clientFluidCache.remove(pos.asLong());
    }
    
    /**
     * Validate and clean up stale entries in the waterlogging data.
     * This should be called periodically or on world load to remove entries
     * for blocks that are no longer waterlogged (e.g., destroyed blocks).
     * 
     * <p>This is important for:
     * <ul>
     *   <li>Cleaning up after blocks are destroyed</li>
     *   <li>Removing entries for fluids from uninstalled mods</li>
     *   <li>Keeping SavedData size reasonable</li>
     * </ul>
     * 
     * @param level The server level to validate
     * @return The number of stale entries removed
     */
    public static int validateAndCleanup(ServerLevel level) {
        if (level == null) return 0;
        
        WaterloggingData data = getOrCreateData(level);
        return data.validateAndCleanup(level);
    }

    private static WaterloggingData getOrCreateData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WaterloggingData.factory(),
                DATA_KEY
        );
    }
    
    /**
     * Get all stored fluid data for syncing to clients
     */
    public static Map<Long, ResourceLocation> getAllStoredFluids(ServerLevel level) {
        WaterloggingData data = getOrCreateData(level);
        return data.getAllFluidsForSync();
    }

    /**
     * SavedData implementation to persist waterlogged fluid types.
     * 
     * <p>Defensive design:
     * <ul>
     *   <li>Gracefully handles missing fluids (from uninstalled mods)</li>
     *   <li>Validates entries against actual world state</li>
     *   <li>Never throws exceptions on corrupted data</li>
     * </ul>
     */
    public static class WaterloggingData extends SavedData {
        private final Map<BlockPos, ResourceLocation> fluidMap = new HashMap<>();

        public WaterloggingData() {
        }

        public WaterloggingData(CompoundTag tag, HolderLookup.Provider provider) {
            CompoundTag fluidsTag = tag.getCompound("fluids");
            for (String key : fluidsTag.getAllKeys()) {
                String[] parts = key.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        BlockPos pos = new BlockPos(x, y, z);
                        String fluidIdStr = fluidsTag.getString(key);
                        // Safe parse that handles malformed resource locations
                        ResourceLocation fluidId = ResourceLocation.tryParse(fluidIdStr);
                        if (fluidId != null) {
                            // Only add if the fluid is still registered
                            if (BuiltInRegistries.FLUID.containsKey(fluidId)) {
                                fluidMap.put(pos, fluidId);
                            } else {
                                LOGGER.debug("Skipping unregistered fluid {} at {} (mod may have been removed)", 
                                        fluidIdStr, pos);
                            }
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid position format in waterlogging data: {}", key);
                    }
                }
            }
        }

        public static Factory<WaterloggingData> factory() {
            return new Factory<>(WaterloggingData::new, WaterloggingData::new);
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            CompoundTag fluidsTag = new CompoundTag();
            for (Map.Entry<BlockPos, ResourceLocation> entry : fluidMap.entrySet()) {
                BlockPos pos = entry.getKey();
                String key = pos.getX() + "," + pos.getY() + "," + pos.getZ();
                fluidsTag.putString(key, entry.getValue().toString());
            }
            tag.put("fluids", fluidsTag);
            return tag;
        }

        public void setFluid(BlockPos pos, Fluid fluid) {
            if (pos == null || fluid == null) return;
            
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            ResourceLocation waterId = BuiltInRegistries.FLUID.getKey(Fluids.WATER);
            // Only store non-vanilla water fluids
            if (fluidId != null && !fluidId.equals(waterId)) {
                fluidMap.put(pos.immutable(), fluidId);
                setDirty();
            }
        }

        /**
         * Get the fluid for a position.
         * Returns null if not found, or Fluids.EMPTY if the fluid is no longer registered.
         */
        @Nullable
        public Fluid getFluid(BlockPos pos) {
            ResourceLocation fluidId = fluidMap.get(pos);
            if (fluidId != null) {
                // Use getOptional for safe lookup
                Optional<Fluid> optionalFluid = BuiltInRegistries.FLUID.getOptional(fluidId);
                if (optionalFluid.isPresent()) {
                    return optionalFluid.get();
                }
                // Fluid is no longer registered (mod removed) - return EMPTY to signal cleanup
                return Fluids.EMPTY;
            }
            return null;
        }

        public void removeFluid(BlockPos pos) {
            if (fluidMap.remove(pos) != null) {
                setDirty();
            }
        }
        
        /**
         * Validate all entries and remove stale ones.
         * A stale entry is one where:
         * - The fluid is no longer registered (mod removed)
         * - The block at that position is no longer waterlogged
         * 
         * @return The number of entries removed
         */
        public int validateAndCleanup(ServerLevel level) {
            int removed = 0;
            Iterator<Map.Entry<BlockPos, ResourceLocation>> iterator = fluidMap.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, ResourceLocation> entry = iterator.next();
                BlockPos pos = entry.getKey();
                ResourceLocation fluidId = entry.getValue();
                
                boolean shouldRemove = false;
                
                // Check if fluid is still registered
                if (!BuiltInRegistries.FLUID.containsKey(fluidId)) {
                    LOGGER.debug("Removing entry for unregistered fluid {} at {}", fluidId, pos);
                    shouldRemove = true;
                } else if (level.isLoaded(pos)) {
                    // Check if block is still waterlogged
                    BlockState state = level.getBlockState(pos);
                    if (!state.hasProperty(BlockStateProperties.WATERLOGGED) 
                            || !state.getValue(BlockStateProperties.WATERLOGGED)) {
                        LOGGER.debug("Removing stale waterlogging entry at {} (block no longer waterlogged)", pos);
                        shouldRemove = true;
                    }
                }
                
                if (shouldRemove) {
                    iterator.remove();
                    removed++;
                }
            }
            
            if (removed > 0) {
                setDirty();
                LOGGER.info("Cleaned up {} stale waterlogging entries", removed);
            }
            
            return removed;
        }
        
        /**
         * Get all fluids as a map of position long to ResourceLocation for syncing.
         * Only includes entries for fluids that are still registered.
         */
        public Map<Long, ResourceLocation> getAllFluidsForSync() {
            Map<Long, ResourceLocation> result = new HashMap<>();
            for (Map.Entry<BlockPos, ResourceLocation> entry : fluidMap.entrySet()) {
                // Only sync if fluid is still registered
                if (BuiltInRegistries.FLUID.containsKey(entry.getValue())) {
                    result.put(entry.getKey().asLong(), entry.getValue());
                }
            }
            return result;
        }
    }
}
