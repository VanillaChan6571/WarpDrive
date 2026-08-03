-- WarpDrive Ship Control Terminal Interface
-- A terminal-style menu for controlling your ship

local version = "1.0.0"
local shipCore = nil
local running = true

-- Colors
local colorHeader = colors.yellow
local colorNormal = colors.white
local colorSuccess = colors.lime
local colorError = colors.red
local colorPrompt = colors.cyan

-- Clear screen and show header
local function drawHeader()
    term.clear()
    term.setCursorPos(1, 1)
    term.setBackgroundColor(colors.black)
    term.setTextColor(colorHeader)
    print("==================================")
    print("   WarpDrive Ship Control v" .. version)
    print("==================================")
    term.setTextColor(colorNormal)
    print("")
end

-- Find Ship Core peripheral
local function findShipCore()
    local sides = {"top", "bottom", "left", "right", "front", "back"}

    for _, side in ipairs(sides) do
        if peripheral.isPresent(side) then
            local pType = peripheral.getType(side)
            if pType == "warpdrive:ship_core" or string.find(pType, "ship_core") then
                return peripheral.wrap(side), side
            end
        end
    end

    return nil, nil
end

-- Display ship status
local function displayStatus()
    if not shipCore then
        term.setTextColor(colorError)
        print("No Ship Core detected!")
        term.setTextColor(colorNormal)
        return
    end

    local energy = shipCore.getEnergyStored()
    local shipSize = shipCore.getShipSize()
    local energyRequired = shipCore.getEnergyRequired()

    term.setTextColor(colorSuccess)
    print("Ship Status:")
    term.setTextColor(colorNormal)
    print("  Energy      : " .. string.format("%,d FE", energy))
    print("  Ship Size   : " .. shipSize .. " blocks")
    print("  Required    : " .. string.format("%,d FE", energyRequired))

    if energy >= energyRequired then
        term.setTextColor(colorSuccess)
        print("  Status      : READY TO JUMP")
    else
        term.setTextColor(colorError)
        print("  Status      : INSUFFICIENT ENERGY")
    end
    term.setTextColor(colorNormal)
    print("")
end

-- Main menu
local function showMenu()
    print("Commands:")
    print("  [S] Scan Ship")
    print("  [J] Jump")
    print("  [D] Set Destination")
    print("  [R] Refresh Status")
    print("  [Q] Quit")
    print("")
    term.setTextColor(colorPrompt)
    write("Enter command: ")
    term.setTextColor(colorNormal)
end

-- Scan ship
local function scanShip()
    if not shipCore then
        term.setTextColor(colorError)
        print("Error: No Ship Core detected!")
        term.setTextColor(colorNormal)
        return
    end

    print("")
    term.setTextColor(colorPrompt)
    print("Scanning ship...")
    term.setTextColor(colorNormal)

    local success, size, message = shipCore.scan()

    if success then
        term.setTextColor(colorSuccess)
        print("Scan successful!")
        print("Ship size: " .. size .. " blocks")
    else
        term.setTextColor(colorError)
        print("Scan failed: " .. message)
    end
    term.setTextColor(colorNormal)
    print("")
    print("Press any key to continue...")
    os.pullEvent("key")
end

-- Set destination
local function setDestination()
    if not shipCore then
        term.setTextColor(colorError)
        print("Error: No Ship Core detected!")
        term.setTextColor(colorNormal)
        return
    end

    print("")
    term.setTextColor(colorPrompt)
    print("Set Destination")
    term.setTextColor(colorNormal)

    write("X coordinate: ")
    local x = tonumber(read())
    write("Y coordinate: ")
    local y = tonumber(read())
    write("Z coordinate: ")
    local z = tonumber(read())
    write("Dimension (default: minecraft:overworld): ")
    local dim = read()
    if dim == "" then dim = "minecraft:overworld" end

    if not x or not y or not z then
        term.setTextColor(colorError)
        print("Invalid coordinates!")
        term.setTextColor(colorNormal)
        print("")
        print("Press any key to continue...")
        os.pullEvent("key")
        return
    end

    local success, message = shipCore.setDestination(x, y, z, dim)

    if success then
        term.setTextColor(colorSuccess)
        print("Destination set!")
        print("Target: " .. x .. ", " .. y .. ", " .. z)
    else
        term.setTextColor(colorError)
        print("Failed: " .. message)
    end
    term.setTextColor(colorNormal)
    print("")
    print("Press any key to continue...")
    os.pullEvent("key")
end

-- Perform jump
local function performJump()
    if not shipCore then
        term.setTextColor(colorError)
        print("Error: No Ship Core detected!")
        term.setTextColor(colorNormal)
        return
    end

    print("")
    term.setTextColor(colors.orange)
    print("WARNING: Initiating warp jump!")
    term.setTextColor(colorNormal)
    write("Are you sure? (y/n): ")
    local confirm = read()

    if confirm:lower() ~= "y" then
        print("Jump cancelled.")
        print("")
        print("Press any key to continue...")
        os.pullEvent("key")
        return
    end

    term.setTextColor(colorPrompt)
    print("Jumping...")
    term.setTextColor(colorNormal)

    local success, message = shipCore.jump()

    if success then
        term.setTextColor(colorSuccess)
        print("Jump successful!")
        print(message)
    else
        term.setTextColor(colorError)
        print("Jump failed!")
        print(message)
    end
    term.setTextColor(colorNormal)
    print("")
    print("Press any key to continue...")
    os.pullEvent("key")
end

-- Main program
local function main()
    drawHeader()

    -- Find Ship Core
    term.setTextColor(colorPrompt)
    print("Detecting Ship Core...")
    term.setTextColor(colorNormal)

    shipCore, side = findShipCore()

    if shipCore then
        term.setTextColor(colorSuccess)
        print("Ship Core detected on " .. side .. " side!")
        term.setTextColor(colorNormal)
        print("")
        sleep(1)
    else
        term.setTextColor(colorError)
        print("ERROR: No Ship Core detected!")
        print("")
        print("Please place this computer adjacent to")
        print("a Ship Core block and try again.")
        term.setTextColor(colorNormal)
        return
    end

    -- Main loop
    while running do
        drawHeader()
        displayStatus()
        showMenu()

        local event, key = os.pullEvent("key")
        local keyChar = keys.getName(key):lower()

        if keyChar == "s" then
            scanShip()
        elseif keyChar == "j" then
            performJump()
        elseif keyChar == "d" then
            setDestination()
        elseif keyChar == "r" then
            -- Just refresh by looping
        elseif keyChar == "q" then
            running = false
        end
    end

    drawHeader()
    print("Thank you for using WarpDrive Ship Control!")
    print("")
end

-- Run
main()
