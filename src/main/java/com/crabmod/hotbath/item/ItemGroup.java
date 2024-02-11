package com.crabmod.hotbath.item;

import com.crabmod.hotbath.ModItems.ItemRegister;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemGroup {
  public static final net.minecraft.world.item.CreativeModeTab HOT_BATH =
      new net.minecraft.world.item.CreativeModeTab("hot_bath") {
        @Override
        public @NotNull ItemStack makeIcon() {
          return new ItemStack(ItemRegister.HERBAL_BATH_BUCKET.get());
        }
      };
}