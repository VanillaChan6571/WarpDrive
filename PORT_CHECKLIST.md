# Block & item port checklist (1.12.2 → 1.16.5)

Every registered block and item, with whether its **behaviour** exists in 1.16.5. Registration and
art are complete across the board — this tracks logic only.

Numbers in brackets after a 1.12.2 class are its line count, as a rough effort signal.
Verified against `v1.12.2-baseline` and the current tree on 2026-08-07.

**Legend**

| | meaning |
|---|---|
| ✅ | behaviour ported and working |
| 🟡 | partially ported — specific gap named |
| ⬜ | placeholder: registered + art only, 1.12.2 had real logic |
| ➖ | no logic to port — passive material, decoration, or crafting component |

Recipes exist for nearly all of these regardless of status; see
[RECIPE_PORT_NOTES.md](RECIPE_PORT_NOTES.md).

---

## Items

| Registry id | # | 1.12.2 | 1.16.5 | | Notes |
|---|---|---|---|---|---|
| `component-*` | 34 | `ItemComponent` | plain `Item` | ➖ | Crafting materials. `raw_rubber` is produced by tapping jungle logs with the laser tree farm |
| `air_tank.{basic,advanced,superior}` | 3 | `ItemAirTank` | `AirTankItem` [131] | ✅ | Damage-based storage, breath consumption, refill at an air generator, ice electrolysis, optional Curios `back` slot (new, not a 1.12.2 feature — COMPAT_NOTES.md §3b). The 1.12 creative `canister` metadata value was not registered as a separate 1.16 item |
| `warp_armor_*` | 12 | `ItemWarpArmor` | `WarpArmorItem` [48] + `WarpArmorMaterial` | ✅ | Tiers, breathing helmet, armour texture. (`ItemWarpArmor.onEntityExpireEvent` exists in 1.12.2 but its body is empty — nothing to port. The only meaningful implementor was `ItemElectromagneticCell`, which vents its particles on despawn) |
| `wrench` | 1 | `ItemWrench` | `WrenchItem` [77] | ✅ | Block rotation on use |
| `tuning_fork-*` | 16 | `ItemTuningFork` [178] | `TuningForkItem` [105] | ✅ | `useOn` ported with the original precedence (video, then control, beam frequency on sneak or when it is the only option), plus the `IVideoChannel` / `IControlChannel` / `IBeamFrequency` API. Lasers and force fields are live beam-frequency targets; the camera, monitor and laser camera are live video-channel targets; accelerator control points and injectors are live control-channel targets |
| `ship_token-*` | 30 | `ItemShipToken` [127] | `ShipTokenItem` + `ShipScannerTileEntity` | ✅ | Named tokens trigger the configured ship-scanner station after the original five-second single-player warm-up and are consumed only when protected deployment starts. Sneak-use a token on a scanner to bind its last completed schematic |
| `force_field_shape-*` | 7 | `ItemForceFieldShape` | plain `Item` | ➖ | Pure data carriers read and rendered by the projector |
| `force_field_upgrade-*` | 18 | `ItemForceFieldUpgrade` | plain `Item` | ➖ | Pure data carriers mounted on projectors and relays |
| `electromagnetic_cell.*` | 15 | `ItemElectromagneticCell` [354] | `ElectromagneticCellItem` | ✅ | Flattened empty/ion/proton/antimatter/strange-matter IDs carry real NBT amounts with 500/1000/2000 tier capacities, fill/drain operations, six fill-level models, localized payload tooltips, rarity/lifespan and radiation/explosion effects on despawn. Particle-aware recipes enforce the original 200-ion/24-proton costs and return the drained cell |
| `tuning_driver-*` | 3 | `ItemTuningDriver` [382] | `TuningDriverItem` + `TuningDriverRecipe` | ✅ | Three flattened modes retain shared channel NBT, cycle on air use, read/write compatible machines, randomize in creative, and restore the legacy redstone-plus-ordered-dyes hexadecimal configuration recipes |
| `plasma_torch.*` | 3 | `ItemPlasmaTorch` [304] | `PlasmaTorchItem` + `PlasmaTorchStorage` | ✅ | Stack-size-one multi-particle container with exact 200/400/800 tier capacities, legacy nested particle NBT, simulated fill/drain, recipe-marked partial consumption, fill predicate, tier rarity and payload tooltips. The surviving models now use the actual plasma-torch texture. Its creation recipe remains absent because it was commented out in 1.12.2 |
| `ic2_reactor_laser_focus` | 1 | `ItemIC2reactorLaserFocus` | retained catalog item | ➖ | Its entire behavior is the absent IC2 `IReactorComponent` API. Registry identity, art and dormant conditional recipe remain for data compatibility; there is no 1.16.5 integration to implement — see [COMPAT_NOTES.md](COMPAT_NOTES.md) |
| `book` | 1 | Patchouli guide book | `ManualBookItem` + optional `PatchouliCompat` | ✅ | Stack-size-one compatibility item opens `warpdrive:warpdrive_manual` through Patchouli's 1.16 server API. The 65-file bilingual book tree now lives in the required data-pack path, uses current flattened item/recipe ids and has no broken resource references or internal links; crafting is gated on Patchouli as it was in 1.12.2 |

---

## Blocks — working

| Registry id | # | 1.12.2 | 1.16.5 | | Notes |
|---|---|---|---|---|---|
| `ship_core` | 1 | `TileEntityShipCore` [1385] | `ShipCoreTileEntity` [1878] + `WarpEngine` [581] | ✅ | Jump engine, warp isolation, persistent global-region identity/bounds and security-station crew membership. Radar consumes the isolation probability across dimensions in universal coordinates |
| `ship_core.{basic,advanced,superior}` | 3 | `TileEntityShipCore` [1385] | `ShipCoreTileEntity` + tiered `ShipCoreBlock` | ✅ | The live jump engine now backs all legacy IDs, with 500K/10M/100M FE buffers, 24/48/96-block axis limits and the original tier mass bands. The untiered compatibility core remains permissive |
| `ship_controller` | 1 | `TileEntityShipController` [304] + `AbstractShipController` [507] | `ShipControllerTileEntity` [402] + container/GUI | ✅ | |
| `ship_controller.{basic,advanced,superior}` | 3 | `TileEntityShipController` [304] | `ShipControllerTileEntity` + tiered `ShipControllerBlock` | ✅ | All legacy IDs use the native adjacent-core controller/container and retained tier visuals; adjacency accepts canonical or tiered cores |
| `air_generator.{basic,advanced,superior}` | 3 | `TileEntityAirGeneratorTiered` [127] | `AirGeneratorTileEntity` [243] | ✅ | Air seeding, FE buffer, tank refill on right-click |
| `air_flow`, `air_source` | 2 | `BlockAirFlow`, `BlockAirSource` | `AirFlowBlock`, `AirSourceBlock` | ✅ | Driven by `AirSpreader` / `StateAir` / `ChunkHandler` |
| `air_shield` | 1 | `BlockAirShield` + `BlockColorAirShield` | `AirShieldBlock` [52] | ✅ | Seals air, walk-through, non-suffocating. 16 colours are now cosmetic NBT rather than 16 blocks |
| `creative_energy` | 1 | (`capacitor.creative`) | `CreativeEnergyTileEntity` [284] | ✅ | |
| `capacitor.{basic,advanced,superior,creative}` | 4 | `TileEntityCapacitor` [199] + `BlockCapacitor` [224] | `CapacitorTileEntity` + `CapacitorBlock` | ✅ | Tiered buffer, per-face routing cycled with a wrench, 95% transfer efficiency, creative tier bottomless. Upgrade slots (superconductors raising efficiency to 98%/100%) and the per-face colour rendering are not ported |
| `laser_medium.{basic,advanced,superior}` | 3 | `TileEntityLaserMedium` [76] | `LaserMediumTileEntity` + `LaserMediumBlock` | ✅ | Tiered input-only FE buffers, 4096 FE/t receive cap, legacy eight-stage animated charge display, and same-tier line consumption by laser machines |
| `enan_reactor_core.*` | 3 | `TileEntityEnanReactorCore` [910] + `Controller` [199] | `EnanReactorCoreTileEntity` + tier/face tables | ✅ | Exact 4/8/16-laser tier geometry and air cavity, 100M/500M/2B FE capacities, five-tick generation/decay and instability equations, computer-boot safety hold, automatic stabilization, four output policies with per-face routing/rate accounting, anti-spam and tier-scaled destructive failure. Signature/name persistence, 16 telemetry models, retained-identity drops and the bundled event-capable CC:Tweaked controller are live; stored charge and operating settings reset on pickup as in 1.12.2 |
| `enan_reactor_laser` | 1 | `TileEntityEnanReactorLaser` [326] | `EnanReactorLaserTileEntity` + `EnanReactorLaserBlock` | ✅ | Core-assigned face/orientation, one laser medium of any tier above or below, deferred exact-energy stabilization shots, original efficiency/randomness and over-firing penalty, blue beam feedback, assembly/status persistence and the legacy CC:Tweaked `side`/`stabilize`/energy API |
| `ic2_reactor_laser_cooler` | 1 | `TileEntityIC2reactorLaserMonitor` [193] | retained catalog block | ➖ | The tile only cooled IC2 reactor-focus items through IC2 reactor/chamber APIs, none of which exists on 1.16.5. Registry identity, art and dormant conditional recipe remain intentionally inert for data compatibility |
| `laser` | 1 | `TileEntityLaser` [859] + `AbstractLaser` [267] | `LaserTileEntity` + `AbstractLaserTileEntity` | ✅ | Computer-fired cannon with medium-line draining, beam frequency/scanner mode, booster beams, air/void attenuation, tagged nonliving targets, entity and block effects, protection event checks, sounds/beam particles, scan results and CC:Tweaked events/API. Matching-frequency beams pass through force fields; mismatched beams are absorbed and drain the projector |
| `laser_camera` | 1 | `TileEntityLaserCamera` [147] | `LaserCameraTileEntity` + `LaserCameraBlock` | ✅ | Inherits the complete working laser cannon, adds persisted/synchronised `IVideoChannel`, tuning-driver/fork support, the legacy CC:Tweaked API and monitor viewing. Spacebar firing from the remote view is server-authoritative and validates monitor proximity plus matching loaded endpoints |
| `weapon_controller` | 1 | `TileEntityWeaponController` [33] | `WeaponControllerTileEntity` + `WeaponControllerBlock` | ✅ | Faithful thin script host: exposes `warpdriveWeaponController`, mounts the bundled common/controller/startup Lua resources, and cleanly unmounts them on detach. Camera and laser peripherals supply the startup program's local-position dependency |
| `mining_laser` | 1 | `TileEntityMiningLaser` [639] + `AbstractMiner` [201] | `MiningLaserTileEntity` + `AbstractMinerTileEntity` | ✅ | Downward layer quarry with the original warm-up/scan/mine delays, atmosphere/void FE costs, medium-scaled radius, all-block or `forge:ores` modes, silk touch, 20 physical pump upgrades for fluids, protection checks, adjacent-inventory output/overflow shutdown, beam feedback and CC:Tweaked API |
| `laser_tree_farm` | 1 | `TileEntityLaserTreeFarm` [1150] + `AbstractMiner` [201] | `LaserTreeFarmTileEntity` + `AbstractMinerTileEntity` | ✅ | Medium-scaled scan radius/reach, planting from adjacent item handlers, log/leaf/mature-crop and stacking-plant harvesting, silk touch, overflow shutdown, protection checks, state models, CC:Tweaked API, and jungle-log tapping as the source of `component-raw_rubber`. IC2 wet-spot resin tapping stays deferred with IC2 itself |
| `ship_scanner.*` | 3 | `TileEntityShipScanner` [917] | `ShipScannerTileEntity` + `ShipSchematic` | ✅ | Three legacy size/mass tiers; finds the configured working ship core above; saves world-local, path-safe native schematics with full block-state/tile NBT; imports the old string-palette format; preflights loaded chunks, obstruction and Forge break/place protection; deploys incrementally with rotation, identity scrubbing/full core-capacitor charge for token-instantiated ships, scan/build effects and the legacy CC:Tweaked API. `configureTarget` is added for creating token stations without hand-editing NBT |
| `transporter_containment`, `transporter_scanner` | 2 | `BlockTransporterContainment` + `BlockTransporterScanner` | `TransporterContainmentBlock` + `TransporterScannerBlock` | ✅ | Half-height pad geometry, core-driven active scanner texture/light and the exact alternating 3×3 containment plus two-block-headroom validation used by the live transporter core |
| `transporter_core` | 1 | `TileEntityTransporterCore` [1835] | `TransporterCoreTileEntity` + `TransporterCoreBlock` | ✅ | Persistent named signatures/global lookup, eight-block scanner-room discovery, tunable beam frequency, exact range/locking/energizing equations, three physical component-upgrade types, cross-layer canonical coordinates, scoped Forge chunk tickets, matching-frequency force-field passage, bidirectional entity slot capture, movement failure/damage, 60-tick charging/200-tick cooldown, state models, NBT-retaining drops and the bundled event-capable CC:Tweaked UI/API |
| `transporter_beacon` | 1 | `TileEntityTransporterBeacon` [268] + `ItemBlockTransporterBeacon` | `TransporterBeaconTileEntity` + `TransporterBeaconBlockItem` | ✅ | Signature copy/apply interaction, portable NBT-backed FE capability and selected-slot drain, placed bottom-input 60K FE buffer, 20-tick deployment, 10 FE/t core handshake, automatic lock/energize and shutoff, four block models, charged item predicate, retained charge/signature drops and CC:Tweaked control |
| `chunk_loader.*` | 3 | `TileEntityChunkLoader` [238] + `TileEntityAbstractChunkLoading` [184] | `ChunkLoaderTileEntity` + owner-scoped Forge tickets | ✅ | All-face 1M FE input buffer; enabled/powered ticket lifecycle; persistent 1–25-chunk rectangles with the legacy area-not-axis limit; redstone-torch control; range, efficiency and computer-interface upgrades; retained energy/name/bounds/upgrades on pickup; active models and the gated legacy CC:Tweaked API. Tiers remain identity/art variants, matching 1.12.2 |
| `projector.*` | 3 | `TileEntityForceFieldProjector` [1332] | `ForceFieldProjectorTileEntity` + `ForceFieldProjectorBlock` | ✅ | Seven async-calculated shapes, half/full projectors, FE costs and cooldown, relay-network upgrades, incremental projection, frequency tuning, entity effects, protection checks, legacy multipart models and CC:Tweaked API. Breaking, pumping into adjacent Forge fluid handlers, item-port collection, stabilization and camouflage are live; inverted projectors operate on their interior without filling it with field blocks. Block drops retain their variant and machine configuration |
| `force_field_relay.*` | 3 | `TileEntityForceFieldRelay` [85] | `ForceFieldRelayTileEntity` + `ForceFieldRelayBlock` | ✅ | Passive 20-block frequency network, upgrade mounting/swapping, tier scaling and legacy multipart rendering. Item and fluid ports discover adjacent capability handlers, while camouflage relays sample the block above them |
| `force_field.*` | 3 | `TileEntityForceField` [187] + `BlockForceField` [644] | `ForceFieldTileEntity` + `ForceFieldBlock` | ✅ | Owned projected segments self-clean, collide, reject entity destruction, hide same-frequency interior faces, retain 16 frequency colours, handle entity effects and integrate with lasers. Explosion hits drain projector energy and downgrade breached segments one tier; camouflage state persists, synchronises and renders through a client tile renderer |
| `lift` | 1 | `TileEntityLift` [347] | `LiftTileEntity` + `LiftBlock` | ✅ | Redstone/computer up-down modes, 900 FE buffer, 150 FE per living entity, shaft/floor scan, cooldown, legacy beam colours and CC:Tweaked API. The optional crew-only security upgrade awaits machine upgrade slots and the global ship-region registry |
| `siren_{industrial,military}.{basic,advanced,superior}` | 6 | `TileEntitySiren` [173] + `BlockSiren` [195] | `SirenTileEntity` + `SirenBlock` | ✅ | Redstone activation, 12-way placement, original collision shapes/models, industrial/raid loops, and tiered 32/64/128-block linear volume falloff. Client sound lifecycle recovers stopped loops and cleans up on removal/unload |
| `speaker.{basic,advanced,superior}` | 3 | `TileEntitySpeaker` [114] + `BlockSpeaker` | `SpeakerTileEntity` + `SpeakerBlock` | ✅ | CC:Tweaked `speak` queue, original 12-message cap and 3-per-60-tick decay, JSON/translation/plain message handling, enable control, six-way model orientation, and tiered 16/32/64-block broadcast boxes |
| `camera` | 1 | `TileEntityCamera` [565] | `CameraTileEntity` + `CameraBlock` | ✅ | Directional loaded-endpoint video registry; 0–8 physical diamond-crystal recognition upgrades at 8 blocks each; line-of-sight entity results persisted with the legacy CC:Tweaked API/event. Crew flags resolve through security stations in overlapping global ship regions |
| `monitor` | 1 | `TileEntityMonitor` [116] | `MonitorTileEntity` + `MonitorBlock` | ✅ | Directional, tunable receiver with loaded-chunk camera lookup, remote first-person view, four zoom levels, legacy overlays, safe exit/context validation and CC:Tweaked API. Camera views never force-load chunks |
| `environmental_sensor` | 1 | `TileEntityEnvironmentalSensor` [239] | `EnvironmentalSensorTileEntity` + `EnvironmentalSensorBlock` | ✅ | Six-way plate with atmosphere concentration, biome/types, humidity, temperature, weather and world-time queries, enabled/active state and the legacy CC:Tweaked API |
| `biometric_scanner` | 1 | `TileEntityBiometricScanner` [241] | `BiometricScannerTileEntity` + `BiometricScannerBlock` | ✅ | Five-second outward scan, exclusive-target jamming and movement/absence aborts, persisted successful identity, legacy particles/sound and CC:Tweaked completion/abort events |
| `security_station` | 1 | `TileEntitySecurityStation` [267] | `SecurityStationTileEntity` + `SecurityStationBlock` | ✅ | Empty-hand player registration/removal with the legacy one-point cost, persisted UUID/name membership, online lookup, deterministic listing/removal and CC:Tweaked API |
| `cloaking_coil` | 1 | `BlockCloakingCoil` [105], no TE | `CloakingCoilBlock` | ✅ | Core-driven connected/active, inner/outer and facing state plus the channeling/projecting animated models used by the live cloaking core |
| `cloaking_core` | 1 | `TileEntityCloakingCore` [656] | `CloakingCoreTileEntity` + `CloakManager` | ✅ | Twelve-coil assembly, progressive loaded-chunk volume scans, 500M FE buffer and legacy tier upkeep/refresh rates. Six diamond crystals unlock transparent tier 2; server-authoritative areas cloak blocks/entities for outside players, reveal on entry/collapse, refresh chunk watches, animate coils/beams, play original sounds and expose the bundled CC:Tweaked API |
| `virtual_assistant.*` | 3 | `TileEntityVirtualAssistant` [271] | `VirtualAssistantTileEntity` + `VirtualAssistantBlock` | ✅ | Tiered 32/64/128-block addressed-chat capture, persisted name/last command, live 10/40/160 FE-per-tick listening cost, active models, renamed-item setup and legacy CC:Tweaked command event/pull/common APIs. The optional crew-security upgrade still awaits machine upgrade slots |
| `radar` | 1 | `TileEntityRadar` [340] | `RadarTileEntity` + `GlobalRegionRegistry` | ✅ | Full FE buffer, cubic scan cost, radius delay, persisted scan state, active/scanning models, bundled CC programs and legacy result API. Persistent ship snapshots supply isolated cross-dimension echoes in universal coordinates without force-loading chunks |
| `lamp_{bubble,flat,long}` | 3 | `BlockAbstractLamp` [195] | `LampBlock` [165] | ✅ | |
| `gas-*` | 12 | `BlockGas` [187] | `GasBlock` [56] | ✅ | Non-collidable, non-suffocating, worldgen only |
| `hull.*.omnipanel-*` | 48 | `BlockAbstractOmnipanel` [463] | `AbstractOmnipanelBlock` [221] + `HullOmnipanelBlock` | ✅ | |
| `hull.*.slab_*` / `stairs_*` | 96 | `BlockHullSlab` [422] | vanilla `SlabBlock` / `StairsBlock` | ✅ | Most of the 1.12.2 class was double-slab metadata handling, now native |
| `hull.*.plain/tiled/glass-*` | 144 | `BlockHull*` | `SimpleBlock` / `SimpleGlassBlock` | ➖ | Structural blocks; ship movement reads them, they have no behaviour of their own |
| `decorative*` | 7 | `BlockDecorative` | `SimpleBlock` | ➖ | |
| `bedrock_glass` | 1 | `BlockBedrockGlass` | `SimpleGlassBlock` | ➖ | Unbreakable flag only |
| `iridium_block`, `highly_advanced_machine` | 2 | passive | `SimpleBlock` | ➖ | Crafting materials |
| `void_shell.{plain,glass}` | 2 | `BlockVoidShell` | `SimpleBlock` / `SimpleGlassBlock` | ➖ | Passive accelerator structure; the accelerator core reads them |
| `warp_isolation` | 1 | passive | `SimpleBlock` read by `ShipCoreTileEntity` | ➖ | Its behaviour is the ship core's isolation scan; the block itself never had machine logic |
| `accelerator_core` | 1 | `TileEntityAcceleratorCore` [1205] | `AcceleratorCoreTileEntity` + `AcceleratorAssembly` | ✅ | Loaded-chunk void-shell topology/magnet-pair validation with the 192-block range, tiered chiller requirements, 100M FE buffer, legacy thermal/cooling/sustain/acceleration equations, injector cadence and block-input consumption, persisted bunch energy, thresholded single/head-on collider output into real particle cells, overflow explosions, driven component states and the bundled CC:Tweaked control API |
| `accelerator_control_point` | 1 | `TileEntityAcceleratorControlPoint` [168] | `AcceleratorControlPointTileEntity` | ✅ | Persisted enable/control channel, core-driven active state, `IControlChannel`, tuning tools and legacy `state`/`enable`/`controlChannel` CC:Tweaked API |
| `particles_injector` | 1 | `TileEntityParticlesInjector` [10] | `ParticlesInjectorTileEntity` | ✅ | Uses the control-point implementation, consumes adjacent block items for bunch injection and retains the distinct `warpdriveParticlesInjector` peripheral type expected by accelerator setups |
| `particles_collider` | 1 | `BlockParticlesCollider` [42], no TE | `AcceleratorComponentBlock` | ✅ | Core-driven active/offline state and textures |
| `chiller.*` | 3 | `BlockChiller` [202], no TE | `ChillerBlock` | ✅ | Tiered core-driven active state, inset collision, absolute heat contact damage/fire, ambient chiller sound and snow particles |
| `electromagnet.*` | 6 | passive | `SimpleBlock` / `SimpleGlassBlock` | ➖ | Tiered hardness/resistance and models are complete; the live accelerator core reads these structural field blocks |

---

## Non-registry legacy prototypes

Source classes in this section never acquired a registered block/item identity and require an audit
before anyone treats them as missing gameplay content.

### Ship movement
| Registry id | # | 1.12.2 | | Notes |
|---|---|---|---|---|
| — | | `TileEntityJumpGateCore` [215] | ➖ | Abandoned 1.12 WIP, not a missing registered machine. Its introducing commit says `(wip)`; `BlockJumpGateCore` was never instantiated/registered and has no item, model or recipe, while the same refactor removed the working admin-generated gate registry/commands and left gate validation commented out. Registering a new 1.16 block would invent a feature and save id. `JUMP_GATE`/`targetName` remain reserved for persistence/API compatibility, and confirmed `GATE` commands now fail explicitly instead of silently doing nothing |

---

## Cross-cutting systems with no single block

| System | 1.12.2 | | Notes |
|---|---|---|---|
| Computer interfacing | `TileEntityAbstractInterfaced` [750] | ✅ | `CommonPeripheral` restores the inherited `isInterfaced`, local-position, tier, upgrade and numeric-version contract across every fixed CC:Tweaked adapter. Air generators, capacitors, laser media, sirens and the native adjacent-core ship controller now have the missing legacy peripheral types; the controller mounts its bundled startup/common files and the generated ship-core peripheral is aliased to `warpdriveShipCore`. Current physical upgrade status is reported where a 1.16 machine still has upgrades. OpenComputers has no 1.16.5 build; installable interface gates are superseded by capability attachment except for the deliberately gated chunk loader |
| Energy base classes | `TileEntityAbstractEnergy` [917] | ✅ | `AbstractEnergyTileEntity` provides the FE buffer, per-face capability routing and NBT. Air generator, capacitor, accelerator, transporter core/beacon, ENAN reactor core and chunk loader use it; ship core and creative energy still hand-roll theirs and can migrate when next touched |
| Chunk loading | `TileEntityAbstractChunkLoading` [184] | ✅ | Forge 1.16 owner-scoped tickets replace the old ticket object/callback wrapper. Transporters use transient operational UUID tickets; dedicated loaders use persistent block-position tickets, validate the owner-chunk bootstrap invariant, rebuild saved rectangles and release on disable, lost power, range change or break |
| Tuning API | `IVideoChannel` / `IControlChannel` / `IBeamFrequency` | ✅ | Ported verbatim, including the beam-frequency colour curve (now returning `Vector3f`). Laser cannons, force-field projectors/relays and transporter cores implement `IBeamFrequency`; cameras, monitors and laser cameras implement `IVideoChannel` |
| Cloak registry | `CloakManager` | ✅ | Transient server-authoritative areas, per-player inside/outside visibility, chunk-watch refresh, block/entity masking and reveal resynchronization. Client masking is a client-only chunk-state mixin, leaving received chunk data intact |
| Force field registry | `ForceFieldRegistry` | ✅ | Loaded server-side projectors and relays form frequency networks through a 20-block relay breadth-first search; stale entries are pruned and tiles re-register after chunk load |
| Block transformers for ship movement | 52 `compat/*` modules | ➖ | Superseded by generic `BlockState.rotate` — see COMPAT_NOTES.md §2 |
| `Dictionary` behaviour tags | `config/Dictionary.java` | ✅ | Central `WarpDriveTags` definitions and appendable item/block/entity-type datapack tags now drive breathing helmets, airless mobs, space movement, fall immunity, mining/transport obstructions, force-field camouflage, cloak reveal safety and ship anchors/mass/left-behind/expandable/placement order. Vanilla log/leaf/ore farming classifications use the standard Minecraft/Forge tags. Dead 1.12 mod ids (including IC2) are not shipped |
| Item gravity in space | `GravityManager` + CoreMod | ✅ | Entities via Forge's `ENTITY_GRAVITY` attribute; **dropped items via `ItemEntityMixin`**, which substitutes the -0.04 gravity and 0.98 drag constants inside `ItemEntity.tick` exactly where the 1.12.2 CoreMod rewrote them. The class comment in `GravityHandler` claiming otherwise was stale and has been corrected |
| Celestial orbit transitions | `AltitudeTransitionHandler` + `CelestialCoordinates` | ✅ | The native one-world-per-layer design deliberately retires the packed-world XML map. Overworld/space share 1:1 XZ, hyperspace uses 8:1 XZ, and the three layers occupy contiguous universal Y bands used consistently by entity transitions, ship jumps and radar |

---

## Summary

Counted by table row, and by the registry ids those rows cover:

| Status | Rows | Registry ids |
|---|---|---|
| ✅ working | 64 | 321 |
| 🟡 partial | 0 | 0 |
| ⬜ placeholder | 0 | 0 |
| ➖ nothing to port | 14 | 224 |

By registry id that is 545 of 545 needing no further work. No registered block or item remains a
placeholder. The final non-registry row was the abandoned jump-gate-core WIP, not shippable 1.12
content, so no placeholder remains.

There are no partial rows left. The three force-field rows cover their 1.12.2 machine behaviour; the old temperature
terraforming branch remains intentionally absent because its legacy method was an empty TODO.

## Done so far

- Tuning fork `useOn` and the `IVideoChannel` / `IControlChannel` / `IBeamFrequency` API
- Warp isolation scanning in the ship core
- Air tank refill at an air generator, and ice electrolysis in the breathing manager
- Tag-driven ore deposits in asteroid generation (`forge:ores/*`)
- `AbstractEnergyTileEntity`, with the air generator migrated onto it
- Subspace capacitors: tiered buffer, per-face routing, transfer losses, creative tier
- Laser media: tiered FE storage, animated charge level, shared same-tier assembly/drain logic
- Laser cannon: scanner and destructive beams, boosters, tuning fork support and CC:Tweaked API
- Laser lift: redstone/computer direction control, FE use, entity transport and beam feedback
- Six tiered industrial/military sirens with legacy placement, shapes, range and looping audio
- Three tiered speakers with the legacy message queue, rate limiter, range and CC:Tweaked API
- Tuning driver: all three modes, machine read/write, creative randomization, mode cycling, shared
  NBT and the original ordered-dye hexadecimal configuration recipes
- Force fields: tiered half/full projectors, relay networks and upgrades, all seven shapes, FE use,
  projected collision/effects, laser/explosion interaction, tuning-fork and CC:Tweaked control,
  breaking/pumping/item-port/stabilization operations, camouflage and legacy models
- Video system: directional cameras and monitors, loaded-endpoint registry, zoom/overlays, validated
  remote laser-camera firing, optical recognition upgrades and the legacy CC:Tweaked APIs/events
- Mining laser: layer quarry, ore-only/silk/fluid-pump modes, protected harvesting and inventory output
- Laser tree farm: planting and harvesting loop, crop/tree handling and jungle-log raw-rubber tapping
- Ship scanners and tokens: tiered native/legacy schematic capture, protected rotated deployment,
  token station warm-up/consumption/binding, identity-safe instantiation and CC:Tweaked control
- Weapon controller: faithful bundled common/controller/startup Lua mounting and peripheral lifecycle
- Detection leaf machines: environmental sensor, biometric scanner and UUID-backed security station
- Accelerator leaf components: tunable control points/injectors, active colliders and hazardous tiered
  chillers; electromagnets confirmed as already-complete passive structure
- Transporter pad components: half-height geometry, active scanner lighting and exact 3×3 validation
- Cloaking coil: connected/active inner/outer state and channeling/projecting animation contract
- Virtual assistants: tiered energy-backed addressed-chat capture and legacy command event/pull API
- Radar: delayed energy-backed universal ship scans, warp-isolation filtering, models and bundled CC tools
- Tiered ship cores/controllers: all six legacy IDs use the live graph with original core limits
- Native celestial coordinates: 1:1 overworld/space and 8:1 hyperspace mapping shared by altitude
  transitions, ship jumps and cross-dimension radar
- Persistent global ship regions: stable ship identity/bounds, radar echoes and security-station crew
  membership without force-loading remote chunks
- Cloaking core and registry: complete coil assembly, progressive volume/upkeep loop, two visual tiers,
  per-player block/entity masking, reveal synchronization, sounds/beams and bundled CC controls
- Accelerator graph and particle cells: loaded void-shell/magnet/chiller topology, thermal and bunch
  simulation, injector/collider operation, typed tier-capacity cells, hazards and amount-aware recipes
- Transporter room and beacon: persistent signatures, live pad graph, focus/range/energy equations,
  shield-aware cross-dimension bidirectional transfer, failure damage, portable/placed beacons,
  scoped chunk tickets, retained configuration and the original bundled CC:Tweaked controls/events
- ENAN reactor graph: tier-exact core cavities and stabilizer positions, original generation,
  instability, laser-control, throttled-output and explosion equations, live FE extraction and the
  bundled automatic CC:Tweaked stabilization interface
- Dedicated chunk loaders: tiered live blocks, persistent owner-scoped ticket rectangles, exact
  area/energy/upgrade rules, pickup NBT, active models and the computer-interface-gated CC API
- Plasma torches: three real tiered multi-particle containers with exact capacity, NBT,
  fill/drain, recipe-remainder and tooltip behavior; their intentionally disabled recipe stays off
- IC2 reactor focus/cooler closure: retained registry/art/dormant recipes, explicitly no target-side
  behavior because IC2 and its reactor APIs do not exist for 1.16.5
- Patchouli manual: optional reflection bridge and mod-gated recipe, corrected 1.16 data-pack layout,
  bilingual content with flattened item/recipe references, resolved images and no dead page links
- Dictionary behaviours: shared reloadable tags replace the old config sets across breathing,
  gravity/fall protection, mining, transport, force-field camouflage, cloaking and ship movement;
  ship removal/placement order, no-mass/left-behind blocks and expandable collisions are restored
- Common computer interfacing: all live machine adapters inherit the five legacy identification/
  position/tier/upgrade/version methods; missing air-generator, capacitor, laser-medium, siren and
  ship-controller peripherals are attached, legacy ship type names are preserved, the controller
  startup mounts again, and version calls read expanded Forge metadata instead of `@version@`
- Jump-gate audit: the unregistered gate core is confirmed as an abandoned WIP with no block/item
  identity or resources and is not invented as new 1.16 content; its enum/API identities stay
  reserved and the dormant `GATE` ship command now returns an explicit unsupported result

## Suggested order from here

The placeholder sweep is complete. Future jump-gate work would be a separately designed new feature,
including registry ids, acquisition, destination ownership and cross-dimension policy, rather than a
remaining 1.12 port task.

Two things every newly-real machine needs, which the placeholders did not: a **loot table** under
`data/warpdrive/loot_tables/blocks/` (placeholder blocks drop nothing when broken), and migration
onto `AbstractEnergyTileEntity` if it stores power.

## Loot tables

1.12.2 dropped a block by default; 1.16.5 drops nothing unless a loot table exists, so every
obtainable block needs one. 383 tables now cover every block that has an item, except four
deliberate cases:

- `force_field.*` — projected by the projector, never dropped
- `gas-*` — worldgen scenery
- `bedrock_glass` — unbreakable, so nothing can mine it
- `air_flow` / `air_source` — no item at all

Hull slabs use the vanilla slab table shape (a `block_state_property` check on `type=double` doubling
the count, with `explosion_decay` instead of `survives_explosion`), so a double slab yields two.
Everything else is a plain self-drop.

This was a real gap rather than a theoretical one: all 288 hull blocks had no table, so the mod's
primary building material could not be recovered by hand *or* by the mining laser, which harvests
through `Block.getDrops`.

Loose end: twelve orphan blockstate files exist with no registered block —
`hull.<tier>.{plain,tiled,glass,omnipanel}.json` without a colour suffix. Harmless, but they make
asset audits noisy.
