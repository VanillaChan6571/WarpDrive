# Mod compatibility: what 1.12.2 actually did, and what carries forward

Companion to [RECIPE_PORT_NOTES.md](RECIPE_PORT_NOTES.md), which covers recipe-level mod gating and
the upstream availability table. This file is about the *code* integrations.

The 1.12.2 README claimed support for a dozen mods in two sentences. That claim covered four
completely different mechanisms with wildly different port costs. Decisions taken 2026-08-07 are
marked **DECIDED**.

## 1. Energy interop — **DECIDED: Forge Energy only**

`TileEntityAbstractEnergy` (1.12.2) implemented four APIs: Forge Energy `IEnergyStorage`, RF
(`cofh.redstoneflux`), IC2 `IEnergySink`/`IEnergySource` (EU), and GregTech's
`CAPABILITY_ENERGY_CONTAINER`. Only IC2 and GregTech needed code — EU and GTEU are not Forge Energy.
Everything else named in the README (AdvancedSolarPanel, BigReactors, EnderIO, Thermal Expansion,
ImmersiveEngineering) was never touched by code; they were listed as examples of RF generators.

By 1.16.5 the RF API is gone and Forge Energy is effectively universal, so the generic FE path covers
the entire "RF mod" list on its own.

- **FE**: the only implementation. Done.
- **RF**: no longer a separate thing; nothing to do.
- **EU**: skipped — IC2 has no build past 1.12.2. Revisit only if IC2 or an EU-speaking mod appears.
- **GTEU**: skipped for the same reason; GregTech Modern (1.19.2+) has a different API anyway.

The same decision closes the two reactor-specific registry entries for the 1.16.5 target. The laser
focus implemented IC2's `IReactorComponent`; the cooler tile inspected IC2 reactor chambers and
removed heat from those focus items. With neither API nor reactor host present, substitute behavior
would be a new machine rather than a port. Their registry ids, art and `forge:conditional` recipes
remain dormant for save/data compatibility, and the checklist classifies them as target-unavailable
rather than unfinished placeholders.

## 2. Ship movement block transformers — **DECIDED: no per-mod modules for now**

The 52 `compat/*.java` modules implemented `IBlockTransformer`, reflectively detecting a mod's tile
entity base class to fix state during a jump or rotation.

The instinct that this is mostly a pre-1.13 problem is **right for the larger half and wrong for the
smaller one**:

- **Gone.** Rotating packed metadata by hand was the bulk of the work. 1.13+ replaced it with
  `BlockState.rotate(Rotation)` / `mirror(Mirror)`, which the jump code can call generically on any
  block from any mod. That is a one-time generic implementation instead of 52 modules.
- **Still real.** Nothing in the platform standardises *external* state — tile entity NBT holding
  absolute `BlockPos` values (multiblock master links, conduit/network membership, bound remote
  positions), or registration in a mod's own network manager that must be torn down before the move
  and rebuilt after. That is exactly what `saveExternals` / `removeExternals` / `restoreExternals`
  existed for, and it has no generic replacement.
- **Watch out.** `BlockState.rotate()` defaults to returning the state unchanged. A mod that never
  overrode it produces a silently wrong-facing block after a jump, not a crash. Absence of errors is
  not evidence the generic path worked.

Plan: implement the generic `rotate`/`mirror` path, and only write a per-mod transformer when a
specific mod is observed breaking — driven by testing, not by porting the old list.

## 3. Dictionary tags — **DECIDED: native datapack tags**

`Dictionary.java` tagged other mods' content with WarpDrive behaviours. No code, just config strings.
This was the *entirety* of three "supported" mods:

| Mod | What it was |
|---|---|
| Advanced Solar Panels | 3 lines — solar helmets tagged `BreathingHelmet`, so they work as space helmets |
| GraviSuite | 3 lines — advanced jetpack / advanced nano chestplate / gravi chestplate tagged `FlyInSpace NoFallDamage` |
| MFFS | 1 line — its force field block tagged `PlaceEarlier StopMining NoBlink` |

All three are IC2-family or dead on 1.16.5, so their stale registry ids are deliberately absent.
IC2 itself has no 1.16.5 build. Compatibility now uses appendable tags under
`data/warpdrive/tags/`: a mod or modpack can add a current registry id without WarpDrive taking a
compile dependency or requiring a restart-only configuration parser.

`WarpDriveTags` is the shared code contract. Its consumers cover:

- breathing helmets, `FlyInSpace`, `NoFallDamage`, and entities which live without air;
- mining include/skip/stop, transporter `NoBlink`, force-field `NoCamouflage`, laser non-living
  targets, and cloak `NoReveal`;
- ship anchor and left-behind blocks/entities, no-mass and expandable blocks, plus all five legacy
  placement priorities. Placement runs earliest-to-latest and source removal in reverse.

The old ore-dictionary farming classifications were not duplicated: Minecraft log/leaf tags and
Forge ore tags are their native 1.16 replacements. `ExcludedAvatar` has no consumer because the
offline-avatar entity itself is not part of the 1.16 registry. Default files contain only valid
vanilla/WarpDrive 1.16 ids; optional mods extend them from their own datapacks.

## 3b. Curios — **NEW: optional accessory slots for air tanks**

Not a port. 1.12.2 had no Baubles support at all (`git grep -i baubles` on the baseline is empty),
so this is a new feature, added because a tank occupying a hotbar slot for the whole time you are in
space is the single most-felt friction in the breathing loop.

- **Bridge**: `compat/CuriosCompat.java`, reflection-only behind `ModList.isLoaded("curios")`, the
  same shape as `PatchouliCompat`. No Curios type is referenced anywhere else in the source, so the
  mod loads normally without it.
- **Slot**: the `back` preset, requested by IMC `register_type` during inter-mod enqueue. A tank is
  worn on the back rather than as a charm.
- **What may be equipped**: item tags, not a capability. `CuriosHelper.isStackValid` reduces to
  `tags.contains(slotId) || tags.contains("curio")`, where the tags are the item's tags in the
  `curios` namespace. So the tanks are listed in **two** files:
  `data/curios/tags/items/back.json` (the intended home) and `data/curios/tags/items/curio.json`
  (the wildcard, valid in any slot).

  The wildcard is deliberate rather than lazy. Curios 4.0.8.2 has no datapack slot loader — IMC is
  the only way a slot can exist — so if the `back` request does not land, a tank tagged only `back`
  is equippable **nowhere**, which is exactly how the first version failed. The wildcard means the
  tanks still work in whatever slots a pack happens to provide, and the log line above says whether
  the back request was made.
- **Dependency**: `runtimeOnly` from `maven.theillusivec4.top`, pinned by `Curios_version` in
  build.properties, plus an optional entry in mods.toml. Development only — it is not shipped.

The enabling change is in `BreathingManager`: six near-duplicate scans of `player.inventory.items`
now go through one `carriedSlots(player)` helper returning `ItemSlotRef` handles. References rather
than stacks, because the breathing code drains and replaces tanks, and a stack copied out of an item
handler is not the stack in the handler. Curios support is then one extra source feeding that list,
which is why consumption, the HUD gauge, refilling and ice electrolysis all picked it up at once.

Verified against the real jar rather than assumed: `ICuriosHelper.getEquippedCurios(LivingEntity)`
returns Forge's `LazyOptional`, not `java.util.Optional`. The first draft checked for the wrong type
and would have silently ignored every worn tank.

## 4. ICBM Classic — **TODO, open when a port exists**

Deepest integration of the lot, and none of it is IC2-dependent — ICBM Classic simply has no build
past 1.12.2 (1.7.10 and 1.12.2 only).

- `event/EMPReceiver.java` implements `icbm.classic.api.caps.IEMPReceiver`, attached as a capability
  to every WarpDrive tile entity, so EMP blasts drain and damage ship machines.
- `BlockForceField` carries a table of ICBM explosion classes → shield damage strength (nuclear 15.0,
  missile 15.0, grenade 3.0, fragments 0.02, …), so force fields resist ICBM weapons proportionally.
- Two recipe overrides (antimatter / red matter explosives from WarpDrive particle cells) — see
  RECIPE_PORT_NOTES.md.
- Entity tags for its Christmas mobs (`LivingWithoutAir`) and loot table entries.

Reopen only if ICBM Classic ships for a target version. The force field damage table is the piece
worth keeping even without ICBM — the same lookup handles vanilla and other mods' explosions.

## 5. Big Reactors / modded ores — **DECIDED: tag-driven ore deposits**

Big Reactors was never a code integration. Its only trace is asteroid/celestial ore generation in
`config/fillerSets-*.xml` (`bigreactors:brore` metadata 0/1/2, `bigreactors:blockmetals:1`), which is
Extreme Reactors' namespace.

Rather than re-listing per-mod block ids — which is what made those files unmaintainable, and which
1.13 flattening invalidated wholesale — deposits should resolve **ore tags**, so any mod that
registers into `forge:ores/<material>` participates for free with no WarpDrive-side entry.

**Implemented** in `world/LegacyAsteroidGenerator.java` via `OreTagSource`. The XML files stay dead
weight, kept only as the record of the original ratios.

Two kinds of deposit:

- **`oreTag(vanillaBlock, "material")`** for materials vanilla already has (coal, iron, redstone,
  gold, lapis, diamond, emerald, quartz). Candidates are the vanilla block *plus* any non-`minecraft`
  block in the tag. Modded variants join in; a vanilla-only world generates exactly what it did
  before. The namespace filter is deliberate — `forge:ores/gold` contains `minecraft:nether_gold_ore`
  in 1.16.5, and the 1.12.2 filler never put that in an overworld asteroid.
- **`optionalOreTag("material")`** for modded-only materials, carrying the ratio the old per-mod
  filler chains used: copper .048, tin .032, aluminum .032, lead .024 (common); silver / nickel /
  lead / zinc .015, osmium / certus_quartz .010, titanium .008, uranium .001 (uncommon). With no mod
  filling the tag the source returns null and the position falls through to base material, so ore
  density is unchanged rather than leaving a hole. Spelling variants are tried in order
  (`aluminum`, `aluminium`, `bauxite`; `uranium`, `yellorite`).

Details worth remembering:

- Which *material* a position gets still comes from the original `unitHash`; a second independent
  `variantHash` chooses among the blocks a material resolves to. Adding a mod therefore changes which
  block fills a deposit, never where the deposits are.
- Tag contents are cached per `ITagCollection` identity, so a datapack reload re-resolves and normal
  generation costs one reference comparison. The cache is published through a volatile snapshot
  because chunk generation is multi-threaded.
- Ore *order within a tag* depends on datapack load order, so the same seed with a different modlist
  can place a different mod's copper. Unavoidable for tag-driven selection, and harmless.

## 6. Patchouli manual — **DECIDED: optional, functional integration**

Patchouli 1.16.4-53.3 runs on Minecraft 1.16.5. It remains optional, matching 1.12.2: WarpDrive
registers its compatibility `book` id unconditionally for save stability, but the crafting recipe is
a `forge:conditional` recipe loaded only with Patchouli.

`ManualBookItem` opens `warpdrive:warpdrive_manual` through a reflection-only bridge to Patchouli's
server-side 1.16 API, so a missing optional mod cannot cause a WarpDrive class-loading failure. The
development runtime includes Patchouli to exercise its real loader; the dependency is not bundled in
the shipped WarpDrive jar and is declared optional in `mods.toml`.

Patchouli moved mod books from `assets/<modid>/patchouli_books` to
`data/<modid>/patchouli_books` after 1.12.2. The complete English/Chinese tree now uses that 1.16
layout, keeps WarpDrive's custom GUI textures and book model, and identifies `warpdrive:book` as its
custom item. Legacy metadata item strings and `@metadata` recipe ids were replaced with flattened
1.16 registry/recipe ids. Links to the unfinished legacy chapters are displayed as highlighted text
instead of opening missing pages; the existing space-suit entry receives the former armor links.

## 7. Computer integration — **DECIDED: CC:Tweaked capability adapters**

CC:Tweaked 1.101.3 is the supported 1.16.5 computer API. OpenComputers has no 1.16.5 release, so its
node/network/filesystem callbacks are target-unavailable rather than an unfinished optional module.

The old `TileEntityAbstractInterfaced` inheritance is represented by
`ComputerCraftCompat.CommonPeripheral`. CC:Tweaked discovers its annotated default methods, giving
every fixed WarpDrive peripheral the legacy `isInterfaced`, `getLocalPosition`, `getTier`,
`getUpgrades` and numeric `getVersion` calls. Version numbers come from Forge's expanded mod
metadata; the Java compatibility token `@version@` is no longer returned to Lua. Upgrade reports
describe the upgrade systems which are actually live in this port (camera recognition, mining
pumps, cloak crystals, transporter components, force-field projector/relay upgrades and the chunk
loader).

Air generators, capacitors, laser media and sirens now receive their formerly missing legacy-named
peripherals. The native ship controller also exposes `warpdriveShipController`, delegates the
bundled flight API to its currently adjacent core and mounts the original common/startup resources.
The generated ship-core peripheral is type-aliased to `warpdriveShipCore`, which is what that startup
program registers.

The 1.12 computer-interface item gate is not recreated globally. Most 1.16 machine ports already
made CC availability follow capability attachment when CC:Tweaked is installed, and several no
longer have general upgrade slots. Reintroducing the gate on only a subset would leave those
machines permanently inaccessible or create inconsistent gameplay. The dedicated chunk loader is
the intentional exception: its fully ported physical upgrade system still gates every operational
Lua call and reports a missing interface through `isInterfaced`.

## 8. Jump gates — **DECIDED: reserve compatibility names, do not invent a block**

The surviving `TileEntityJumpGateCore` is an abandoned prototype, not an omitted registered machine.
Repository history identifies its introducing refactor as `(wip)`. The associated
`BlockJumpGateCore` was never instantiated or registered and never received an item, blockstate,
model, loot table or recipe. That refactor simultaneously removed the older administrator-created
gate registry, `/jumpgates` listing and `/generate jumpgate` path, while leaving global-registry
validation commented out. The tile itself also contains unfinished calculations (incorrect volume
precedence and inverted/zero-unsafe occupancy), further ruling out a faithful shippable behavior to
port.

Accordingly, 1.16 does not assign a new registry id to it. `GlobalRegionType.JUMP_GATE` and the ship
controller's `targetName` call remain reserved so old API/persistent names are not reused for another
meaning. Confirming the dormant `GATE` command returns an explicit unsupported result rather than
claiming success and doing nothing. A future gate network would be new feature design: it needs an
explicit block/acquisition identity, ownership and naming rules, destination chunk/dimension policy,
collision behavior and migration semantics before implementation.
