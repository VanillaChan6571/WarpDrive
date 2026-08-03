# WarpDrive Reimagined - Implementation Plan for 1.16.5

**Strategy**: Clean slate reimplementation focusing on core features
**Timeline**: 4-6 weeks
**Approach**: Modern 1.16.5 patterns, borrowing concepts from legacy code

## Core Concept

WarpDrive allows players to:
1. Build a ship from blocks
2. Use ComputerCraft computers to control it
3. Warp/teleport the entire ship structure to new locations
4. Travel between dimensions (Overworld, Space, End, modded dimensions)
5. Mine asteroids in space for resources

## Essential Features (MVP)

### 1. Ship Core Block + CC:Tweaked Integration
**Purpose**: Main control block for the ship, interfaced via ComputerCraft

**Lua API Methods**:
```lua
-- Ship scanning
ship.scan() → returns {blocks: number, valid: boolean}

-- Destination setting
ship.setDestination(x, y, z, dimension) → boolean

-- Energy management
ship.getEnergyStored() → number
ship.getEnergyRequired() → number

-- Warping
ship.jump() → {success: boolean, message: string}

-- Ship info
ship.getShipSize() → {x: number, y: number, z: number}
ship.getCorePosition() → {x: number, y: number, z: number}
```

**Features**:
- Peripheral for CC:Tweaked
- Stores energy (Forge Energy)
- Scans connected ship blocks
- Initiates warp jumps

### 2. Ship Structure System
**Purpose**: Define and save ship structures

**Algorithm** (borrowed from legacy):
```java
1. Start from Ship Core block
2. Flood-fill to find all connected blocks (up to max size)
3. Save block states + TileEntity NBT data
4. Calculate energy cost (distance × blocks × dimension_multiplier)
5. On warp: Remove blocks from origin, place at destination
```

**Constraints**:
- Max ship size: 128×128×128 blocks (configurable)
- Must be contiguous (connected blocks)
- Ship core must be part of the ship

### 3. Space Dimension
**Purpose**: Custom dimension with asteroids and resources

**Features**:
- Void world generator (empty space)
- Asteroid generation:
  - Random sizes (10-30 blocks)
  - Spawn periodically (every 5 minutes in loaded chunks)
  - Contain ores (iron, gold, diamond, emerald)
- No oxygen (vacuum environment)
- Custom sky rendering (stars, planets)

**Implementation**:
```java
- DimensionType registration
- Custom ChunkGenerator with asteroid features
- Periodic asteroid spawning (using tick events)
```

### 4. Energy System
**Purpose**: Power the warp drive

**Blocks**:
- **Capacitor Block**: Stores Forge Energy
- **Ship Core**: Consumes energy for warps

**Energy Costs**:
```
Base cost = blocks × 100 FE
Distance multiplier = sqrt(distance) × 10 FE
Dimension change = +50,000 FE
Total = (base × distance_mult) + dimension_penalty
```

### 5. Dimension Integration
**Purpose**: Warp between any dimension

**Support**:
- ✅ Vanilla dimensions (Overworld, Nether, End)
- ✅ Auto-detect modded dimensions (Twilight Forest, Aether, etc.)
- ✅ Custom Space dimension

**Implementation**:
```java
// Get all registered dimensions
Registry<DimensionType> dimensionRegistry = ...
// Allow warping to any dimension in registry
```

## Implementation Phases

### Phase 1: Foundation (Week 1)
**Goal**: Basic mod structure and Ship Core block

**Tasks**:
1. ✅ Create mod structure (build.gradle, mods.toml) - Already done!
2. Create Ship Core block + TileEntity
3. Add CC:Tweaked dependency and peripheral integration
4. Implement basic Lua API (getEnergyStored, scan placeholder)
5. Test: Place Ship Core, access from computer

**Deliverable**: Ship Core block that responds to Lua commands

### Phase 2: Ship Scanning & Energy (Week 2)
**Goal**: Ship structure detection and energy system

**Tasks**:
1. Implement flood-fill ship scanner
2. Create Capacitor block + TileEntity (energy storage)
3. Implement energy cost calculation
4. Add ship size validation
5. Test: Scan ship, check energy requirements

**Deliverable**: Ship scanner that reports ship size and energy needs

### Phase 3: Warp Mechanics (Week 2-3)
**Goal**: Actually teleport ships

**Tasks**:
1. Implement block save/restore system
   - Save BlockStates + TileEntity NBT
   - Handle special blocks (chests, furnaces, etc.)
2. Implement warp execution
   - Remove blocks from origin
   - Place blocks at destination
   - Teleport players/entities on ship
3. Add safety checks (collision detection)
4. Test: Warp ship in Overworld

**Deliverable**: Working ship teleportation within same dimension

### Phase 4: Space Dimension (Week 3-4)  — NOT STARTED
**Goal**: Custom Space dimension

> **TODO: neither dimension exists yet.** `isInSpace()` / `isInHyperspace()` are already exposed on
> the Ship Core and compare against `warpdrive:space` / `warpdrive:hyperspace`, so they answer
> `false` today and become correct the moment the dimensions are registered — no Lua call site will
> need changing. Still outstanding for this phase:
> - register the Space and Hyperspace dimensions + dimension types
> - void chunk generator
> - asteroid feature + periodic placement
> - vacuum / no-oxygen handling
> - **cross-dimension jumps in `WarpEngine`** — it currently moves blocks within a single `World`
>   only, so a dimension change is not merely a coordinate change

**Tasks**:
1. Register Space dimension
2. Create void world generator
3. Implement asteroid generation
   - Random spawn locations
   - Size variation
   - Ore placement
4. Add periodic spawning system
5. Test: Warp to Space, find asteroids

**Deliverable**: Space dimension with asteroids

### Phase 5: Cross-Dimension Warping (Week 4-5)
**Goal**: Warp between dimensions

**Tasks**:
1. Implement dimension change logic
2. Add dimension parameter to warp API
3. Handle dimension-specific quirks
4. Test: Warp Overworld ↔ Space ↔ End
5. Test: Warp to modded dimensions (if installed)

**Deliverable**: Cross-dimension warping

### Phase 6: Polish & Features (Week 5-6)
**Goal**: User experience and extras

**Tasks**:
1. Add GUI for Ship Core (optional, nice-to-have)
2. Add coordinate bookmark system (via Lua)
3. Add ship mass calculation (for energy costs)
4. Add warp cooldown system
5. Add particle effects for warping
6. Documentation + example Lua programs
7. Testing and bug fixes

**Deliverable**: Polished, feature-complete mod

## Technical Architecture

### Mod Structure
```
src/main/java/cr0s/warpdrive/
├── WarpDrive.java           # Main mod class
├── block/
│   ├── ShipCoreBlock.java           # Ship core block
│   ├── ShipCoreTileEntity.java      # Ship core logic + CC integration
│   └── CapacitorBlock.java          # Energy storage
├── system/
│   ├── ShipScanner.java             # Ship structure detection
│   ├── ShipData.java                # Ship structure data class
│   ├── WarpEngine.java              # Warp execution logic
│   └── EnergyCalculator.java        # Energy cost formulas
├── dimension/
│   ├── SpaceDimension.java          # Space dimension registration
│   ├── SpaceChunkGenerator.java     # Void + asteroids
│   └── AsteroidFeature.java         # Asteroid generation
└── integration/
    └── ComputerCraftIntegration.java # CC:Tweaked peripheral

src/main/resources/
├── META-INF/mods.toml               # Mod metadata
├── data/warpdrive/
│   ├── dimension/space.json         # Space dimension definition
│   └── dimension_type/space.json    # Space dimension type
└── assets/warpdrive/
    ├── blockstates/                 # Block models
    ├── models/                      # 3D models
    └── lua/                         # Example Lua programs
```

### Key Algorithms to Borrow from Legacy Code

1. **Ship Scanning** (from `JumpShip.java`):
   - Flood-fill algorithm
   - Block validation
   - TileEntity data preservation

2. **Block Copy/Paste** (from `JumpBlock.java`):
   - BlockState serialization
   - NBT data handling
   - Entity teleportation

3. **Dimension Warping** (from `JumpSequencer.java`):
   - Multi-phase execution
   - Chunk loading
   - Collision detection

## Dependencies

```gradle
dependencies {
    minecraft "net.minecraftforge:forge:1.16.5-36.2.39"

    // CC:Tweaked (essential)
    compileOnly fg.deobf("org.squiddev:cc-tweaked-1.16.5:1.100.9")
    runtimeOnly fg.deobf("org.squiddev:cc-tweaked-1.16.5:1.100.9")
}
```

## Configuration

```toml
# config/warpdrive-common.toml
[ship]
    maxShipSize = 128           # Max ship dimension
    maxShipBlocks = 50000       # Max total blocks
    energyPerBlock = 100        # FE per block
    energyPerMeter = 10         # FE per meter traveled
    dimensionChangeCost = 50000 # Extra FE for dimension change

[space]
    asteroidSpawnChance = 0.05  # 5% chance per chunk per 5min
    asteroidMinSize = 10
    asteroidMaxSize = 30

[general]
    enableDebugLogging = false
```

## Testing Checklist

### Phase 1
- [ ] Ship Core block places and breaks correctly
- [ ] CC computer can detect Ship Core as peripheral
- [ ] Basic Lua commands respond (getEnergyStored, etc.)

### Phase 2
- [ ] Ship scanner finds all connected blocks
- [ ] Ship size validation works (max size enforcement)
- [ ] Energy cost calculation correct
- [ ] Capacitor stores and transfers energy

### Phase 3
- [ ] Ship warps within same dimension
- [ ] All blocks copied correctly (including TileEntities)
- [ ] Chests/furnaces preserve inventory
- [ ] Players on ship teleport with it
- [ ] Collision detection prevents overlapping ships

### Phase 4
- [ ] Space dimension exists and loads
- [ ] Asteroids generate with ores
- [ ] Asteroids spawn periodically
- [ ] Can mine asteroids for resources

### Phase 5
- [ ] Warp Overworld → Space
- [ ] Warp Space → End
- [ ] Warp to modded dimension (if installed)
- [ ] Energy costs correct for dimension changes

### Phase 6
- [ ] All features polished
- [ ] No crashes or major bugs
- [ ] Example Lua programs work
- [ ] Documentation complete

## Success Criteria

✅ Players can build a ship
✅ Players can control ship via ComputerCraft
✅ Ships can warp within and between dimensions
✅ Space dimension has mineable asteroids
✅ Energy system works and feels balanced
✅ Compatible with modded dimensions
✅ No critical bugs

## Future Enhancements (Post-MVP)

- Force fields
- Weapon systems
- Cloaking
- Radar/scanners
- Multiple ship cores per ship
- Ship-to-ship combat
- Programmable autopilot
- Warp gates (fixed teleporters)

---

**Next Step**: Start Phase 1 - Create Ship Core block with CC:Tweaked integration!
