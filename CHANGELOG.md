# Changelog

## 0.1.0-beta.6

### Added

- Siege engine ownership, team and alliance access, capture of unmanned enemy machines, and release of abandoned machines.
- Datapack rules for block material strength, penetration resistance and fracture energy.
- Server settings for terrain damage and stone fracture energy. Fire and falling debris can respect claim protection.
- A clickable update notice, shown once per game launch, with a client setting to disable it.
- Config layout versions. Missing settings are added without resetting valid existing values.

### Changed

- Projectile flight now uses gravity, air resistance and rocket thrust. Aiming calculations use the same flight model.
- Penetration and crater damage use projectile mass, diameter, speed and the material struck. Block cracks are saved with the world and remain until the block is removed or its state changes.
- Tower Crossbow bolts launch at 120 m/s; Arcballista bolts at 140 m/s.
- Cannon launch speeds are 300 m/s for Culverin, 330 m/s for Serpentine and 315 m/s for Mons Meg.
- Renamed Hwacha ammunition to So-singijeon and Jung-singijeon, with automatic migration of old item IDs.
- Tower Crossbow bolts stack to 16.
- Projectile and engine profiles use format 2. Custom datapacks need to be updated; see the [wiki](https://github.com/mess1re/siegeworks/wiki/Data-Pack-Reference).
- Projectile profiles are synchronized from the server on joining and after reload. Invalid overrides report their source and do not replace a working catalog during reload.
- Requires Axiomata 0.1.0-beta.5 or newer.

### Fixed

- Siege engineers keep their firing target and turn the machine to aim. Move, attack and hold orders no longer compete with stale driving targets; recruits resume following after a siege task.
- Recruit operators can be selected through the machine to open their inventory.
- Fixed a crash when opening siege commands with no selected engine type.
- Rams follow attack and stand orders instead of attacking by default.
- Supply containers and dismantling check access permissions. Commanders can recall their crew from foreign machines, and carried ladders can be set up by the owner's side.
- Enemy siege engines only appear on the RTS map when scouted.
- Smoother aim and driving synchronization, including the local operator's predicted controls.
- Draft mounts collide with terrain during towing and follow ground height at their hitch position.
- Mangonel release timing follows the throwing arm. Incendiary pots render correctly when loaded and in flight.
- Restored staged construction for Mangonel and Trebuchet.
- Corrected operator hand positions and upper-body leaning poses.
- Shots continue through loaded chunks outside simulation distance instead of hanging in the air. Flight visuals follow the server's unclipped launch velocity.

## 0.1.0-beta.5

### Added

- Siege controls can now be issued by machine type from the Recruits command menu without looking at a machine.

### Fixed

- The crew command no longer sends every recruit in the selected groups to the same machine.
- Fixed a client crash with mods that replace living entity renderers.

## 0.1.0-beta.4

### Fixed

- Siege projectiles can now hit multipart targets such as the Ender Dragon.
- Bolts no longer stop in mid-air after killing a target they can penetrate.
- Bolts embedded in living targets now stay attached to the part they hit.

## 0.1.0-beta.3

### Changed

- Updated the required Axiomata version to 0.1.0-beta.2.

## 0.1.0-beta.2

### Changed

- Replaced the procedural maintenance panel with a Minecraft-style GUI.
- Returned Mons Meg to one centered draft mount and restored its previous towing distance.

## 0.1.0-beta.1

### Added

- Initial public beta.
