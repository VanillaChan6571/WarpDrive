-- WarpDrive ship controller
--
-- Layout follows the 1.12.2 warpdriveShipController "Ship controls" page: header bar, grouped
-- Ship / Dimensions / Warp data blocks with aligned '=' columns, and a coloured control bar
-- pinned to the bottom of the screen.
--
-- Self-contained on purpose: the original depended on a 38 KB warpdriveCommons API, of which the
-- controller only used ~34 helpers. Those are inlined below.

--------------------------------------------------------------------- commons

local isColour = term.isColour()

local function setStyle(front, back)
  term.setTextColour(isColour and front or colours.white)
  term.setBackgroundColour(isColour and back or colours.black)
end

-- Exact colour pairs from the 1.12.2 warpdriveCommons styles table.
local function setColorNormal()   setStyle(colours.black , colours.lightGrey) end
local function setColorHelp()     setStyle(colours.white , colours.blue) end
local function setColorHeader()   setStyle(colours.orange, colours.black) end
local function setColorSuccess()  setStyle(colours.white , colours.lime) end
local function setColorWarning()  setStyle(colours.white , colours.red) end
local function setColorError()    setStyle(colours.red   , colours.lightGrey) end
local function setColorControl()  setStyle(colours.white , colours.blue) end
local function setColorSelected() setStyle(colours.black , colours.lightBlue) end

local width, height = term.getSize()

local function writeLn(text)
  print(text or "")
end

local function writeFullLine(text)
  text = text or ""
  term.write(text .. string.rep(" ", math.max(0, width - #text)))
  local _, y = term.getCursorPos()
  term.setCursorPos(1, y + 1)
end

local function clearLine()
  term.clearLine()
  local _, y = term.getCursorPos()
  term.setCursorPos(1, y)
end

local function writeCentered(y, text)
  term.setCursorPos(math.max(1, math.floor((width - #text) / 2) + 1), y)
  term.write(text)
  term.setCursorPos(1, y + 1)
end

-- 1,234,567 style, matching the legacy readout
local function format_integer(value)
  value = math.floor(tonumber(value) or 0)
  local sign = value < 0 and "-" or ""
  local digits = tostring(math.abs(value))
  local out = ""
  while #digits > 3 do
    out = "," .. digits:sub(-3) .. out
    digits = digits:sub(1, -4)
  end
  return sign .. digits .. out
end

local function page_begin(title)
  setColorNormal()
  term.clear()
  term.setCursorPos(1, 1)
  setColorHeader()
  clearLine()
  writeCentered(1, title)
  setColorNormal()
end

local function input_readInteger(current)
  local input = format_integer(current)
  if input == "0" then input = "" end
  local x, y = term.getCursorPos()
  local done = false

  term.setCursorBlink(true)
  repeat
    term.setCursorPos(x, y)
    setColorNormal()
    term.write(input .. string.rep(" ", math.max(0, 12 - #input)))
    input = input:sub(-9)
    term.setCursorPos(x + #input, y)

    local event, value = os.pullEventRaw()
    if event == "char" then
      if value >= "0" and value <= "9" then
        input = input .. value
      elseif value == "-" or value == "n" or value == "N" then
        input = input:sub(1, 1) == "-" and input:sub(2) or ("-" .. input)
      elseif value == "+" or value == "p" or value == "P" then
        if input:sub(1, 1) == "-" then input = input:sub(2) end
      end
    elseif event == "key" then
      if value == keys.backspace then
        input = input:sub(1, #input - 1)
      elseif value == keys.delete then
        input = ""
      elseif value == keys.enter then
        done = true
      end
    elseif event == "terminate" then
      done = true
    end
  until done

  term.setCursorBlink(false)
  term.setCursorPos(1, y + 1)
  setColorNormal()
  if input == "" or input == "-" then return current end
  return math.floor(tonumber(input) or current)
end

local function input_readText(current)
  setColorSelected()
  term.setCursorBlink(true)
  local entered = read()
  term.setCursorBlink(false)
  setColorNormal()
  if entered == nil or entered == "" then
    return current
  end
  return entered
end

local function input_readConfirmation(message)
  local oldX, oldY = term.getCursorPos()
  term.setCursorPos(1, height)
  setColorWarning()
  writeFullLine(" " .. (message or "Are you sure? (Y/n)"))
  setColorNormal()
  repeat
    local event, value = os.pullEventRaw()
    local answer = nil
    if event == "key" and value == keys.enter then answer = true end
    if event == "char" then answer = value == "y" or value == "Y" end
    if event == "terminate" then answer = false end
    if answer ~= nil then
      term.setCursorPos(1, height)
      setColorNormal()
      clearLine()
      term.setCursorPos(oldX, oldY)
      return answer
    end
  until false
end

local function status_show(colourFn, message)
  term.setCursorPos(1, height)
  colourFn()
  writeFullLine(" " .. message)
  setColorNormal()
end

--------------------------------------------------------------------- ship

local ship = nil

local function findShip()
  for _, side in ipairs(peripheral.getNames()) do
    local candidate = peripheral.wrap(side)
    if candidate ~= nil and candidate.getAssemblyStatus ~= nil and candidate.dim_positive ~= nil then
      return candidate
    end
  end
  return nil
end

local function boot()
  setColorNormal()
  term.clear()
  term.setCursorPos(1, 1)
  writeLn("Booting Ship")
  writeLn("")

  term.write("- acquiring ship core : ")
  ship = findShip()
  if ship == nil then
    setColorError()
    writeLn("FAILED")
    setColorNormal()
    writeLn("")
    writeLn("No Ship Core peripheral found.")
    writeLn("Place this computer touching a Ship Core,")
    writeLn("or connect one with a wired modem.")
    return false
  end
  setColorSuccess()
  writeLn("ok")

  setColorNormal()
  term.write("- checking assembly   : ")
  local isValid, message = ship.getAssemblyStatus()
  if isValid then
    setColorSuccess()
    writeLn("ok")
  else
    setColorWarning()
    writeLn(message or "invalid")
  end
  setColorNormal()
  sleep(0.6)
  return true
end

--------------------------------------------------------------------- pages

local function page_setDimensions()
  page_begin("<==== Set ship dimensions ====>")
  local front, right, up = ship.dim_positive()
  local back, left, down = ship.dim_negative()

  term.setCursorPos(1, 3)
  setColorHelp()
  writeLn(" Enter each distance from the Ship Core.")
  writeLn(" Blank keeps the current value.")
  writeLn("")
  setColorNormal()

  term.write(" Front (" .. front .. "): ") front = input_readInteger(front)
  term.write(" Right (" .. right .. "): ") right = input_readInteger(right)
  term.write(" Up    (" .. up .. "): ")    up    = input_readInteger(up)
  term.write(" Back  (" .. back .. "): ")  back  = input_readInteger(back)
  term.write(" Left  (" .. left .. "): ")  left  = input_readInteger(left)
  term.write(" Down  (" .. down .. "): ")  down  = input_readInteger(down)

  local ok, message = ship.setDimensions(front, back, left, right, up, down)
  status_show(ok and setColorSuccess or setColorError, message or "")
  sleep(1.2)
end

local function writeMovement(prefix, forward, up, right, rotationSteps)
  local parts = {}
  if forward > 0 then table.insert(parts, format_integer(forward) .. " front")
  elseif forward < 0 then table.insert(parts, format_integer(-forward) .. " back") end
  if up > 0 then table.insert(parts, format_integer(up) .. " up")
  elseif up < 0 then table.insert(parts, format_integer(-up) .. " down") end
  if right > 0 then table.insert(parts, format_integer(right) .. " right")
  elseif right < 0 then table.insert(parts, format_integer(-right) .. " left") end
  if rotationSteps == 1 then table.insert(parts, "Turn right")
  elseif rotationSteps == 2 then table.insert(parts, "Turn back")
  elseif rotationSteps == 3 then table.insert(parts, "Turn left") end
  writeLn(prefix .. (#parts == 0 and "(none)" or table.concat(parts, ", ")))
end

local function page_setDistanceAxis(line, axis, positive, negative,
                                    current, shipLength, maxJumpDistance, offset)
  local maximumDistance = math.floor(shipLength + maxJumpDistance)
  local entered
  repeat
    term.setCursorPos(1, line + 2)
    setColorHelp()
    writeFullLine(" Enter between " .. format_integer(offset + math.floor(shipLength + 1))
      .. " and " .. format_integer(offset + maximumDistance) .. " to move " .. positive .. ".")
    writeFullLine(" Enter " .. format_integer(offset) .. " to keep position on this axis.")
    writeFullLine(" Enter between " .. format_integer(offset - maximumDistance)
      .. " and " .. format_integer(offset + math.floor(-shipLength - 1))
      .. " to move " .. negative .. ".")

    term.setCursorPos(1, line)
    setColorNormal()
    clearLine()
    term.write(axis .. " movement: ")
    entered = input_readInteger(offset + current)
    if math.abs(entered - offset) > maximumDistance then
      status_show(setColorWarning, "Wrong distance. Try again.")
      sleep(0.8)
    end
  until math.abs(entered - offset) <= maximumDistance

  setColorNormal()
  for y = line + 2, line + 4 do
    term.setCursorPos(1, y)
    clearLine()
  end
  return entered - offset
end

-- This is the 1.12.2 M/P page: M edits forward/up/right; P edits absolute X/Y/Z while
-- retaining the same ship-relative movement tuple under the hood.
local function page_setMovement(isByPosition)
  ship.command("MANUAL", false)
  local success, maxJumpDistance = ship.getMaxJumpDistance()
  if success ~= true then
    page_begin("<==== Set ship movement ====>")
    term.setCursorPos(1, 3)
    status_show(setColorWarning, tostring(maxJumpDistance))
    sleep(1.2)
    return false
  end

  local movement = { ship.movement() }
  local rotationSteps = ship.rotationSteps()
  local front, right, up = ship.dim_positive()
  local back, left, down = ship.dim_negative()
  local shipX, shipY, shipZ = ship.getLocalPosition()
  local lenFB = math.abs(front + back + 1)
  local lenUD = math.abs(up + down + 1)
  local lenLR = math.abs(left + right + 1)

  page_begin("<==== Set ship movement ====>")
  term.setCursorPos(1, 3)
  setColorNormal()
  writeMovement("Current movement is ", movement[1], movement[2], movement[3], rotationSteps)

  if isByPosition then
    local dx, _, dz = ship.getOrientation()
    if dx == 0 then
      movement[3] = -dz * page_setDistanceAxis(4, "X", "East", "West",
        movement[3], lenLR, maxJumpDistance, shipX)
      movement[1] = dz * page_setDistanceAxis(6, "Z", "South", "North",
        movement[1], lenFB, maxJumpDistance, shipZ)
    else
      movement[1] = dx * page_setDistanceAxis(4, "X", "East", "West",
        movement[1], lenFB, maxJumpDistance, shipX)
      movement[3] = dx * page_setDistanceAxis(6, "Z", "South", "North",
        movement[3], lenLR, maxJumpDistance, shipZ)
    end
    movement[2] = page_setDistanceAxis(8, "Y", "Up", "Down",
      movement[2], lenUD, maxJumpDistance, shipY)
  else
    movement[1] = page_setDistanceAxis(4, "Forward/back", "Forward", "Backward",
      movement[1], lenFB, maxJumpDistance, 0)
    movement[2] = page_setDistanceAxis(6, "Up/down", "Up", "Down",
      movement[2], lenUD, maxJumpDistance, 0)
    movement[3] = page_setDistanceAxis(8, "Right/left", "Right", "Left",
      movement[3], lenLR, maxJumpDistance, 0)
  end

  ship.movement(movement[1], movement[2], movement[3])
  return true
end

local function page_setRotation()
  page_begin("<==== Set ship rotation ====>")
  term.setCursorPos(1, 8)
  setColorHelp()
  writeFullLine(" Select ship rotation (Up, Down, Left, Right).")
  writeFullLine(" Select Front to keep current orientation.")
  writeFullLine(" Press Enter to save your selection.")

  local steps = ship.rotationSteps()
  local done = false
  repeat
    term.setCursorPos(1, 3)
    setColorNormal()
    clearLine()
    if steps == 0 then writeLn(" Rotation         = Front    ")
    elseif steps == 1 then writeLn(" Rotation         = Right +90")
    elseif steps == 2 then writeLn(" Rotation         = Back 180 ")
    else writeLn(" Rotation         = Left -90 ") end

    local event, value = os.pullEventRaw()
    if event == "key" then
      if value == keys.up then steps = 0
      elseif value == keys.left then steps = 3
      elseif value == keys.right then steps = 1
      elseif value == keys.down then steps = 2
      elseif value == keys.enter then done = true end
    elseif event == "terminate" then
      done = true
    end
  until done
  ship.rotationSteps(steps)
end

local function page_setName()
  page_begin("<==== Set ship name ====>")
  term.setCursorPos(1, 3)
  setColorNormal()
  term.write(" Name (" .. ship.name() .. "): ")
  local newName = input_readText(ship.name())
  ship.name(newName)
  status_show(setColorSuccess, "Ship renamed to '" .. newName .. "'")
  sleep(1.2)
end

local function page_jump()
  if not input_readConfirmation("Engage jump drive? (Y/n)") then
    return
  end
  local mx, my, mz = ship.movement()
  local rotation = ship.rotationSteps()
  ship.command("MANUAL", false)
  ship.movement(mx, my, mz)
  ship.rotationSteps(rotation)
  -- Confirming MANUAL schedules the countdown on the core, as it did in 1.12.2.
  local ok, message = ship.command("MANUAL", true)
  status_show(ok and setColorSuccess or setColorError, message or "")
  sleep(1.2)
end

-- Main page. Field layout copied from the 1.12.2 "Ship controls" page.
local function page_controls()
  page_begin(ship.name() .. " - Ship controls")

  local isValid, assemblyMessage = ship.getAssemblyStatus()
  local shipState, countdown, cooldown = ship.getJumpTimers()

  if shipState == 1 then
    status_show(setColorWarning, string.format("JUMPING IN %.1fs  -  press A to abort", countdown / 20))
  elseif shipState == 2 then
    status_show(setColorHelp, string.format("Drive cooling down: %.1fs", cooldown / 20))
  elseif not isValid then
    status_show(setColorWarning, assemblyMessage or "Ship not assembled")
  end

  term.setCursorPos(1, 3)
  setColorNormal()

  local x, y, z = ship.getLocalPosition()
  local energyStored, energyMax, energyUnits = ship.getEnergyStatus()
  energyStored = energyStored or 0
  if energyMax == nil or energyMax == 0 then energyMax = 1 end
  energyUnits = energyUnits or "FE"

  writeLn("Ship:")
  writeLn(" Current position = " .. format_integer(x) .. ", " .. format_integer(y) .. ", " .. format_integer(z))
  writeLn(" Energy           = " .. math.floor(energyStored / energyMax * 100) .. " % ("
    .. format_integer(energyStored) .. " " .. energyUnits .. ")")

  writeLn("")
  writeLn("Dimensions:")
  local front, right, up = ship.dim_positive()
  local back, left, down = ship.dim_negative()
  writeLn(" Front, Right, Up = " .. format_integer(front) .. ", " .. format_integer(right) .. ", " .. format_integer(up) .. " blocks")
  writeLn(" Back, Left, Down = " .. format_integer(back) .. ", " .. format_integer(left) .. ", " .. format_integer(down) .. " blocks")

  local shipMass, shipVolume = ship.getShipSize()
  writeLn(" Mass, Volume     = " .. format_integer(shipMass or 0) .. " blocks, "
    .. format_integer(shipVolume or 0) .. " envelope")

  if isValid then
    local forward, moveUp, right = ship.movement()
    local energyValid, required = ship.getEnergyRequired()
    local distance = math.ceil(math.sqrt(forward * forward + moveUp * moveUp + right * right))
    local jumps = 0
    if energyValid and required and required > 0 then
      jumps = math.floor(energyStored / required)
    end

    local dx, _, dz = ship.getOrientation()
    local worldX = dx * forward - dz * right
    local worldZ = dz * forward + dx * right

    writeLn("")
    writeLn("Warp data:")
    writeMovement(" Movement         = ", forward, moveUp, right, ship.rotationSteps())
    writeLn(" Distance         = " .. format_integer(distance) .. " m ("
      .. format_integer(required or 0) .. " " .. energyUnits .. ", " .. jumps .. " jumps)")
    writeLn(" Target position  = " .. format_integer(x + worldX) .. ", "
      .. format_integer(y + moveUp) .. ", " .. format_integer(z + worldZ))
  end

  -- Control bar pinned to the bottom, as in 1.12.2
  term.setCursorPos(1, height - 2)
  setColorControl()
  writeFullLine(" set ship Name (N), dImensions (I), Movement (M/P)")
  writeFullLine(" Jump to move ship (J), Abort jump (A), Quit (Q)")
  setColorNormal()
end

--------------------------------------------------------------------- main

if not boot() then
  return
end

local running = true
while running do
  page_controls()

  -- Wait for a key OR a refresh tick, so energy and status update on their own
  local acted = false
  local refresh = os.startTimer(1)
  while not acted do
    local event, p1 = os.pullEvent()
    if event == "key" then
      local name = keys.getName(p1)
      if name == "n" then page_setName() acted = true
      elseif name == "i" then page_setDimensions() acted = true
      elseif name == "m" then
        if page_setMovement(false) then page_setRotation() page_jump() end
        acted = true
      elseif name == "p" then
        if page_setMovement(true) then page_setRotation() page_jump() end
        acted = true
      elseif name == "r" then page_setRotation() acted = true
      elseif name == "j" then page_jump() acted = true
      elseif name == "a" then ship.abortJump() acted = true
      elseif name == "q" then running = false acted = true
      else acted = true end
    elseif event == "timer" and p1 == refresh then
      acted = true   -- redraw with fresh values
    end
  end
end

setColorNormal()
term.clear()
term.setCursorPos(1, 1)
print("Ship controller stopped.")
