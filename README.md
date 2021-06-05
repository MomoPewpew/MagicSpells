# MagicSpells

MagicSpells is a Bukkit plugin that allows you to set up a magic system, and much more. The plugin is very configurable and flexible.
This plugin has nearly endless possibilities.

### This version of MagicSpells

The SneakyRP branch was made for minecraft roleplay on the SneakyRP server. Changelist from master branch:

- Added globalstring variables that function like playerstrings but are persistent across characters.
- Added a sound-on-fail configuration that plays sound effects when you don't have a permission.
- Added permissions for magicspells.advanced.modifyvariable, magicitem, debug, forcecast, reload, resetcd and castat.
- Fixed a bug that caused location drift on pulserspells and targetedmultispells.
- Cleaned up debug logging and added chat logging.
- Creative mode players are now valid targets for spells.
- Spectator mode players are now only valid targets if "can-target: spectator" is set
- added modifiers nbtdebug, nbtexists, nbtgreaterthan, nbtlessthan, nbtequals, nbtmatches. These modifiers can read any NBT tag of your choosing.
- Added a trigger "gamemode N" where N is the new gamemode.

### Building the project

The build.bat file in the git should compile the project for you. At this time it is not completely flawless though, and you will have to go in and add the plugin.yml manually.