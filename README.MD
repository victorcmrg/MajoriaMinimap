## Vanilla Minimaps

-----

A minimap plugin, without any clientside mods.

https://github.com/JNNGL/VanillaMinimaps/assets/45935949/4a6f9cf4-0575-4a46-852e-da11a18edc56

https://github.com/JNNGL/VanillaMinimaps/assets/45935949/ea7738c9-bd4a-4388-9d65-109aa9233fcc

https://github.com/JNNGL/VanillaMinimaps/assets/45935949/f64aed82-8193-4c3b-9c8f-be32dc3d6fa0

**Supported server version: 1.21.10** (Paper)

### Features

- Clientside layer entities
- Smooth clientside rotation
- Real-time world rendering
- Position interpolation
- Better map colors
- Configurable markers
- Fullscreen map view
- Configurable blockstate colors
- Other players shown on the minimap

### Other players on the minimap

Other online players near you are shown on the minimap and follow their movement.
Settings are under `markers.player-markers` in `config.yml` (radius, max players, hide spectators/invisible/sneaking).

By default they are drawn as a **pink dot**. To use a custom icon, put a PNG named `other_player.png` into
`plugins/VanillaMinimaps/icons/` (or into `src/main/resources/minimap/` before building). The plugin checks for
the file every few seconds, so the icon shows up without a restart.

### Resource pack

The shaders in `src/main/resources/shaders` must be shipped to the clients in a resource pack under
`assets/minecraft/shaders/` (Minecraft 1.21.10, pack format 69):

```
pack.mcmeta
assets/minecraft/shaders/core/rendertype_text.vsh
assets/minecraft/shaders/core/rendertype_text.fsh
assets/minecraft/shaders/include/minimap/*.glsl
```

```json
{
  "pack": {
    "description": "Vanilla Minimaps",
    "pack_format": 69,
    "min_format": 69,
    "max_format": 69
  }
}
```

### Mods compatibility

- [x] Sodium (without shaders)
- [x] OptiFine (without shaders)
- [ ] Iris/OptiFine shader packs
- [ ] MoreCulling
- [ ] ImmediatelyFast

### Common problems

**The plugin isn't working!!!!!! I get two maps, one above my chest and the other one above my head.**
> This means that the resource pack is not installed properly, or you have iris/optifine shaders enabled. 
> If you are using plugins like ItemsAdder, they may overwrite shaders in the resource pack, 
> ask the dev of the plugin you are using to manage the resource pack.

**This doesn't work with optifine/iris shaderpacks.**
> Optifine/Iris override vanilla shaders. This can't be fixed.

**This doesn't work on Bedrock with Geyser.**
> The plugin uses vanilla shaders to display the minimap, this is an exclusive feature for the Java Edition. 
> Bedrock will never be supported by this plugin.

**The minimap is not displayed correctly with mod <...>**
> There are some mods that break the minimap, compatibility with some of them can be fixed and with some not. 
> Check [Mods compatibility](https://github.com/JNNGL/VanillaMinimaps?tab=readme-ov-file#mods-compatibility) 
> and [Issues](https://github.com/JNNGL/VanillaMinimaps/issues). If you haven't found any information for a mod 
> that breaks the minimap, feel free to open an issue.

### Credits

- **Godlander's map colors**: [*Link*](https://github.com/Godlander/vpp/blob/main/assets/minecraft/shaders/core/render/text.fsh) (Used in this plugin to significantly improve the look of minimaps by tweaking map colors)
