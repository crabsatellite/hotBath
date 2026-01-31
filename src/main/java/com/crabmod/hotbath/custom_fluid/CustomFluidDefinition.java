package com.crabmod.hotbath.custom_fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data-driven custom fluid definition that can be loaded from data packs.
 * Users can create JSON files in data/&lt;namespace&gt;/hotbath/custom_fluids/ to define custom bath liquids.
 * 
 * <p>Example JSON:
 * <pre>
 * {
 *   "id": "example:golden_bath",
 *   "color": 16766720,
 *   "temperature": 40.0,
 *   "viscosity": 1000,
 *   "density": 1000,
 *   "luminosity": 4,
 *   "show_particles": true,
 *   "show_bubbles": true,
 *   "show_steam": true,
 *   "effects": [
 *     {
 *       "effect": "minecraft:regeneration",
 *       "duration": 200,
 *       "amplifier": 1,
 *       "ambient": true,
 *       "show_particles": false,
 *       "show_icon": true
 *     }
 *   ],
 *   "trigger_time_seconds": 15,
 *   "texture_still": "hotbath:block/still_custom_fluid_grayscale",
 *   "texture_flowing": "hotbath:block/flowing_custom_fluid_grayscale"
 * }
 * </pre>
 */
public record CustomFluidDefinition(
        ResourceLocation id,
        int color,
        float temperature,
        int viscosity,
        int density,
        int luminosity,
        boolean showParticles,
        boolean showBubbles,
        boolean showSteam,
        List<EffectEntry> effects,
        int triggerTimeSeconds,
        ResourceLocation textureStill,
        ResourceLocation textureFlowing,
        String nameKey,
        Map<String, String> translations
) {
    
    public static final Codec<CustomFluidDefinition> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(CustomFluidDefinition::id),
                    Codec.INT.optionalFieldOf("color", 0x45E1E9).forGetter(CustomFluidDefinition::color),
                    Codec.FLOAT.optionalFieldOf("temperature", 40.0f).forGetter(CustomFluidDefinition::temperature),
                    Codec.INT.optionalFieldOf("viscosity", 1000).forGetter(CustomFluidDefinition::viscosity),
                    Codec.INT.optionalFieldOf("density", 1000).forGetter(CustomFluidDefinition::density),
                    Codec.INT.optionalFieldOf("luminosity", 2).forGetter(CustomFluidDefinition::luminosity),
                    Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(CustomFluidDefinition::showParticles),
                    Codec.BOOL.optionalFieldOf("show_bubbles", true).forGetter(CustomFluidDefinition::showBubbles),
                    Codec.BOOL.optionalFieldOf("show_steam", true).forGetter(CustomFluidDefinition::showSteam),
                    EffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(CustomFluidDefinition::effects),
                    Codec.INT.optionalFieldOf("trigger_time_seconds", 15).forGetter(CustomFluidDefinition::triggerTimeSeconds),
                    ResourceLocation.CODEC.optionalFieldOf("texture_still", 
                            new ResourceLocation("hotbath", "block/still_custom_fluid_grayscale"))
                            .forGetter(CustomFluidDefinition::textureStill),
                    ResourceLocation.CODEC.optionalFieldOf("texture_flowing",
                            new ResourceLocation("hotbath", "block/flowing_custom_fluid_grayscale"))
                            .forGetter(CustomFluidDefinition::textureFlowing),
                    Codec.STRING.optionalFieldOf("name_key", "").forGetter(CustomFluidDefinition::nameKey),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("translations", Map.of())
                            .forGetter(CustomFluidDefinition::translations)
            ).apply(instance, CustomFluidDefinition::new)
    );

    /**
     * Gets the translation key for this fluid's name.
     * If a custom name_key is provided in the data pack, it is used directly.
     * Otherwise, generates a key based on the fluid ID: "fluid.{namespace}.{path}"
     * 
     * @return The translation key for this fluid
     */
    public String getTranslationKey() {
        if (nameKey != null && !nameKey.isEmpty()) {
            return nameKey;
        }
        return "fluid." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /**
     * Gets the translated name of this fluid using the translation manager.
     * Falls back to the translation key if no translation is found.
     */
    public String getTranslatedName() {
        return CustomFluidTranslationManager.getTranslation(getTranslationKey());
    }

    /**
     * Registers all translations from this definition to the translation manager.
     */
    public void registerTranslations() {
        if (!translations.isEmpty()) {
            CustomFluidTranslationManager.registerTranslations(getTranslationKey(), translations);
        }
    }

    /**
     * Creates MobEffectInstance list from the effect entries.
     */
    public List<MobEffectInstance> createEffectInstances() {
        return effects.stream()
                .map(EffectEntry::toMobEffectInstance)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Whether this fluid should be considered "hot" (temperature >= 30°C).
     */
    public boolean isHot() {
        return temperature >= 30.0f;
    }

    /**
     * Represents a single effect entry in the fluid definition.
     */
    public record EffectEntry(
            ResourceLocation effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon
    ) {
        public static final Codec<EffectEntry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("effect").forGetter(EffectEntry::effect),
                        Codec.INT.optionalFieldOf("duration", 200).forGetter(EffectEntry::duration),
                        Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectEntry::amplifier),
                        Codec.BOOL.optionalFieldOf("ambient", false).forGetter(EffectEntry::ambient),
                        Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(EffectEntry::showParticles),
                        Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(EffectEntry::showIcon)
                ).apply(instance, EffectEntry::new)
        );

        /**
         * Converts this entry to a MobEffectInstance.
         * Returns null if the effect is not found in the registry.
         */
        public MobEffectInstance toMobEffectInstance() {
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect);
            if (mobEffect == null) {
                return null;
            }
            return new MobEffectInstance(mobEffect, duration, amplifier, ambient, showParticles, showIcon);
        }
    }

    /**
     * Builder for programmatic creation of CustomFluidDefinition
     */
    public static class Builder {
        private ResourceLocation id;
        private int color = 0x45E1E9;
        private float temperature = 40.0f;
        private int viscosity = 1000;
        private int density = 1000;
        private int luminosity = 2;
        private boolean showParticles = true;
        private boolean showBubbles = true;
        private boolean showSteam = true;
        private List<EffectEntry> effects = List.of();
        private int triggerTimeSeconds = 15;
        private ResourceLocation textureStill = new ResourceLocation("hotbath", "block/still_custom_fluid_grayscale");
        private ResourceLocation textureFlowing = new ResourceLocation("hotbath", "block/flowing_custom_fluid_grayscale");
        private String nameKey = "";
        private Map<String, String> translations = new HashMap<>();

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder viscosity(int viscosity) {
            this.viscosity = viscosity;
            return this;
        }

        public Builder density(int density) {
            this.density = density;
            return this;
        }

        public Builder luminosity(int luminosity) {
            this.luminosity = luminosity;
            return this;
        }

        public Builder showParticles(boolean showParticles) {
            this.showParticles = showParticles;
            return this;
        }

        public Builder showBubbles(boolean showBubbles) {
            this.showBubbles = showBubbles;
            return this;
        }

        public Builder showSteam(boolean showSteam) {
            this.showSteam = showSteam;
            return this;
        }

        public Builder effects(List<EffectEntry> effects) {
            this.effects = effects;
            return this;
        }
        
        public Builder nameKey(String nameKey) {
            this.nameKey = nameKey;
            return this;
        }
        
        public Builder translation(String languageCode, String translatedName) {
            this.translations.put(languageCode, translatedName);
            return this;
        }
        
        public Builder translations(Map<String, String> translations) {
            this.translations.putAll(translations);
            return this;
        }

        public Builder triggerTimeSeconds(int seconds) {
            this.triggerTimeSeconds = seconds;
            return this;
        }

        public Builder textureStill(ResourceLocation texture) {
            this.textureStill = texture;
            return this;
        }

        public Builder textureFlowing(ResourceLocation texture) {
            this.textureFlowing = texture;
            return this;
        }

        public CustomFluidDefinition build() {
            return new CustomFluidDefinition(
                    id, color, temperature, viscosity, density, luminosity,
                    showParticles, showBubbles, showSteam,
                    effects, triggerTimeSeconds,
                    textureStill, textureFlowing, nameKey, translations
            );
        }
    }
}
