# WarpDrive for 1.16.5
### Still Development - Build at your own risk!
[![WarpDrive Curse statistics](http://cf.way2muchnoise.eu/warpdrive.svg)](http://minecraft.curseforge.com/projects/warpdrive)
[![Build Status](https://travis-ci.org/LemADEC/WarpDrive.svg?branch=MC1.7)](https://travis-ci.org/LemADEC/WarpDrive)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/dd939aed95ab4fac9eab9c2b63e0028b)](https://app.codacy.com/gh/VanillaChan6571/WarpDrive/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

An update to the WarpDrive mod for 1.16.5. Currently in progress.
Faithful port with some modernization from 1.12.2!

If you would like to help, find an issue and then fork the repository. If you can fix it, submit a pull request and we will accept it! This is valid even if you dont know how to code, modifications to textures, resources, wikis, and everything else are up for improvment.

See mcmod.info for credits.

See the official forum [here](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2510855).

## Installation

1.  Download WarpDrive.jar from the [Curse website](http://minecraft.curseforge.com/projects/warpdrive) and put it in your mods folder. (Currently 1.12.2 is only option at this time)

2.  To move your ship, you'll ComputerCraft CC:Tweaked for 1.16.5 or Build a Ship Controller that comes with Warp Drive now.

3.  FE/µI, EU and RF power are supported (#TODO - including but not limited to IC2, GregTech, AdvancedSolarPanel, BigReactors, EnderIO, Thermal Expansion, ImmersiveEngineering).
    ICBM, MFFS, Advanced Repulsion System, Advanced Solar Panels and GraviSuite are supported.

## Developping

To setup you development environment:
1.  From the WarpDrive mod folder, type:
```
./gradlew setupDecompWorkspace
```
2.  Start IdeaJ.
3.  Import the gradle project.
4.  Import the code formating & inspection rules from `IntelliJ IDEA-Code Style.xml` and `IntelliJ IDEA-Inspection.xml`.
5.  Create run configuration using gradle, select the gradle project, enter the task `runClient` or `runServer`.
