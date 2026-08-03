-- WarpDrive Auto-Startup Script (CC:Tweaked datapack)
-- Automatically detects a Ship Core adjacent to the computer and launches shipcontrol

-- Bail out early if the program isn't available in the ROM or on disk
if not fs.exists("rom/programs/shipcontrol.lua")
	and not fs.exists("shipcontrol")
	and not fs.exists("shipcontrol.lua") then
	return
end

-- Locate an adjacent Ship Core peripheral
local function findShipCore()
	local sides = { "top", "bottom", "left", "right", "front", "back" }
	for _, side in ipairs(sides) do
		if peripheral.isPresent(side) then
			-- Prefer explicit peripheral type, but fall back to fuzzy match
			local pType = peripheral.getType(side)
			if pType == "warpdrive:ship_core" or string.find(pType or "", "ship_core") then
				return side
			end
		end
	end
	return nil
end

local shipCoreSide = findShipCore()
if not shipCoreSide then
	-- No Ship Core next to us, boot normally
	return
end

-- Ship Core detected: launch the control UI
term.clear()
term.setCursorPos(1, 1)
term.setTextColor(colors.lime)
print("=================================")
print("  WarpDrive Ship Core Detected!")
print("=================================")
term.setTextColor(colors.white)
print("")
print("Detected on: " .. shipCoreSide)
print("")
print("Launching control interface...")
sleep(1.5)

if fs.exists("rom/programs/shipcontrol.lua") then
	shell.run("rom/programs/shipcontrol.lua")
elseif fs.exists("shipcontrol.lua") then
	shell.run("shipcontrol.lua")
elseif fs.exists("shipcontrol") then
	shell.run("shipcontrol")
end

-- If something fails above, fall back to normal boot behavior
