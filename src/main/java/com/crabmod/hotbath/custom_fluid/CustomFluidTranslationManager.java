package com.crabmod.hotbath.custom_fluid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic translations for custom fluids loaded from data packs.
 * This allows data pack authors to include translations directly in their fluid definitions,
 * without needing a separate resource pack.
 * 
 * <p>Translations are stored per-language and looked up at runtime. When a translation
 * key is requested, the manager first checks if a custom translation exists for the
 * current game language, then falls back to English (en_us), and finally to the
 * translation key itself.
 */
public class CustomFluidTranslationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFluidTranslationManager.class);
    
    // Map of language code -> (translation key -> translated text)
    private static final Map<String, Map<String, String>> TRANSLATIONS = new ConcurrentHashMap<>();
    
    // Default fallback language
    private static final String DEFAULT_LANGUAGE = "en_us";
    
    /**
     * Registers a translation for a specific language.
     * 
     * @param languageCode The language code (e.g., "en_us", "zh_cn")
     * @param key The translation key (e.g., "fluid.hotbath.golden_bath")
     * @param value The translated text
     */
    public static void registerTranslation(String languageCode, String key, String value) {
        TRANSLATIONS.computeIfAbsent(languageCode.toLowerCase(), k -> new ConcurrentHashMap<>())
                .put(key, value);
    }
    
    /**
     * Registers translations from a fluid definition's translations map.
     * The map should have language codes as keys and translated names as values.
     * Keys with a ".desc" suffix are treated as description translations and registered
     * under the translationKey + ".desc" key for the base language.
     *
     * @param translationKey The translation key for this fluid
     * @param translations Map of language code (or languageCode.desc) to translated name
     */
    public static void registerTranslations(String translationKey, Map<String, String> translations) {
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String mapKey = entry.getKey();
            if (mapKey.endsWith(".desc")) {
                // "zh_hk.desc" → language "zh_hk", key "translationKey.desc"
                String langCode = mapKey.substring(0, mapKey.length() - ".desc".length());
                registerTranslation(langCode, translationKey + ".desc", entry.getValue());
            } else {
                registerTranslation(mapKey, translationKey, entry.getValue());
            }
        }
    }
    
    // Locale fallback chains for regional variants
    private static final Map<String, List<String>> LOCALE_FALLBACKS = Map.of(
            "zh_hk", List.of("zh_hk", "zh_tw", "zh_cn"),
            "zh_tw", List.of("zh_tw", "zh_cn"),
            "zh_cn", List.of("zh_cn", "zh_tw"),
            "pt_br", List.of("pt_br", "pt_pt"),
            "pt_pt", List.of("pt_pt", "pt_br"),
            "en_gb", List.of("en_gb", "en_us"),
            "en_au", List.of("en_au", "en_gb", "en_us")
    );

    /**
     * Gets the translation for a key in the current game language.
     * Uses a locale fallback chain (e.g., zh_hk → zh_tw → zh_cn → en_us).
     *
     * @param key The translation key
     * @return The translated text, or the key if no translation exists
     */
    public static String getTranslation(String key) {
        // First check if vanilla I18n has this key
        if (I18n.exists(key)) {
            return I18n.get(key);
        }

        // Get current language
        String currentLang = getCurrentLanguage();

        // Try locale fallback chain (e.g., zh_hk → zh_tw → zh_cn)
        List<String> fallbackChain = LOCALE_FALLBACKS.get(currentLang);
        if (fallbackChain != null) {
            for (String fallbackLang : fallbackChain) {
                String result = getTranslationForLanguage(fallbackLang, key);
                if (result != null) {
                    return result;
                }
            }
        } else {
            // No fallback chain defined, just try current language
            String result = getTranslationForLanguage(currentLang, key);
            if (result != null) {
                return result;
            }
        }

        // Try fallback to English
        if (!currentLang.equals(DEFAULT_LANGUAGE)) {
            String result = getTranslationForLanguage(DEFAULT_LANGUAGE, key);
            if (result != null) {
                return result;
            }
        }

        // Return the key itself as last resort
        return key;
    }
    
    /**
     * Checks if a translation exists for the given key.
     * 
     * @param key The translation key
     * @return true if a translation exists (either in vanilla or custom)
     */
    public static boolean hasTranslation(String key) {
        if (I18n.exists(key)) {
            return true;
        }
        
        String currentLang = getCurrentLanguage();
        if (getTranslationForLanguage(currentLang, key) != null) {
            return true;
        }
        
        return getTranslationForLanguage(DEFAULT_LANGUAGE, key) != null;
    }
    
    /**
     * Gets a translation for a specific language.
     * 
     * @param languageCode The language code
     * @param key The translation key
     * @return The translation, or null if not found
     */
    private static String getTranslationForLanguage(String languageCode, String key) {
        Map<String, String> langTranslations = TRANSLATIONS.get(languageCode.toLowerCase());
        if (langTranslations != null) {
            return langTranslations.get(key);
        }
        return null;
    }
    
    /**
     * Gets the current game language code via LanguageManager (most reliable API).
     * Falls back to Options.languageCode, then to "en_us".
     */
    private static String getCurrentLanguage() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                // Prefer LanguageManager.getSelected() - this is the authoritative source
                if (mc.getLanguageManager() != null) {
                    String selected = mc.getLanguageManager().getSelected();
                    if (selected != null && !selected.isEmpty()) {
                        return selected;
                    }
                }
                // Fallback to options field
                if (mc.options != null && mc.options.languageCode != null) {
                    return mc.options.languageCode;
                }
            }
        } catch (Exception e) {
            // May fail during early loading
        }
        return DEFAULT_LANGUAGE;
    }
    
    /**
     * Clears all registered translations.
     * Called when reloading data packs.
     */
    public static void clearAll() {
        TRANSLATIONS.clear();
    }
    
    /**
     * Gets the total number of registered translations.
     */
    public static int getTranslationCount() {
        return TRANSLATIONS.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
