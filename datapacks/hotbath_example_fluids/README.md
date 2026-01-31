# HotBath Example Custom Fluids

This is an example data pack demonstrating the HotBath custom fluid extension system.

## Installation

### For Players

1. Copy the entire `hotbath_example_fluids` folder to your world's `datapacks` folder:

   ```
   .minecraft/saves/<your_world>/datapacks/hotbath_example_fluids/
   ```

2. Also copy it to the `resourcepacks` folder for translations:

   ```
   .minecraft/resourcepacks/hotbath_example_fluids/
   ```

3. In-game, run `/reload` to load the data pack.

4. Enable the resource pack in Options > Resource Packs.

### For Mod Developers

This pack serves as an example for creating your own custom fluids. See the `data/hotbath/hotbath/custom_fluids/` folder for JSON examples.

## Recipes & Interactions

### Obtaining Items

| Action                                  | Result                                      |
| --------------------------------------- | ------------------------------------------- |
| Right-click fluid with **Empty Bucket** | Get **Custom Fluid Bucket** (removes fluid) |
| Right-click fluid with **Glass Bottle** | Get **Custom Fluid Bottle**                 |

### Brewing Recipes

All custom fluids support brewing in the brewing stand:

| Input               | Ingredient | Output                     |
| ------------------- | ---------- | -------------------------- |
| Custom Fluid Bottle | Gunpowder  | Splash Custom Fluid Bottle |

This works automatically for any custom fluid defined in data packs.

## Included Fluids

| Fluid ID                 | Color        | Catalyst Item | Effects                        |
| ------------------------ | ------------ | ------------- | ------------------------------ |
| `hotbath:golden_bath`    | Gold         | Gold Ingot    | Regeneration II, Absorption II |
| `hotbath:enchanted_bath` | Lapis Blue   | Lapis Lazuli  | Luck II, Night Vision          |
| `hotbath:slime_bath`     | Slime Green  | Slime Ball    | Jump Boost III, Slow Falling   |
| `hotbath:blazing_bath`   | Blaze Orange | Blaze Powder  | Fire Resistance, Strength II   |
| `hotbath:ender_bath`     | Ender Purple | Ender Pearl   | Invisibility, Speed III        |

## Structure

```
hotbath_example_fluids/
├── pack.mcmeta                    # Pack metadata (format 15 for 1.20.x)
├── README.md                      # This file
├── data/
│   └── hotbath/
│       └── hotbath/
│           └── custom_fluids/     # Custom fluid definitions
│               ├── golden_bath.json
│               ├── enchanted_bath.json
│               ├── slime_bath.json
│               ├── blazing_bath.json
│               └── ender_bath.json
└── assets/
    └── hotbath/
        └── lang/                  # Translations
            ├── en_us.json         # English
            ├── zh_cn.json         # Simplified Chinese
            └── zh_tw.json         # Traditional Chinese
```

## Creating Your Own

To create your own custom fluid pack:

1. Copy this folder structure
2. Edit `pack.mcmeta` with your pack description
3. Create new JSON files in `data/<your_namespace>/hotbath/custom_fluids/`
4. Add translations to `assets/<your_namespace>/lang/`

### JSON Schema

```json
{
  "id": "yourmod:your_bath",
  "name_key": "fluid.yourmod.your_bath",
  "color": 16766720,
  "temperature": 40.0,
  "catalyst_item": "minecraft:gold_ingot",
  "trigger_time_seconds": 15,
  "effects": [
    {
      "effect": "minecraft:regeneration",
      "duration": 200,
      "amplifier": 1
    }
  ]
}
```

See the main mod's documentation for full field reference.

## License

This example pack is provided as a reference implementation. Feel free to use it as a template for your own custom fluid packs.
