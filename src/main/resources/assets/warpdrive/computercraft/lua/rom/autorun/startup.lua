-- WarpDrive Auto-Startup Script
-- Automatically detects Ship Core and launches control interface

-- Check if shipcontrol program exists
if not fs.exists("rom/programs/shipcontrol.lua") and not fs.exists("shipcontrol") and not fs.exists("shipcontrol.lua") then
    -- Program not found, skip auto-start
    return
end

-- Find Ship Core peripheral
local function findShipCore()
    local sides = {"top", "bottom", "left", "right", "front", "back"}
    for _, side in ipairs(sides) do
        if peripheral.isPresent(side) then
            local pType = peripheral.getType(side)
            if pType == "warpdrive:ship_core" or string.find(pType or "", "ship_core") then
                return side
            end
        end
    end
    return nil
end

-- Check for Ship Core
local shipCoreSide = findShipCore()

if shipCoreSide then
    -- Ship Core detected! Auto-launch control interface
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

    -- Launch shipcontrol
    if fs.exists("rom/programs/shipcontrol.lua") then
        shell.run("rom/programs/shipcontrol.lua")
    elseif fs.exists("shipcontrol.lua") then
        shell.run("shipcontrol.lua")
    elseif fs.exists("shipcontrol") then
        shell.run("shipcontrol")
    end
end

-- If no Ship Core detected, boot normally (do nothing)
