package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.fluid_blocks.HerbalBathBlock;
import com.crabmod.hotbath.fluid_blocks.HoneyBathBlock;
import com.crabmod.hotbath.fluid_blocks.HotWaterBlock;
import com.crabmod.hotbath.fluid_blocks.MilkBathBlock;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import com.crabmod.hotbath.fluid_blocks.RoseBathBlock;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin for BottleItem to handle filling bottles from hotbath custom fluids.
 * This is necessary because vanilla BottleItem.use() performs its own raytrace
 * and doesn't trigger the RightClickBlock event when targeting non-water fluids.
 */
@Mixin(BottleItem.class)
public class BottleItemMixin {

    /**
     * Inject at the HEAD of use() to handle custom fluid bottles before vanilla logic.
     */
    @Inject(
            method = "use",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hotbath$handleCustomFluidBottle(
            Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        
        ItemStack itemStack = player.getItemInHand(hand);
        
        // Perform raytrace to find the block the player is looking at (same as vanilla)
        BlockHitResult hitResult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return; // Let vanilla handle it
        }
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        // Check if it's water - if so, let vanilla handle it
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return;
        }
        
        // Determine which bottle item to give based on the block type
        ItemStack filledBottle = hotbath$getFilledBottle(level, pos, block);
        
        if (filledBottle.isEmpty()) {
            return; // Not a custom fluid block, let vanilla handle it
        }
        
        // Play fill sound
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
        
        // Emit game event
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        
        // Update player stats
        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
        
        // Create the filled result (handles creative mode properly)
        ItemStack result = ItemUtils.createFilledResult(itemStack, player, filledBottle);
        
        // Return success with the filled bottle
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(result, level.isClientSide()));
    }
    
    /**
     * Determines which filled bottle item to return based on the block type.
     */
    private ItemStack hotbath$getFilledBottle(Level level, BlockPos pos, Block block) {
        // Handle DynamicCustomFluidBlock (data pack fluids)
        if (block instanceof DynamicCustomFluidBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                Optional<CustomFluidDefinition> definitionOpt = customBe.getFluidDefinition();
                if (definitionOpt.isPresent()) {
                    return CustomFluidAPI.createBottle(definitionOpt.get());
                }
            }
            return ItemStack.EMPTY;
        }
        
        // Handle hardcoded bath blocks - use RegistryObject.get() for 1.20 Forge
        RegistryObject<Item> bottleItem = hotbath$getBottleForBlock(block);
        if (bottleItem != null) {
            return new ItemStack(bottleItem.get());
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Maps hardcoded bath blocks to their corresponding bottle items.
     */
    private RegistryObject<Item> hotbath$getBottleForBlock(Block block) {
        if (block instanceof HotWaterBlock) {
            return ItemRegister.HOT_WATER_BOTTLE;
        } else if (block instanceof HoneyBathBlock) {
            return ItemRegister.HONEY_BATH_BOTTLE;
        } else if (block instanceof MilkBathBlock) {
            return ItemRegister.MILK_BATH_BOTTLE;
        } else if (block instanceof HerbalBathBlock) {
            return ItemRegister.HERBAL_BATH_BOTTLE;
        } else if (block instanceof PeonyBathBlock) {
            return ItemRegister.PEONY_BATH_BOTTLE;
        } else if (block instanceof RoseBathBlock) {
            return ItemRegister.ROSE_BATH_BOTTLE;
        }
        return null;
    }
}
