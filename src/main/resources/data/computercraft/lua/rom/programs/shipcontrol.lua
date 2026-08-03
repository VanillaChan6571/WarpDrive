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

local function setColorNormal()   term.setTextColour(isColour and colours.white  or colours.white) end
local function setColorHelp()     term.setTextColour(isColour and colours.lightGrey or colours.white) end
local function setColorSuccess()  term.setTextColour(isColour and colours.lime   or colours.white) end
local function setColorWarning()  term.setTextColour(isColour and colours.orange or colours.white) end
local function setColorError()    term.setTextColour(isColour and colours.red    or colours.white) end
local function setColorControl()  term.setTextColour(isColour and colours.cyan   or colours.white) end
local function setColorSelected() term.setTextColour(isColour and colours.yellow or colours.white) end

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
  term.clear()
  term.setCursorPos(1, 1)
  setColorSelected()
  writeFullLine(" " .. title)
  setColorNormal()
end

local function input_readInteger(current)
  setColorSelected()
  term.setCursorBlink(true)
  local entered = read()
  term.setCursorBlink(false)
  setColorNormal()
  if entered == nil or entered == "" then
    return current
  end
  return math.floor(tonumber(entered) or current)
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
  setColorWarning()
  writeLn(message or "Are you sure? (y/n)")
  setColorNormal()
  local _, key = os.pullEvent("key")
  return keys.getName(key) == "y"
end

local function status_show(colourFn, message)
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
  term.clear()
  term.setCursorPos(1, 1)
  setColorNormal()
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

local function page_setMovement()
  page_begin("<==== Set ship movement ====>")
  local mx, my, mz = ship.movement()

  term.setCursorPos(1, 3)
  setColorHelp()
  writeLn(" Distance to move, relative to the ship.")
  writeLn("")
  setColorNormal()

  term.write(" X (" .. mx .. "): ") mx = input_readInteger(mx)
  term.write(" Y (" .. my .. "): ") my = input_readInteger(my)
  term.write(" Z (" .. mz .. "): ") mz = input_readInteger(mz)

  local ok, message = ship.movement(mx, my, mz)
  status_show(ok and setColorSuccess or setColorError, message or "Movement set")
  sleep(1.2)
end

local function page_setRotation()
  page_begin("<==== Set ship rotation ====>")
  term.setCursorPos(1, 3)
  setColorHelp()
  writeLn(" 0 = none, 1 = 90 right, 2 = 180, 3 = 90 left")
  writeLn("")
  setColorNormal()
  local steps = ship.rotationSteps()
  term.write(" Rotation steps (" .. steps .. "): ")
  steps = input_readInteger(steps)
  ship.rotationSteps(steps)
  status_show(setColorSuccess, "Rotation set to " .. (steps % 4))
  sleep(1.2)
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
  page_begin("<==== Jump ====>")
  local mx, my, mz = ship.movement()
  term.setCursorPos(1, 3)
  setColorNormal()
  writeLn(" Movement = " .. mx .. ", " .. my .. ", " .. mz)
  writeLn(" Rotation = " .. ship.rotationSteps())
  writeLn("")
  if not input_readConfirmation(" Engage warp drive? (y/n)") then
    return
  end
  -- jump() only schedules: the countdown runs on the ship core, and the destination chunks are
  -- force-loaded while it counts down. Progress shows on the main page.
  local ok, message = ship.jump()
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
    local mx, my, mz = ship.movement()
    local required = ship.getEnergyRequired()
    local _, maxDistance = ship.getMaxJumpDistance()
    local distance = math.floor(math.sqrt(mx * mx + my * my + mz * mz))
    local jumps = 0
    if required and required > 0 then
      jumps = math.floor(energyStored / required)
    end

    writeLn("")
    writeLn("Warp data:")
    writeLn(" Movement         = " .. mx .. ", " .. my .. ", " .. mz .. " (rot " .. ship.rotationSteps() .. ")")
    writeLn(" Distance         = " .. format_integer(distance) .. " m ("
      .. format_integer(required or 0) .. " " .. energyUnits .. ", " .. jumps .. " jumps)")
    writeLn(" Target position  = " .. format_integer(x + mx) .. ", " .. format_integer(y + my) .. ", " .. format_integer(z + mz))
    writeLn(" Max jump         = " .. format_integer(maxDistance or 0) .. " m")
  end

  -- Control bar pinned to the bottom, as in 1.12.2
  term.setCursorPos(1, height - 2)
  setColorControl()
  writeFullLine(" set ship Name (N), dImensions (I), Movement (M)")
  writeFullLine(" Rotation (R), Jump (J), Abort (A), Quit (Q)")
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
      elseif name == "m" then page_setMovement() acted = true
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

term.clear()
term.setCursorPos(1, 1)
setColorNormal()
print("Ship controller stopped.")
