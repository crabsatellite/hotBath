package com.crabmod.hotbath.mixin.lso;

import com.crabmod.hotbath.custom_fluid.CustomFluidBottleItem;
import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sfiomn.legendarysurvivaloverhaul.api.data.json.JsonThirstConsumable;
import sfiomn.legendarysurvivaloverhaul.common.listeners.ThirstConsumableListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Mixin to extend LSO's thirst consumable lookup to support HotBath's custom fluid bottles.
 * This allows LSO to show the thirst bar preview when holding custom fluid bottles,
 * and also provides correct tooltip information.
 */
@Mixin(value = ThirstConsumableListener.class, remap = false)
public class ThirstConsumableListenerMixin {

    /**
     * Inject at the head of get(ItemStack) to handle custom fluid bottles.
     * If the item is a HotBath custom fluid bottle with thirst value, return a synthetic
     * JsonThirstConsumable with the correct hydration data.
     */
    @Inject(
        method = "get(Lnet/minecraft/world/item/ItemStack;)Lsfiomn/legendarysurvivaloverhaul/api/data/json/JsonThirstConsumable;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hotbath$getCustomFluidThirst(ItemStack itemStack, CallbackInfoReturnable<JsonThirstConsumable> cir) {
        if (itemStack.getItem() instanceof CustomFluidBottleItem) {
            CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(itemStack);
            if (definition != null && definition.thirst() > 0) {
                // Create a synthetic JsonThirstConsumable with the fluid's thirst data
                JsonThirstConsumable syntheticConsumable = new JsonThirstConsumable(
                        definition.thirst(),
                        0.5f,
                        new ArrayList<>(),
                        new HashMap<>()
                );
                cir.setReturnValue(syntheticConsumable);
            }
        }
    }
}
