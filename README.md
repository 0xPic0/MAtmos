# MAtmos

MAtmos, a Minecraft mod.

Originally authored by Hurricaaane, updated by Sollace to from Minecraft 1.8 to Minecraft 1.10.

Ported to Minecraft 1.12 and 1.12.2 (todo) by ndousson.

# Requierements

Libraries required before compiling this project:

- [MC Commons](https://github.com/Sollace/MC-Commons)
- [LiteLoader](http://www.liteloader.com/)

# Installation (IntelliJ IDEA)

1. Clone project
2. Import project with build.gradle in IDE
3. Run gradle task named setupDecompWorkspace
4. Refresh all gradle project
5. Clone MC-Commons in lib
6. Run gradle genIntellijRuns
7. Run minecraft client
8. Start coding
9. Run gradle build when you finished

# DO

- Updated code to MC 1.12
- Updated ForgeGradle to 2.3-SNAPSHOT
- Updated MCPBot Mapping Release

# TODO

- Fix loading lang (no lang is working)
- Fix message display in chat

# More About Requierements

MAtmos source code requires some custom libraries/classes found at https://github.com/Hurricaaane/MC-Commons/ (WTFPLv2).
1.8 Fork at: https://github.com/dags-/MC-Commons

The (net.sf.) PracticalXML library (Apache License) is also required to compile versions that include the XML expansion converter:
- net.sf.practicalxml.*

Requires a LiteLoader workspace. Before decompiling, make sure you have followed the instructions located at:
https://www.assembla.com/code/liteloader/subversion/nodes/202/LiteLoader/trunk/README.md