package com.crabmod.hotbath.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import static com.crabmod.hotbath.registers.ItemRegister.HOT_WATER_BUCKET;

@EventBusSubscriber(modid = "hotbath")
public class CraftingEventHandler {

  @SubscribeEvent
  public static void onItemCrafted(ItemCraftedEvent event) {
    if (event.getInventory() instanceof CraftingContainer craftingInventory) {
      boolean foundHotWaterBucket = false;
      boolean foundMilkBucket = false;

      // Iterate through all items in the crafting grid
      for (int i = 0; i < craftingInventory.getContainerSize(); i++) {
        ItemStack itemStack = craftingInventory.getItem(i);
        Item item = itemStack.getItem();

        // Check if the item is the custom hot water bucket
        if (item == HOT_WATER_BUCKET.get()) {
          foundHotWaterBucket = true;
          // Remove the hot water bucket from the crafting grid
          itemStack.shrink(1);
        }

        // Check if the item is a milk bucket
        if (item == Items.MILK_BUCKET) {
          foundMilkBucket = true;
        }
      }

      // If the recipe contains both the hot water bucket and milk bucket, add an empty bucket
      if (foundHotWaterBucket && foundMilkBucket) {
        if (event.getEntity() instanceof Player player) {
          ItemStack emptyBucket = new ItemStack(Items.BUCKET);
          if (!player.getInventory().add(emptyBucket)) {
            player.drop(emptyBucket, false);
          }
        }
      }
    }
  }
}
