package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.compat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A splash potion-style bottle that can contain any custom fluid defined in data packs.
 * When thrown, it applies the effects defined in the fluid's JSON configuration to
 * all entities in the splash radius.
 * 
 * <p>The splash effect applies a slightly enhanced version of the drink effect
 * (higher amplifier, shorter duration) to maintain balance.</p>
 */
public class SplashCustomFluidBottleItem extends Item {
    
    public SplashCustomFluidBottleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            ThrownCustomFluidBottle thrownBottle = new ThrownCustomFluidBottle(level, player);
            thrownBottle.setItem(stack.copy());
            thrownBottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(thrownBottle);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Applies the splash effect to an entity.
     * Called by the thrown projectile when it hits.
     */
    public void applySplashEffect(LivingEntity entity, CustomFluidDefinition definition) {
        if (definition == null) return;
        
        // Apply effects with splash modifiers (shorter duration, +1 amplifier)
        List<MobEffectInstance> effects = definition.createEffectInstances();
        for (MobEffectInstance effect : effects) {
            entity.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    (int)(effect.getDuration() * 0.75), // 75% duration for splash
                    Math.min(effect.getAmplifier() + 1, 4), // +1 level, max 5
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }

        // Apply temperature effects for mod compatibility
        if (entity instanceof Player player) {
            applySplashTemperatureEffects(player, definition);
        }
    }

    /**
     * Applies temperature effects for compatible mods (splash version).
     */
    private void applySplashTemperatureEffects(Player player, CustomFluidDefinition definition) {
        // Apply ToughAsNails temperature effect
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applySplashWarmEffect(player);
        }
        
        // Apply Cold Sweat temperature effect
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applySplashWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applySplashWarmEffect(player);
        }
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
        if (definition != null) {
            // Use the dynamic translation system to get the translated fluid name
            String translatedFluidName = definition.getTranslatedName();
            Component fluidName = Component.literal(translatedFluidName);
            return Component.translatable("item.hotbath.splash_custom_fluid_bottle", fluidName);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
        if (definition != null) {
            tooltipComponents.add(Component.translatable("item.hotbath.splash_custom_fluid_bottle.desc")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            
            // Show effects (with splash modifiers)
            List<MobEffectInstance> effects = definition.createEffectInstances();
            for (MobEffectInstance effect : effects) {
                String effectName = effect.getEffect().getDescriptionId();
                int amplifier = Math.min(effect.getAmplifier() + 1, 4);
                int durationSeconds = (int)(effect.getDuration() * 0.75) / 20;
                
                String level_str = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
                tooltipComponents.add(Component.translatable(effectName)
                        .append(level_str)
                        .append(" (" + durationSeconds + "s)")
                        .withStyle(effect.getEffect().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED));
            }
        }
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }

    /**
     * Gets the custom fluid color for item rendering.
     */
    public static int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) {
            int color = CustomFluidNBTHelper.getFluidColor(stack);
            if (color != -1) {
                return color;
            }
        }
        return 0xFFFFFFFF;
    }

    /**
     * Gets the color for this fluid (used by projectile).
     */
    public int getColor(ItemStack stack) {
        int color = CustomFluidNBTHelper.getFluidColor(stack);
        return color != -1 ? color : 0x45E1E9;
    }
}
