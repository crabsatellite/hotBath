package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Handles bucket interactions with custom fluid blocks.
 * Allows players to collect custom fluids from the world using empty buckets.
 * 
 * <p>Note: Glass bottle interactions are handled by BottleItemMixin instead of this handler,
 * because vanilla glass bottles use the item's use() method rather than block interaction.</p>
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class CustomFluidBucketHandler {

    /**
     * Handles right-click on blocks with buckets.
     * Allows collecting custom fluids from DynamicCustomFluidBlocks.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack heldItem = player.getItemInHand(hand);
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Only handle empty bucket interactions
        if (!heldItem.is(Items.BUCKET)) {
            return;
        }

        // Check if the block is a dynamic custom fluid block
        if (!(block instanceof DynamicCustomFluidBlock dynamicBlock)) {
            // Also check for old CustomFluidBlock for backwards compatibility
            if (block instanceof CustomFluidBlock customFluidBlock) {
                handleBucketFill(event, level, player, hand, heldItem, pos, customFluidBlock.getDefinition());
            }
            return;
        }

        // Get fluid definition from BlockEntity
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CustomFluidBlockEntity customBe)) {
            return;
        }

        Optional<CustomFluidDefinition> definitionOpt = customBe.getFluidDefinition();
        if (definitionOpt.isEmpty()) {
            return;
        }

        CustomFluidDefinition definition = definitionOpt.get();
        handleBucketFill(event, level, player, hand, heldItem, pos, definition);
    }

    /**
     * Handles bucket filling from a custom fluid.
     */
    private static void handleBucketFill(
            PlayerInteractEvent.RightClickBlock event,
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack heldItem,
            BlockPos pos,
            CustomFluidDefinition definition) {

        if (!level.isClientSide) {
            // Create the filled bucket
            ItemStack filledBucket = CustomFluidAPI.createBucket(definition);

            // Remove the fluid block
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);

            // Play sound
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);

            // Give the filled bucket to the player
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
                if (!player.getInventory().add(filledBucket)) {
                    player.drop(filledBucket, false);
                }
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
