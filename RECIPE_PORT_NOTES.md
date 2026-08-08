# Recipe port notes (1.12.2 → 1.16.5)

See [COMPAT_NOTES.md](COMPAT_NOTES.md) for the non-recipe side of mod integration (energy, ship
movement transformers, Dictionary tags, ICBM, ore generation) and the decisions taken there.

Source of truth: `src/legacy/java/config/Recipes.java` (2158 lines, code-registered recipes).
Target: data-driven JSON under `src/main/resources/data/warpdrive/recipes/` (1031 files).

Everything the legacy file registered has been ported except the items listed under
**Not ported** below. This document records every place the port is *not* a literal
transcription, so the deviations are reviewable rather than buried in 900 JSON files.

## Structural tweaks that apply everywhere

### 1. Mod-compat fallbacks collapsed to the vanilla branch
`Recipes.initIngredients()` picked ingredients by scanning for GregTech / IC2 / Thermal
Foundation / EnderIO / ImmersiveEngineering. JSON recipes cannot branch on loaded mods, so
every such choice resolved to the **vanilla fallback** that `WarpDriveConfig.getOreOrItemStack`
would have landed on with no mods present:

| Legacy ingredient | Ported as |
|---|---|
| Machine casing LV / MV / HV / EV | `minecraft:iron_block` / `minecraft:diamond_block` / `warpdrive:highly_advanced_machine` / `minecraft:beacon` |
| Motor LV / MV / HV / EV | `warpdrive:component-motor` (all four tiers) |
| `ingotIronOrSteel`, `ironIngotOrCopperIngotOrCoil*`, `ingotSteelOrIron` | `forge:ingots/iron` |
| `goldNuggetOrBasicCircuit` / `goldIngotOrAdvancedCircuit` / `emeraldOrSuperiorCircuit` | `forge:nuggets/gold` / `forge:ingots/gold` / `forge:gems/emerald` |
| `oreRadarDish` (titanium/enderium/vibrant/iridium plate) | `forge:gems/quartz` |
| `oreRadarSensor` (quartzite rod / signalum / pulsating iron) | `minecraft:ghast_tear` |
| Cloaking coil `oreGoldIngotOrCoil` / `oreGoldIngotOrTitaniumPlate` / `oreEmeraldOrIridiumPlate` | `forge:ingots/gold` / `forge:ingots/gold` / `forge:gems/emerald` |
| Hull tier 2 `oreObsidianTungstenSteelPlate` | `minecraft:obsidian` |
| Hull tier 3 `oreDiamondOrNaquadahPlate` | `forge:gems/diamond` |
| Chiller coolants (cryotheum branch) | snow block / ice / packed ice (the no-cryotheum branch) |
| `enderPearlOrMagnetizer` (laser lift) | `minecraft:ender_pearl` |
| Reactor coolant, overclocked heat vent (IC2) | recipe not ported, see below |

The IC2-only alternate recipes (`highly_advanced_machine` from iridium plates, iridium block from
`plateAlloyIridium` / `plateIridium`, the Thermal/EnderIO iridium block variants, the reinforced-stone
hull recipe, the soldering-alloy computer interface, the "second, more expensive lens" recipe added
when GregTech lenses exist) are all **dropped** — each was a conditional extra alternative, never the
only path to its output.

Ore dictionary names became Forge tags: `forge:ingots/iron`, `forge:nuggets/gold`,
`forge:gems/{quartz,lapis,diamond,emerald}`, `forge:storage_blocks/{redstone,lapis}`,
`forge:dusts/redstone`, `forge:glass/colorless`, `forge:glass_panes/colorless`, `forge:stone`,
`forge:chests/wooden`, `forge:dyes` + `forge:dyes/<colour>`, `minecraft:planks`, `minecraft:wool`,
`minecraft:leaves`. 1.12's `SILVER` dye is `forge:dyes/light_gray`, but the WarpDrive registry names
keep the `silver` spelling.

### 2. ACCELERATOR_ENABLE branches currently retain the `false` branch
Six recipes had two forms in 1.12 depending on `WarpDriveConfig.ACCELERATOR_ENABLE` (default `true`).
JSON cannot branch on config, so the original migration selected the **accelerator-disabled** form
for all six:

- `component-reactor_core` (nether star + superior hull + HV casing)
- `enan_reactor_core.basic`
- `biometric_scanner` (MV casing instead of a basic electromagnet)
- `transporter_containment`, `transporter_scanner` (HV casing instead of an advanced electromagnet)

The accelerator graph and filled particle cells are now functional, so the old dead-end reason no
longer applies. These six recipes intentionally remain on their established fallback ingredients
until they are switched as one balancing change; the accelerator blocks themselves all have recipes.

### 3. Particle-aware recipes preserve and consume their payload
`RecipeParticleShapedOre` carried an electromagnetic cell's stored particle type + amount into the
result. The four advanced/superior electromagnet recipes now use
`warpdrive:particle_shaped`: it keeps the flattened particle item IDs, verifies at least the legacy
200 ions / 24 protons, consumes exactly that amount and returns the partially drained or empty cell.
Potion ingredients remain on Forge's `forge:nbt` ingredient, matching the convention already used
by `component-activated_carbon`.

### 4. Metadata subtypes that became NBT "catalogue variants"
1.13 flattening turned some 1.12 metadata into cosmetic NBT on a single item, and vanilla recipe
results cannot set NBT. Affected:

- **Air shield**: one colourless recipe instead of 16 dyed ones; the two dye-recolour recipes are
  dropped.
- **Force field projector**: the "single" (`" e "/"pm "/" r "` and its mirror) and "double"
  (`" e "/"pmp"/" r "`) recipes all produce the same item — the single/double distinction was
  metadata. All three patterns are kept.
- **Hull slabs**: 1.12 had four slab metas (plain/tiled × bottom/top). 1.16 uses one slab item with
  a blockstate, so there is one `3 plain → 6 slab` recipe and one `2 slab → 1 plain` uncrafting
  recipe per tier/colour. The tiled-slab recipes and the double-slab uncrafting recipes are dropped.

### 5. Ship cores and controllers
`warpdrive:ship_core` / `warpdrive:ship_controller` are the *functional* blocks in this port and
already carried the 1.12 basic-tier recipe. `ship_core.{basic,advanced,superior}` and
`ship_controller.{basic,advanced,superior}` are catalogue blocks. To avoid two identical patterns
producing different items, the tiered chain starts from the functional block:

- `ship_core.advanced` consumes `warpdrive:ship_core`; `ship_core.superior` consumes `ship_core.advanced`
- same for the controllers
- `ship_core.basic` / `ship_controller.basic` have **no** recipe (the functional block is the basic tier)
- `weapon_controller` consumes `ship_controller.advanced`; `cloaking_core` consumes `ship_controller.superior`

Revisit this once the tiered blocks become functional.

### 6. Tuning driver configuration

`RecipeTuningDriver` is ported as three special recipe serializers, one for each flattened driver
mode. The recipe takes the matching driver, one redstone dust and an ordered dye sequence: seven
dyes for video/control channels or four for beam frequency. As in 1.12.2, legacy dye damage values
(black `0` through white `15`) are read in crafting-slot order as hexadecimal digits and written to
the driver's channel NBT. Right-clicking air also cycles between the three registry items while
retaining that NBT, so the beam-frequency and control-channel variants are obtainable again.

## Not ported

| Legacy recipe | Reason |
|---|---|
| `itemIC2reactorLaserFocus`, `blockIC2reactorLaserCooler` | Ported as *conditional* recipes — they only load when IC2 is present, which no 1.16.5 build is. See **TODO: mod-gated recipes** below |
| ICBM Classic antimatter + red matter explosives | ICBM Classic-gated, see **TODO: mod-gated recipes** below |
| Plasma torches | Already commented out in 1.12 |
| Ship scanner, chunk loaders, creative capacitor, bedrock glass, gas blocks | No recipe in 1.12 either (creative-only) |
| `initOreDictionary()` | Superseded by the tag files under `data/warpdrive/tags/` |

## TODO: mod-gated recipes

Deferred, not abandoned. Planned version path is **1.16.5 → 1.21.1 → 1.26.3**. These are recorded
here so the decision stays reversible without re-reading the 1.12.2 source. Revisit at each port hop,
and only implement if the target mod actually exists for that Minecraft version.

### Upstream availability (checked 2026-08-07)

| Mod | 1.12.2 (legacy) | 1.16.5 | Notes |
|---|---|---|---|
| Immersive Engineering | yes | **yes** | actively maintained through 1.21.1 |
| Thermal Expansion / Foundation | yes | **yes** | Thermal Series, last 1.16.5 build Nov 2022 |
| Big Reactors | dead | — | Erogenous Beef's original stopped at 1.10.2; two independent successors below |
| ↳ Extreme Reactors (ZeroNoRyouki) | yes, as modid `bigreactors` | **yes** | `ExtremeReactors2-1.16.5-2.0.32`. This is what WarpDrive's filler ids actually point at |
| ↳ Bigger Reactors (RogueLogix) | n/a | **yes** | modid `biggerreactors`, 1.15.2–1.20.1. Separate codebase, not a fork of Extreme Reactors |
| Ender IO | yes | **no** | jumps 1.12.2 → 1.20.1 / 1.21.1 |
| GregTech (CEu) | yes | **no** | 1.12.2, then GTCEu Modern on 1.19.2 / 1.20.1+ |
| IndustrialCraft 2 | yes | **no** | 1.12.2 is the last release (June 2022) |
| Advanced Solar Panels | yes | **no** | IC2 addon, dies with IC2 |
| GraviSuite | yes | **no** | IC2 addon; "Gravisuit Classic" follows IC2 Classic instead |
| ICBM Classic | yes | **no** | 1.7.10 and 1.12.2 only |
| MFFS | yes | **no** | 1.12.2 is the last legacy build; an unrelated rewrite covers 1.20.1 / 1.21.1+ |
| Advanced Repulsion Systems | yes | **no** | 1.7.10 era, long dead |

Big Reactors is worth spelling out because it is not a recipe integration at all. 1.12.2 had **no**
`CompatBigReactors` module; the only touchpoints are `src/main/resources/config/fillerSets-*.xml`
(`bigreactors:brore` metadata 0/1/2 = yellorite/anglesite/benitoite, `bigreactors:blockmetals:1` =
cyanite) and the README's claim of RF interop, which the generic Forge Energy path already covers.
Those filler ids use Extreme Reactors' namespace — the original Big Reactors never shipped for
1.12.2, and Extreme Reactors kept the `bigreactors` modid. They need rewriting for 1.16.5 regardless,
since 1.13 flattening removed the metadata subtypes, and `LegacyAsteroidGenerator` currently reads
only the vanilla entries from those files, so every modded filler line is inert today.

So on 1.16.5 only three of the legacy integrations are even possible: Immersive Engineering, Thermal,
and Extreme Reactors (or Bigger Reactors, if the filler ids are pointed there instead). At the 1.21.1 hop, Ender IO, GregTech (Modern) and MFFS (rewrite — different
mod, different API) come back into range; IC2, ICBM Classic, ASP, GraviSuite and ARS look
permanently gone.

From 1.16.5 onward the gating mechanism is a `forge:conditional` recipe wrapping a
`forge:mod_loaded` condition, which replaces the 1.12.2 `isIndustrialCraft2Loaded` runtime check. A
conditional recipe is simply skipped when the mod is absent, so mod-gated recipes are safe to ship
*before* the mod exists.

### IndustrialCraft 2 (`Recipes.initEnergy()`) — DONE, dormant

Both recipes ship as `forge:conditional` on `modid: ic2` and will stay inert until an IC2 build for
the running version exists:

- `recipes/ic2_reactor_laser_focus.json` — `"cld" / "lhl" / "dlc"`, `l` = `component-lens`,
  `h` = `ic2:overclocked_heat_vent`, `c` and `d` = 60k coolant cell (`ic2:hex_heat_storage`).
- `recipes/ic2_reactor_laser_cooler.json` — `"gCp" / "lme" / "gC "`, `l` = `component-lens`,
  `e` = `component-emerald_crystal`, `C` = `component-capacitive_crystal`,
  `p` = `component-power_interface`, `g` = `forge:glass_panes/colorless`, `m` = MV machine casing.

**Caveat:** the two `ic2:` item ids are the 1.12.2 Experimental ids. IC2 Classic used
`ic2:itemheatvent:2` and `ic2:itemheatstorage:2`, and a future port could rename either. If IC2 ever
returns, verify both ids — a wrong id in a condition-satisfied recipe is a datapack load error, not a
silent skip.

### IC2 alternates dropped from ported recipes

If IC2 support returns, these 1.12.2 alternatives should come back as conditional recipes rather than
replacing what is already ported:

- `highly_advanced_machine` from 8 `plateAlloyIridium` + an MV machine casing.
- `iridium_block` from 9 `plateAlloyIridium`, plus its 1 → 9 uncrafting recipe.
- Basic hull tier 1 from 4 obsidian + 4 IC2 reinforced stone + dye, giving 10.
- Machine casings and motors resolving to real IC2 tiers instead of the vanilla stand-ins in the
  table at the top of this document.
- Laser lift accepting an IC2 Magnetizer in place of the ender pearl.
- Superconductor accepting a 10k coolant cell in place of the lapis block.
- Hull tier 2 accepting an IC2 carbon plate; hull tier 3 accepting an iridium plate.

### ICBM Classic (`Recipes.initAtomic()`)

Both replace an existing ICBM recipe at runtime (`removeRecipe`), which in 1.16.5+ means shipping a
recipe under the `icbmclassic` namespace to override it rather than mutating the registry:

- **Antimatter explosive** — 8 advanced antimatter cells (1000 units) around 1 `icbmclassic:explosives:15`.
- **Red matter explosive** — 8 advanced strange matter cells (1000 units) around 1 antimatter explosive.

Filled electromagnetic cells and their amount semantics now exist; only the absent ICBM integration
and cross-namespace recipe overrides keep these two recipes deferred.

## Other notes

- `warpdrive:book` (the Patchouli manual) is registered unconditionally for save compatibility, but
  its recipe is again gated on Patchouli with `forge:conditional`, matching the 1.12 availability
  contract. The WarpDrive-owned item opens the real Patchouli book through the optional compat bridge.
- New item tags were generated for the hull families —
  `warpdrive:hulls/{basic,advanced,superior}/{plain,tiled,glass,stairs,slab,omnipanel}` — replacing
  the `blockHull1_plain`-style ore dictionary entries, plus
  `warpdrive:electromagnets/{advanced,superior}` to match the existing `basic` tag.
- The hull set is generated combinatorially (3 tiers × 16 colours × 15 recipes ≈ 816 files),
  mirroring what 1.12 built in code.
