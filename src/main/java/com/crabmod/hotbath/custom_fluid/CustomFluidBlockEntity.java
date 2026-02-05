package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.registers.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * BlockEntity for storing custom fluid data in placed fluid blocks.
 * This allows each fluid block to have its own fluid definition from data packs.
 */
public class CustomFluidBlockEntity extends BlockEntity {
    
    private static final String TAG_FLUID_ID = "FluidId";
    
    @Nullable
    private ResourceLocation fluidId;
    
    public CustomFluidBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.CUSTOM_FLUID_BLOCK_ENTITY.get(), pos, state);
    }
    
    /**
     * Sets the fluid ID for this block.
     * Also updates all connected flowing fluid blocks.
     */
    public void setFluidId(@Nullable ResourceLocation fluidId) {
        ResourceLocation oldFluidId = this.fluidId;
        this.fluidId = fluidId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Update all connected flowing fluid blocks
            updateConnectedFluidBlocks(fluidId);
            
            // If the fluid ID changed, schedule tick updates for neighbors
            // so they can re-evaluate if they still have a valid source
            boolean idChanged = (oldFluidId == null && fluidId != null)
                    || (oldFluidId != null && !oldFluidId.equals(fluidId));
            if (idChanged) {
                scheduleNeighborFluidUpdates();
            }
        }
    }
    
    /**
     * Schedule tick updates for neighboring flowing fluid blocks.
     * This ensures they re-evaluate their state when the source fluid ID changes.
     */
    private void scheduleNeighborFluidUpdates() {
        if (level == null || level.isClientSide) return;
        
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            
            // Schedule tick for all non-source fluids
            if (!neighborFluid.isEmpty() && !neighborFluid.isSource()) {
                level.scheduleTick(neighborPos, neighborFluid.getType(), 1);
            }
        }
    }
    
    /**
     * Updates all connected custom fluid blocks with the new fluid ID.
     * Uses BFS to find all adjacent fluid blocks.
     * Only updates blocks that have no fluidId (flowing from this source) or the same fluidId.
     * Does NOT overwrite blocks that already have a different fluidId (different source).
     */
    private void updateConnectedFluidBlocks(@Nullable ResourceLocation newFluidId) {
        if (level == null || level.isClientSide) return;
        
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        
        // Start from current position
        visited.add(worldPosition);
        
        // Add adjacent positions to queue
        for (Direction dir : Direction.values()) {
            queue.add(worldPosition.relative(dir));
        }
        
        int maxBlocks = 256; // Limit to prevent lag
        int processed = 0;
        
        while (!queue.isEmpty() && processed < maxBlocks) {
            BlockPos pos = queue.poll();
            if (visited.contains(pos)) continue;
            visited.add(pos);
            
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CustomFluidBlockEntity fluidBe) {
                ResourceLocation existingId = fluidBe.fluidId;
                
                // Only update if:
                // 1. The block has no fluidId (null) - it's a flowing block from this source
                // 2. The block has the same fluidId - it's from the same source
                // Do NOT update if it has a different fluidId - it's from a different source
                boolean shouldUpdate = existingId == null || existingId.equals(newFluidId);
                
                if (shouldUpdate) {
                    fluidBe.fluidId = newFluidId;
                    fluidBe.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                    processed++;
                    
                    // Add adjacent positions to continue searching
                    for (Direction dir : Direction.values()) {
                        BlockPos adjacent = pos.relative(dir);
                        if (!visited.contains(adjacent)) {
                            queue.add(adjacent);
                        }
                    }
                }
                // If the block has a different fluidId, we don't update it and don't continue BFS from it
            }
        }
    }
    
    /**
     * Gets the fluid ID stored in this block.
     */
    @Nullable
    public ResourceLocation getFluidId() {
        return fluidId;
    }
    
    /**
     * Gets the fluid definition for the stored fluid ID.
     */
    public Optional<CustomFluidDefinition> getFluidDefinition() {
        ResourceLocation id = getFluidId();
        if (id == null) {
            return Optional.empty();
        }
        return CustomFluidRegistry.getDefinition(id);
    }
    
    /**
     * Checks if the stored fluid is hot (temperature >= 35°C threshold).
     */
    public boolean isHot() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::isHot)
                .orElse(false);
    }
    
    /**
     * Checks if steam particles should be shown for this fluid.
     * Steam is shown when temperature >= HOT_TEMPERATURE_THRESHOLD (35°C) AND show_steam is true.
     */
    public boolean shouldShowSteam() {
        return getFluidDefinition()
                .map(def -> def.isHot() && def.showSteam())
                .orElse(false);
    }
    
    /**
     * Checks if general particles (splash, drip, etc.) should be shown for this fluid.
     * Controlled by the show_particles setting in the fluid definition.
     */
    public boolean shouldShowParticles() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::showParticles)
                .orElse(true);
    }
    
    /**
     * Checks if bubble particles should be shown for this fluid.
     * Controlled by the show_bubbles setting in the fluid definition.
     */
    public boolean shouldShowBubbles() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::showBubbles)
                .orElse(true);
    }
    
    /**
     * Gets the color of the stored fluid.
     */
    public int getFluidColor() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::color)
                .orElse(0x45E1E9); // Default cyan color
    }
    
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (fluidId != null) {
            tag.putString(TAG_FLUID_ID, fluidId.toString());
        }
    }
    
    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_FLUID_ID)) {
            fluidId = ResourceLocation.tryParse(tag.getString(TAG_FLUID_ID));
        } else {
            fluidId = null;
        }
    }
    
    /**
     * Called when the BlockEntity is fully loaded into the world.
     * This ensures light emission is properly updated after world load,
     * fixing the issue where custom fluids with luminosity > 0 would lose their light
     * after quitting and rejoining the game (single player).
     */
    @Override
    public void onLoad() {
        super.onLoad();
        scheduleLightUpdate();
        // Also mark the block for update on server side
        if (level != null && !level.isClientSide && fluidId != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (fluidId != null) {
            tag.putString(TAG_FLUID_ID, fluidId.toString());
        }
        return tag;
    }
    
    /**
     * Called on the client when receiving the initial sync data from the server.
     * This is called when a chunk is loaded or when a player joins the server.
     */
    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        super.handleUpdateTag(tag);
        // After loading the tag, schedule a light update
        // This fixes the issue where custom fluids with luminosity > 0 would lose their light
        // after rejoining a server
        scheduleLightUpdate();
    }
    
    /**
     * Schedules a light update for this block position.
     * Called after fluid data is loaded/synced to ensure proper light emission.
     * 
     * We always trigger the light update if fluidId is present, because:
     * 1. The CustomFluidRegistry might not be populated yet when this is called during world load
     * 2. The light engine will call getLightEmission() which will return the correct value
     *    once the registry is populated
     * 3. This fixes the issue where custom fluids with luminosity > 0 would lose their light
     *    after quitting and rejoining the game (single player)
     */
    private void scheduleLightUpdate() {
        if (level != null && fluidId != null) {
            // Always trigger light engine to recompute light at this position
            // The getLightEmission() in DynamicCustomFluidBlock will be called during the update
            level.getLightEngine().checkBlock(worldPosition);
        }
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt) {
        ResourceLocation oldFluidId = this.fluidId;
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
        
        // If fluid ID changed, trigger re-render and light update
        if (level != null && level.isClientSide) {
            boolean fluidChanged = (oldFluidId == null && fluidId != null) 
                    || (oldFluidId != null && !oldFluidId.equals(fluidId));
            if (fluidChanged) {
                // Force chunk re-render for fluid color update
                level.setBlocksDirty(worldPosition, Blocks.AIR.defaultBlockState(), getBlockState());
                // Also update light level
                scheduleLightUpdate();
            }
        }
    }
}
