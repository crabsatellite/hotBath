package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.registers.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
     */
    public void setFluidId(@Nullable ResourceLocation fluidId) {
        this.fluidId = fluidId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        if (fluidId == null) {
            return Optional.empty();
        }
        return CustomFluidRegistry.getDefinition(fluidId);
    }
    
    /**
     * Checks if the stored fluid is hot (temperature >= threshold).
     */
    public boolean isHot() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::isHot)
                .orElse(false);
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
    
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (fluidId != null) {
            tag.putString(TAG_FLUID_ID, fluidId.toString());
        }
        return tag;
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
        
        // If fluid ID changed, trigger re-render
        if (level != null && level.isClientSide) {
            boolean fluidChanged = (oldFluidId == null && fluidId != null) 
                    || (oldFluidId != null && !oldFluidId.equals(fluidId));
            if (fluidChanged) {
                // Force chunk re-render for fluid color update
                level.setBlocksDirty(worldPosition, Blocks.AIR.defaultBlockState(), getBlockState());
            }
        }
    }
}
