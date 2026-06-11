# HotBath Custom Fluid Extension System

This system allows you to create custom bath fluids using data packs. Custom fluids use a grayscale texture that is tinted at runtime with your specified color, and can apply configurable potion effects to players.

## Quick Start

1. Create a JSON file in your data pack at:

   ```
   data/<your_namespace>/hotbath/custom_fluids/<your_fluid>.json
   ```

2. Define your fluid using the JSON schema below.

3. Reload data packs (`/reload` command) to load your custom fluids.

## JSON Schema

```json
{
  "id": "yourmod:your_bath",
  "color": 16766720,
  "temperature": 40.0,
  "viscosity": 1000,
  "density": 1000,
  "luminosity": 2,
  "show_particles": true,
  "show_bubbles": true,
  "show_steam": true,
  "catalyst_item": "minecraft:gold_ingot",
  "trigger_time_seconds": 15,
  "effects": [
    {
      "effect": "minecraft:regeneration",
      "duration": 200,
      "amplifier": 1,
      "ambient": true,
      "show_particles": false,
      "show_icon": true
    }
  ],
  "texture_still": "hotbath:block/still_custom_fluid_grayscale",
  "texture_flowing": "hotbath:block/flowing_custom_fluid_grayscale"
}
```

## Field Reference

### Required Fields

| Field | Type             | Description                                                  |
| ----- | ---------------- | ------------------------------------------------------------ |
| `id`  | ResourceLocation | Unique identifier for your fluid (e.g., "mymod:golden_bath") |

### Optional Fields

| Field                  | Type             | Default                                      | Description                                           |
| ---------------------- | ---------------- | -------------------------------------------- | ----------------------------------------------------- |
| `name_key`             | String           | "fluid.{namespace}.{path}"                   | Translation key for localization (see Localization)   |
| `color`                | Integer          | 4579817 (cyan)                               | RGB color value (see Color Calculation)               |
| `temperature`          | Float            | 40.0                                         | Temperature in Celsius (visual only)                  |
| `viscosity`            | Integer          | 1000                                         | Flow resistance (higher = slower flow)                |
| `density`              | Integer          | 1000                                         | Fluid density (affects buoyancy)                      |
| `luminosity`           | Integer          | 2                                            | Light level emitted (0-15)                            |
| `show_particles`       | Boolean          | true                                         | Whether to show splash particles                      |
| `show_bubbles`         | Boolean          | true                                         | Whether to show bubble particles                      |
| `show_steam`           | Boolean          | true                                         | Whether to show steam particles                       |
| `catalyst_item`        | ResourceLocation | null                                         | Item used to create this bath (for recipes)           |
| `trigger_time_seconds` | Integer          | 15                                           | Seconds player must stay in bath before effects apply |
| `effects`              | Array            | []                                           | List of potion effects to apply                       |
| `texture_still`        | ResourceLocation | hotbath:block/still_custom_fluid_grayscale   | Still texture                                         |
| `texture_flowing`      | ResourceLocation | hotbath:block/flowing_custom_fluid_grayscale | Flowing texture                                       |

### Effect Entry Fields

| Field            | Type             | Default  | Description                                           |
| ---------------- | ---------------- | -------- | ----------------------------------------------------- |
| `effect`         | ResourceLocation | Required | The potion effect ID (e.g., "minecraft:regeneration") |
| `duration`       | Integer          | 200      | Effect duration in ticks (20 ticks = 1 second)        |
| `amplifier`      | Integer          | 0        | Effect level (0 = Level I, 1 = Level II, etc.)        |
| `ambient`        | Boolean          | false    | Whether effect particles are more translucent         |
| `show_particles` | Boolean          | true     | Whether to show effect particles on player            |
| `show_icon`      | Boolean          | true     | Whether to show effect icon in HUD                    |

## Localization

Custom fluids support localization through the `name_key` field. This allows you to provide translated names for your fluids in different languages.

### How It Works

1. **Default Behavior**: If `name_key` is not specified, the system generates a translation key automatically:

   ```
   fluid.<namespace>.<path>
   ```

   For example, `mymod:golden_bath` becomes `fluid.mymod.golden_bath`.

2. **Custom Key**: You can specify a custom translation key:
   ```json
   {
     "id": "mymod:special_bath",
     "name_key": "item.mymod.my_custom_translation_key",
     ...
   }
   ```

### Adding Translations

Add your translations to the language files in your resource pack:

**English (`assets/<namespace>/lang/en_us.json`):**

```json
{
  "fluid.mymod.golden_bath": "Golden Bath",
  "fluid.mymod.enchanted_bath": "Enchanted Bath"
}
```

**Chinese (`assets/<namespace>/lang/zh_cn.json`):**

```json
{
  "fluid.mymod.golden_bath": "黄金浴",
  "fluid.mymod.enchanted_bath": "附魔浴"
}
```

### Item Names

The translated fluid name is used to construct item names using these patterns:

- **Bucket**: `%s Bucket` / `%s桶`
- **Bottle**: `%s Bottle` / `%s瓶`
- **Splash Bottle**: `Splash %s Bottle` / `喷溅型%s瓶`

For example, if `fluid.hotbath.golden_bath` = "Golden Bath", the bucket will display as "Golden Bath Bucket".

## Color Calculation

Colors are specified as RGB integers. Calculate using:

```
color = (red × 65536) + (green × 256) + blue
```

### Common Colors

| Color        | RGB           | Value    |
| ------------ | ------------- | -------- |
| Gold         | 255, 215, 0   | 16766720 |
| Lapis Blue   | 60, 68, 170   | 3949738  |
| Slime Green  | 113, 195, 93  | 7458653  |
| Blaze Orange | 255, 127, 0   | 16744448 |
| Ender Purple | 21, 21, 117   | 1381717  |
| Pure Red     | 255, 0, 0     | 16711680 |
| Pure Green   | 0, 255, 0     | 65280    |
| Pure Blue    | 0, 0, 255     | 255      |
| White        | 255, 255, 255 | 16777215 |

## Examples

### Golden Regeneration Bath

```json
{
  "id": "hotbath:golden_bath",
  "color": 16766720,
  "temperature": 42.0,
  "luminosity": 6,
  "catalyst_item": "minecraft:gold_ingot",
  "trigger_time_seconds": 10,
  "effects": [
    {
      "effect": "minecraft:regeneration",
      "duration": 400,
      "amplifier": 1
    },
    {
      "effect": "minecraft:absorption",
      "duration": 600,
      "amplifier": 1
    }
  ]
}
```

### Fire Resistance Bath

```json
{
  "id": "mymod:blazing_bath",
  "color": 16744448,
  "temperature": 80.0,
  "luminosity": 12,
  "show_bubbles": false,
  "catalyst_item": "minecraft:blaze_powder",
  "effects": [
    {
      "effect": "minecraft:fire_resistance",
      "duration": 1200,
      "amplifier": 0
    }
  ]
}
```

### Quiet Meditation Bath (No Particles)

```json
{
  "id": "mymod:quiet_bath",
  "color": 8421504,
  "show_particles": false,
  "show_bubbles": false,
  "show_steam": false,
  "trigger_time_seconds": 30,
  "effects": [
    {
      "effect": "minecraft:night_vision",
      "duration": 600,
      "ambient": true,
      "show_particles": false
    }
  ]
}
```

## API Usage (For Mod Developers)

```java
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import net.minecraft.resources.ResourceLocation;

// Check if a fluid exists
boolean exists = CustomFluidAPI.isFluidRegistered(
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));

// Get fluid properties
int color = CustomFluidAPI.getFluidColor(
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));
float temp = CustomFluidAPI.getFluidTemperature(
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));

// Apply effects to a player
CustomFluidAPI.applyFluidEffects(serverPlayer,
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));

// Create a fluid programmatically
CustomFluidDefinition custom = CustomFluidAPI.builder(
        ResourceLocation.fromNamespaceAndPath("mymod", "my_bath"))
    .color(CustomFluidAPI.rgb(255, 100, 50))
    .temperature(45.0f)
    .luminosity(4)
    .effects(List.of(
        CustomFluidAPI.createEffect(
            ResourceLocation.parse("minecraft:haste"),
            400, 1)
    ))
    .build();
```

### External Bath Container Integration

Other mods should use the public integration surface instead of depending on HotBath internals:

```java
import com.crabmod.hotbath.api.HotBathApi;

if (HotBathApi.isCleansingFluid(storedFluid)) {
    HotBathApi.applyDirtinessCleaning(serverPlayer, storedFluid, isMoving);
}
```

The same contract is also available to data packs and tags through:

- `hotbath:bath_fluids`
- `hotbath:cleansing_fluids`

External bath containers are responsible for validating their own fluid storage, shape, and immersion checks before calling the API.

## Custom Textures

You can provide your own grayscale textures by:

1. Create your textures (should be grayscale for proper tinting)
2. Place them in your resource pack at:
   ```
   assets/<namespace>/textures/block/<your_texture>.png
   ```
3. Reference them in your JSON:
   ```json
   {
     "texture_still": "mymod:block/my_still_texture",
     "texture_flowing": "mymod:block/my_flowing_texture"
   }
   ```

## Tips

1. **Duration**: 20 ticks = 1 second. A duration of 200 = 10 seconds.

2. **Trigger Time**: Set higher trigger times for powerful effects to require players to stay in the bath longer.

3. **Ambient Effects**: Set `ambient: true` for effects that should feel more natural with subtle particles.

4. **Luminosity**: Higher values (up to 15) make the fluid glow more. Use for magical or hot fluids.

5. **Temperature**: Currently visual/informational only, but can be used by other mods for integration.
