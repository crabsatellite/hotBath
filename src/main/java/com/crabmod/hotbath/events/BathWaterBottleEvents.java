package com.crabmod.hotbath.events;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.fluid_blocks.*;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Event handler for collecting bath water with glass bottles
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class BathWaterBottleEvents {

    @SubscribeEvent
    public static void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();

        // Only care about glass bottles
        if (item != Items.GLASS_BOTTLE) {
            return;
        }

        HitResult rayTraceResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        // Must be hitting a block
        if (rayTraceResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = ((BlockHitResult) rayTraceResult).getBlockPos();

        if (!level.mayInteract(player, pos)) {
            return;
        }

        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();

        // Check if it's one of our bath water blocks
        ItemStack filledStack = null;

        if (block instanceof HotWaterBlock) {
            filledStack = new ItemStack(ItemRegister.HOT_WATER_BOTTLE.get());
        } else if (block instanceof HoneyBathBlock) {
            filledStack = new ItemStack(ItemRegister.HONEY_BATH_BOTTLE.get());
        } else if (block instanceof MilkBathBlock) {
            filledStack = new ItemStack(ItemRegister.MILK_BATH_BOTTLE.get());
        } else if (block instanceof HerbalBathBlock) {
            filledStack = new ItemStack(ItemRegister.HERBAL_BATH_BOTTLE.get());
        } else if (block instanceof PeonyBathBlock) {
            filledStack = new ItemStack(ItemRegister.PEONY_BATH_BOTTLE.get());
        } else if (block instanceof RoseBathBlock) {
            filledStack = new ItemStack(ItemRegister.ROSE_BATH_BOTTLE.get());
        }

        // If we matched a bath water block, fill the bottle
        if (filledStack != null) {
            // Play filling sound
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);

            player.awardStat(Stats.ITEM_USED.get(item));
            
            // Replace the stack in player's hand
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            
            if (stack.isEmpty()) {
                player.setItemInHand(hand, filledStack);
            } else {
                if (!player.getInventory().add(filledStack)) {
                    player.drop(filledStack, false);
                }
            }

            // Cancel the event - we've taken responsibility for bottle filling
            event.setCanceled(true);
        }
    }

    private static HitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        float f = player.getXRot();
        float f1 = player.getYRot();
        Vec3 vec3 = player.getEyePosition();
        float f2 = net.minecraft.util.Mth.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = net.minecraft.util.Mth.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -net.minecraft.util.Mth.cos(-f * ((float)Math.PI / 180F));
        float f5 = net.minecraft.util.Mth.sin(-f * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = player.getBlockReach();
        Vec3 vec31 = vec3.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        return level.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, fluidMode, player));
    }
}













