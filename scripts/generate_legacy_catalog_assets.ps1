param()

$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $workspace 'src/main/resources/assets/warpdrive'
$blockstates = Join-Path $assets 'blockstates'
$blockModels = Join-Path $assets 'models/block'
$itemModels = Join-Path $assets 'models/item'
$catalogModels = Join-Path $blockModels 'catalog'
$hullModels = Join-Path $blockModels 'hull_generated'
$langPath = Join-Path $assets 'lang/en_us.json'
$legacyLangPath = Join-Path $assets 'lang/en_us.lang'

New-Item -ItemType Directory -Force -Path $catalogModels, $hullModels | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Write-Json([string] $Path, $Value) {
	$json = $Value | ConvertTo-Json -Depth 100
	[System.IO.File]::WriteAllText($Path, $json + [Environment]::NewLine, $utf8)
}

function Title-Case([string] $Value) {
	$words = $Value -split '[._-]+'
	return (($words | ForEach-Object {
		if ($_.Length -eq 0) { return $_ }
		if ($_ -eq 'ic2') { return 'IC2' }
		return $_.Substring(0, 1).ToUpperInvariant() + $_.Substring(1)
	}) -join ' ')
}

function Normalize-Model([string] $Model) {
	if ($Model -notlike 'warpdrive:*') { return $Model }
	if ($Model -like 'warpdrive:block/*') { return $Model }
	return $Model.Replace('warpdrive:', 'warpdrive:block/')
}

function Existing-Custom-Model([string] $Raw) {
	foreach ($match in [regex]::Matches($Raw, '"model"\s*:\s*"([^"]+)"')) {
		$model = $match.Groups[1].Value
		if ($model -notlike 'warpdrive:*') { continue }
		$normalized = Normalize-Model $model
		$relative = $normalized.Substring('warpdrive:block/'.Length).Replace('/', [IO.Path]::DirectorySeparatorChar)
		$path = Join-Path $blockModels ($relative + '.json')
		if (Test-Path $path) {
			$modelRaw = [System.IO.File]::ReadAllText($path)
			if ($modelRaw -notmatch '"loader"\s*:') { return $normalized }
		}
	}
	return $null
}

function Catalog-Cube-Model([string] $Raw) {
	$textures = [ordered]@{}
	foreach ($match in [regex]::Matches($Raw,
		'"(all|side|bottom|top|particle|front|back|down|up|north|south|west|east)"\s*:\s*"(warpdrive:[^"]+)"')) {
		$key = $match.Groups[1].Value
		if (-not $textures.Contains($key)) { $textures[$key] = $match.Groups[2].Value }
	}
	if ($textures.Contains('all')) {
		return [ordered]@{ parent = 'minecraft:block/cube_all'; textures = [ordered]@{ all = $textures.all } }
	}
	if ($textures.Contains('side') -and $textures.Contains('top') -and $textures.Contains('bottom')) {
		return [ordered]@{
			parent = 'minecraft:block/cube_bottom_top'
			textures = [ordered]@{ side = $textures.side; top = $textures.top; bottom = $textures.bottom }
		}
	}
	if ($textures.Contains('down') -and $textures.Contains('up') -and $textures.Contains('north') `
		-and $textures.Contains('south') -and $textures.Contains('west') -and $textures.Contains('east')) {
		return [ordered]@{
			parent = 'minecraft:block/cube'
			textures = [ordered]@{
				particle = if ($textures.Contains('particle')) { $textures.particle } else { $textures.north }
				down = $textures.down; up = $textures.up; north = $textures.north
				south = $textures.south; west = $textures.west; east = $textures.east
			}
		}
	}
	if ($textures.Count -gt 0) {
		$first = ($textures.GetEnumerator() | Select-Object -First 1).Value
		return [ordered]@{ parent = 'minecraft:block/cube_all'; textures = [ordered]@{ all = $first } }
	}
	return [ordered]@{
		parent = 'minecraft:block/cube_all'
		textures = [ordered]@{ all = 'warpdrive:blocks/passive/highly_advanced_machine-side' }
	}
}

$catalogBlocks = @(
	'accelerator_control_point', 'accelerator_core', 'biometric_scanner', 'camera',
	'capacitor.basic', 'capacitor.advanced', 'capacitor.superior', 'capacitor.creative',
	'chiller.basic', 'chiller.advanced', 'chiller.superior',
	'chunk_loader.basic', 'chunk_loader.advanced', 'chunk_loader.superior',
	'cloaking_coil', 'cloaking_core',
	'enan_reactor_core.basic', 'enan_reactor_core.advanced', 'enan_reactor_core.superior',
	'enan_reactor_laser', 'environmental_sensor',
	'force_field.basic', 'force_field.advanced', 'force_field.superior',
	'force_field_relay.basic', 'force_field_relay.advanced', 'force_field_relay.superior',
	'ic2_reactor_laser_cooler', 'laser', 'laser_camera',
	'laser_medium.basic', 'laser_medium.advanced', 'laser_medium.superior',
	'laser_tree_farm', 'lift', 'mining_laser', 'monitor', 'particles_collider', 'particles_injector',
	'projector.basic', 'projector.advanced', 'projector.superior', 'radar', 'security_station',
	'ship_controller.basic', 'ship_controller.advanced', 'ship_controller.superior',
	'ship_core.basic', 'ship_core.advanced', 'ship_core.superior',
	'ship_scanner.basic', 'ship_scanner.advanced', 'ship_scanner.superior',
	'siren_industrial.basic', 'siren_industrial.advanced', 'siren_industrial.superior',
	'siren_military.basic', 'siren_military.advanced', 'siren_military.superior',
	'speaker.basic', 'speaker.advanced', 'speaker.superior',
	'transporter_beacon', 'transporter_containment', 'transporter_core', 'transporter_scanner',
	'virtual_assistant.basic', 'virtual_assistant.advanced', 'virtual_assistant.superior',
	'weapon_controller'
)

foreach ($name in $catalogBlocks) {
	$statePath = Join-Path $blockstates ($name + '.json')
	$modelPath = Join-Path $catalogModels ($name + '.json')
	if (-not (Test-Path $statePath)) { throw "Missing legacy blockstate for $name" }
	if (-not (Test-Path $modelPath)) {
		$raw = [System.IO.File]::ReadAllText($statePath)
		$customModel = Existing-Custom-Model $raw
		$model = if ($null -ne $customModel) {
			[ordered]@{ parent = $customModel }
		} else {
			Catalog-Cube-Model $raw
		}
		Write-Json $modelPath $model
	}
	Write-Json $statePath ([ordered]@{
		variants = [ordered]@{ '' = [ordered]@{ model = "warpdrive:block/catalog/$name" } }
	})
	Write-Json (Join-Path $itemModels ($name + '.json')) ([ordered]@{
		parent = "warpdrive:block/catalog/$name"
	})
}

# Repair output from early versions of this generator where indexing OrderedDictionary.Keys in
# Windows PowerShell returned every value. Seven entries are the legacy cube order
# particle/down/up/north/south/west/east; a single entry is an ordinary cube texture.
foreach ($path in Get-ChildItem $catalogModels -Filter *.json) {
	$model = Get-Content $path.FullName -Raw | ConvertFrom-Json
	$all = $model.textures.all
	if ($all -isnot [System.Array]) { continue }
	if ($all.Count -ge 7) {
		Write-Json $path.FullName ([ordered]@{
			parent = 'minecraft:block/cube'
			textures = [ordered]@{
				particle = $all[0]; down = $all[1]; up = $all[2]; north = $all[3]
				south = $all[4]; west = $all[5]; east = $all[6]
			}
		})
	} elseif ($all.Count -gt 0) {
		Write-Json $path.FullName ([ordered]@{
			parent = 'minecraft:block/cube_all'; textures = [ordered]@{ all = $all[0] }
		})
	}
}

$colors = @('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
	'silver', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
$tiers = @('basic', 'advanced', 'superior')

foreach ($color in $colors) {
	foreach ($style in @('plain', 'tiled', 'glass')) {
		Write-Json (Join-Path $hullModels ("$style-$color.json")) ([ordered]@{
			parent = 'minecraft:block/cube_all'
			textures = [ordered]@{ all = "warpdrive:blocks/hull/$style-$color" }
		})
	}
	Write-Json (Join-Path $hullModels ("omnipanel-$color.json")) ([ordered]@{
		parent = 'minecraft:block/cube_all'
		textures = [ordered]@{ all = "warpdrive:blocks/hull/glass-$color" }
	})
	foreach ($suffix in @('slab', 'slab_top', 'stairs', 'inner_stairs', 'outer_stairs')) {
		Write-Json (Join-Path $hullModels ("plain-$color`_$suffix.json")) ([ordered]@{
			parent = "minecraft:block/$suffix"
			textures = [ordered]@{
				bottom = "warpdrive:blocks/hull/plain-$color"
				top = "warpdrive:blocks/hull/plain-$color"
				side = "warpdrive:blocks/hull/plain-$color"
			}
		})
	}

	# The legacy slab ItemBlock exposed eight metadata variants. Four were half-block forms and
	# four were full blocks with distinct tiled-axis mapping. They now share one registry item and
	# select these prebaked inventory models through warpdrive:slab_variant.
	Write-Json (Join-Path $hullModels ("slab-$color-plain_down.json")) ([ordered]@{
		parent = 'warpdrive:block/slab_down'
		textures = [ordered]@{
			full = "warpdrive:blocks/hull/plain-$color"
			horizontal = "warpdrive:blocks/hull/plain-$color"
		}
	})
	Write-Json (Join-Path $hullModels ("slab-$color-plain_north.json")) ([ordered]@{
		parent = 'warpdrive:block/slab_north'
		textures = [ordered]@{
			full = "warpdrive:blocks/hull/plain-$color"
			horizontal = "warpdrive:blocks/hull/plain-$color"
			vertical = "warpdrive:blocks/hull/plain-$color"
		}
	})
	Write-Json (Join-Path $hullModels ("slab-$color-tiled_down.json")) ([ordered]@{
		parent = 'warpdrive:block/slab_down'
		textures = [ordered]@{
			full = "warpdrive:blocks/hull/tiled-$color"
			horizontal = "warpdrive:blocks/hull/tiled_horizontal-$color"
		}
	})
	Write-Json (Join-Path $hullModels ("slab-$color-tiled_north.json")) ([ordered]@{
		parent = 'warpdrive:block/slab_north'
		textures = [ordered]@{
			full = "warpdrive:blocks/hull/tiled-$color"
			horizontal = "warpdrive:blocks/hull/tiled_horizontal-$color"
			vertical = "warpdrive:blocks/hull/tiled_vertical-$color"
		}
	})
	Write-Json (Join-Path $hullModels ("slab-$color-plain_full.json")) ([ordered]@{
		parent = 'minecraft:block/cube_all'
		textures = [ordered]@{ all = "warpdrive:blocks/hull/plain-$color" }
	})
	Write-Json (Join-Path $hullModels ("slab-$color-tiled_full_x.json")) ([ordered]@{
		parent = 'minecraft:block/cube'
		textures = [ordered]@{
			particle = "warpdrive:blocks/hull/tiled_vertical-$color"
			down = "warpdrive:blocks/hull/tiled_vertical-$color"
			up = "warpdrive:blocks/hull/tiled_vertical-$color"
			north = "warpdrive:blocks/hull/tiled_vertical-$color"
			south = "warpdrive:blocks/hull/tiled_vertical-$color"
			west = "warpdrive:blocks/hull/tiled-$color"
			east = "warpdrive:blocks/hull/tiled-$color"
		}
	})
	Write-Json (Join-Path $hullModels ("slab-$color-tiled_full_y.json")) ([ordered]@{
		parent = 'minecraft:block/cube'
		textures = [ordered]@{
			particle = "warpdrive:blocks/hull/tiled-$color"
			down = "warpdrive:blocks/hull/tiled-$color"
			up = "warpdrive:blocks/hull/tiled-$color"
			north = "warpdrive:blocks/hull/tiled_horizontal-$color"
			south = "warpdrive:blocks/hull/tiled_horizontal-$color"
			west = "warpdrive:blocks/hull/tiled_horizontal-$color"
			east = "warpdrive:blocks/hull/tiled_horizontal-$color"
		}
	})
	Write-Json (Join-Path $hullModels ("slab-$color-tiled_full_z.json")) ([ordered]@{
		parent = 'minecraft:block/cube'
		textures = [ordered]@{
			particle = "warpdrive:blocks/hull/tiled_horizontal-$color"
			down = "warpdrive:blocks/hull/tiled_horizontal-$color"
			up = "warpdrive:blocks/hull/tiled_horizontal-$color"
			north = "warpdrive:blocks/hull/tiled-$color"
			south = "warpdrive:blocks/hull/tiled-$color"
			west = "warpdrive:blocks/hull/tiled_vertical-$color"
			east = "warpdrive:blocks/hull/tiled_vertical-$color"
		}
	})
}

foreach ($tier in $tiers) {
	foreach ($color in $colors) {
		foreach ($style in @('plain', 'tiled', 'glass', 'omnipanel')) {
			$name = "hull.$tier.$style-$color"
			Write-Json (Join-Path $blockstates ($name + '.json')) ([ordered]@{
				variants = [ordered]@{ '' = [ordered]@{ model = "warpdrive:block/hull_generated/$style-$color" } }
			})
			Write-Json (Join-Path $itemModels ($name + '.json')) ([ordered]@{
				parent = "warpdrive:block/hull_generated/$style-$color"
			})
		}

		$slabName = "hull.$tier.slab_$color"
		Write-Json (Join-Path $blockstates ($slabName + '.json')) ([ordered]@{
			variants = [ordered]@{
				'type=bottom' = [ordered]@{ model = "warpdrive:block/hull_generated/plain-$color`_slab" }
				'type=top' = [ordered]@{ model = "warpdrive:block/hull_generated/plain-$color`_slab_top" }
				'type=double' = [ordered]@{ model = "warpdrive:block/hull_generated/plain-$color" }
			}
		})
		$slabVariants = @('plain_north', 'tiled_down', 'tiled_north', 'plain_full',
			'tiled_full_x', 'tiled_full_y', 'tiled_full_z')
		$slabOverrides = @()
		for ($variant = 0; $variant -lt $slabVariants.Count; $variant++) {
			$slabOverrides += [ordered]@{
				predicate = [ordered]@{ 'warpdrive:slab_variant' = $variant + 1 }
				model = "warpdrive:block/hull_generated/slab-$color-$($slabVariants[$variant])"
			}
		}
		Write-Json (Join-Path $itemModels ($slabName + '.json')) ([ordered]@{
			parent = "warpdrive:block/hull_generated/slab-$color-plain_down"
			overrides = $slabOverrides
		})

		$stairsName = "hull.$tier.stairs_$color"
		$legacyStairs = Get-Content (Join-Path $blockstates ($stairsName + '.json')) -Raw | ConvertFrom-Json
		$stairsVariants = [ordered]@{}
		foreach ($property in $legacyStairs.variants.PSObject.Properties) {
			if ($property.Name -eq 'inventory') { continue }
			$oldModel = [string]$property.Value.model
			$modelSuffix = if ($oldModel -match 'outer') { 'outer_stairs' }
				elseif ($oldModel -match 'inner') { 'inner_stairs' } else { 'stairs' }
			$entry = [ordered]@{ model = "warpdrive:block/hull_generated/plain-$color`_$modelSuffix" }
			if ($null -ne $property.Value.x) { $entry.x = [int]$property.Value.x }
			if ($null -ne $property.Value.y) { $entry.y = [int]$property.Value.y }
			if ($null -ne $property.Value.uvlock) { $entry.uvlock = [bool]$property.Value.uvlock }
			$stairsVariants[$property.Name] = $entry
		}
		Write-Json (Join-Path $blockstates ($stairsName + '.json')) ([ordered]@{ variants = $stairsVariants })
		Write-Json (Join-Path $itemModels ($stairsName + '.json')) ([ordered]@{
			parent = "warpdrive:block/hull_generated/plain-$color`_stairs"
		})
	}
}

$legacyLang = @{}
foreach ($line in Get-Content $legacyLangPath) {
	if ($line -match '^([^#=]+)=(.*)$') { $legacyLang[$matches[1]] = $matches[2] }
}
$current = Get-Content $langPath -Raw | ConvertFrom-Json
$language = [ordered]@{}
foreach ($property in $current.PSObject.Properties) { $language[$property.Name] = $property.Value }

foreach ($name in $catalogBlocks) {
	$legacyKey = "tile.warpdrive.$name.name"
	$language["block.warpdrive.$name"] = if ($legacyLang.ContainsKey($legacyKey)) {
		$legacyLang[$legacyKey]
	} else { Title-Case $name }
}

foreach ($tier in $tiers) {
	$tierTitle = Title-Case $tier
	$language["block.warpdrive.projector.$tier.single"] = "$tierTitle Force Field Half Projector"
	$language["block.warpdrive.projector.$tier.double"] = "$tierTitle Force Field Full Projector"
}

$catalogItems = @(
	'book',
	'electromagnetic_cell.basic-empty', 'electromagnetic_cell.basic-ion',
	'electromagnetic_cell.basic-proton', 'electromagnetic_cell.basic-antimatter',
	'electromagnetic_cell.basic-strange_matter',
	'electromagnetic_cell.advanced-empty', 'electromagnetic_cell.advanced-ion',
	'electromagnetic_cell.advanced-proton', 'electromagnetic_cell.advanced-antimatter',
	'electromagnetic_cell.advanced-strange_matter',
	'electromagnetic_cell.superior-empty', 'electromagnetic_cell.superior-ion',
	'electromagnetic_cell.superior-proton', 'electromagnetic_cell.superior-antimatter',
	'electromagnetic_cell.superior-strange_matter',
	'ic2_reactor_laser_focus', 'plasma_torch.basic', 'plasma_torch.advanced', 'plasma_torch.superior',
	'tuning_driver-beam_frequency', 'tuning_driver-control_channel', 'tuning_driver-video_channel'
)
foreach ($name in $catalogItems) {
	$legacyKey = "item.warpdrive.$name.name"
	$language["item.warpdrive.$name"] = if ($legacyLang.ContainsKey($legacyKey)) {
		$legacyLang[$legacyKey]
	} else { Title-Case $name }
}

$colorTitles = @{
	white='White'; orange='Orange'; magenta='Magenta'; light_blue='Light Blue'; yellow='Yellow';
	lime='Lime'; pink='Pink'; gray='Gray'; silver='Silver'; cyan='Cyan'; purple='Purple';
	blue='Blue'; brown='Brown'; green='Green'; red='Red'; black='Black'
}
foreach ($tier in $tiers) {
	$tierTitle = Title-Case $tier
	foreach ($color in $colors) {
		$prefix = $colorTitles[$color] + ' Stained ' + $tierTitle + ' Hull'
		$language["block.warpdrive.hull.$tier.plain-$color"] = $prefix
		$language["block.warpdrive.hull.$tier.tiled-$color"] = $prefix + ' (Tiled)'
		$language["block.warpdrive.hull.$tier.glass-$color"] = $prefix + ' Glass'
		$language["block.warpdrive.hull.$tier.omnipanel-$color"] = $prefix + ' Omnipanel'
		$language["block.warpdrive.hull.$tier.slab_$color"] = $prefix + ' Slab'
		$language["block.warpdrive.hull.$tier.stairs_$color"] = $prefix + ' Stairs'
	}
}

$language['itemGroup.warpdrive.hull'] = "WarpDrive's hulls"
Write-Json $langPath $language

Write-Host ("Generated {0} inert machine blocks, {1} hull blocks and {2} standalone items." -f `
	$catalogBlocks.Count, ($tiers.Count * $colors.Count * 6), $catalogItems.Count)
