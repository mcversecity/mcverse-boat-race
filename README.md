# McvBoatRace

Repeatable ice boat races for Spigot 26.2. Players queue from a spectator area, get locked into start boats for a 3, 2, 1, START countdown, then follow checkpoints to a finish line. Leaving the ice sends that same boat back to the nearest safe spot.

## Requirements

- Spigot or a Spigot-compatible server for Minecraft 26.2
- Java 25
- WorldGuard 7.0.19 is optional. When it is installed, each arena box is also saved as a WorldGuard region.

## Install

Build the plugin:

```
mvn package
```

Copy `target/mcverse-boat-race-1.0.0.jar` into the server `plugins` folder and restart. Arenas are stored in `plugins/McvBoatRace/arenas.yml`. New arenas copy their lobby size, minimum players, time limit, and lap count from `plugins/McvBoatRace/config.yml`.

## Set up an arena

All setup commands need `mcvboatrace.admin` (operators have this by default). Replace `<arena>` with a short name such as `iceway`.

1. `/mcvboatrace create <arena>`
2. `/mcvboatrace wand`
3. Fly or stand at one corner and run `/mcvboatrace pos1`. Go to the opposite corner and run `/mcvboatrace pos2`. Both save your position, rounded up to the next whole block. The wand still uses the block you click.
4. Stand where finishers should return and run `/mcvboatrace setspectator`.
5. Select the stands and run `/mcvboatrace setspectatearea`. Players can only join from inside this box.
6. Select the ice, including the air above it, and run `/mcvboatrace settrack`. The boat has to be inside this box or it counts as off track.
7. Select the finish line and run `/mcvboatrace setfinish`.
8. Select each gate in driving order and run `/mcvboatrace addcheckpoint` for each one.
9. Stand on each start spot, look down the track, and run `/mcvboatrace setslot <n>`. Slots must be on ice and inside the track.
10. `/mcvboatrace info` lists anything still missing.

A race will not start until the spectator spawn, spectator area, track, finish, at least one checkpoint, and at least one start slot are set.

WorldGuard region ids look like `mcvbr_<arena>_spectate`, `mcvbr_<arena>_track`, `mcvbr_<arena>_finish`, and `mcvbr_<arena>_cp1`. The plugin keeps its own copy of the boxes, so the race still runs if WorldGuard is not installed.

## Run a race

Players need `mcvboatrace.join`, which is granted to everyone by default.

- Join from inside the spectator area with `/mcvboatrace join [arena]`, or right-click a sign whose first line is `[mcvboatrace]`. The second line can name the arena.
- Once enough players are queued, a lobby countdown starts. The default wait is 20 seconds. Filling every start slot shortens it to 5 seconds. Extra players wait for the next race.
- Racers are placed in one oak boat each, in join order, and held on the start line for **3**, **2**, **1**, **START**. The titles and boss bar are also shown to anyone standing in the spectator area.
- Checkpoints count only in order. The finish line counts after the last checkpoint of the current lap. That crossing completes the lap. Racers pass the checkpoints again for every remaining lap, and the last crossing ends the race. One lap is the default. Finishers are sent back to the spectator spawn and their boat is removed.
- Anyone still racing when the time limit ends (5 minutes by default) is marked DNF.
- `/mcvboatrace start [arena]` skips the lobby if someone is already queued. `/mcvboatrace stop [arena]` ends the race and removes the boats. `/mcvboatrace leave` drops you from the queue, or out of a race that has already started.

Going outside the track, or off the ice, waits about half a second and then moves that same boat to the newest saved spot that is still on the ice. If none of those spots work, the boat goes back to the last checkpoint the racer passed, or to their start slot.

Each racer has one boat for the whole race. The boat is removed when they finish, leave, disconnect, die, or the race stops. Boats left behind by a crash are removed when the plugin starts and when their chunk loads.

## Commands

`/mcvboatrace help` prints this list in game.

| Command | What it does |
| --- | --- |
| `/mcvboatrace join [arena]` | Queue for a race from the spectator area |
| `/mcvboatrace leave` | Leave the queue or the current race |
| `/mcvboatrace create <arena>` | Create an arena and select it |
| `/mcvboatrace select <arena>` | Choose the arena that setup commands edit |
| `/mcvboatrace delete <arena>` | Stop that arena, if needed, and delete it |
| `/mcvboatrace list` | List arenas and whether a race is running |
| `/mcvboatrace info [arena]` | Show setup progress |
| `/mcvboatrace wand` | Give the corner-selection wand |
| `/mcvboatrace pos1` / `pos2` | Set a corner to your position, rounded up to a whole block |
| `/mcvboatrace setspectator` | Save the return point where you are standing |
| `/mcvboatrace setspectatearea` | Save the selected box as the stands |
| `/mcvboatrace settrack` | Save the selected box as the legal ice corridor |
| `/mcvboatrace setfinish` | Save the selected box as the finish line |
| `/mcvboatrace addcheckpoint` | Append the selected box as the next checkpoint |
| `/mcvboatrace removecheckpoint <n>` | Remove checkpoint `n` and renumber the rest |
| `/mcvboatrace clearcheckpoints` | Remove every checkpoint |
| `/mcvboatrace listcheckpoints` | List checkpoint boxes |
| `/mcvboatrace setslot <n>` | Save a facing start slot where you are standing |
| `/mcvboatrace delslot <n>` | Remove a start slot |
| `/mcvboatrace listslots` | List start slots |
| `/mcvboatrace setminplayers <n>` | Set how many players the lobby waits for |
| `/mcvboatrace setlobby <seconds>` | Set the lobby wait, from 5 to 300 seconds |
| `/mcvboatrace setmaxtime <seconds>` | Set the race time limit, from 30 to 3600 seconds |
| `/mcvboatrace setlaps <n>` | Set how many laps the finish line takes, from 1 to 64 |
| `/mcvboatrace start [arena]` | Start the countdown immediately |
| `/mcvboatrace stop [arena]` | Stop the race and remove its boats |
| `/mcvboatrace reload` | Reload config and arenas that are not currently racing |

Stop a race before changing its course. Arena names are 1 to 24 characters and may use letters, numbers, underscores, and hyphens.

## Config

`config.yml` supplies the defaults copied onto an arena at creation. Change an existing arena with `setlobby`, `setminplayers`, `setmaxtime`, and `setlaps`.

| Setting | Default | Meaning |
| --- | --- | --- |
| `lobby-seconds` | 20 | Lobby wait for a new arena |
| `full-lobby-seconds` | 5 | Wait used when every start slot is filled |
| `min-players` | 1 | Players required before the lobby counts down |
| `max-race-seconds` | 300 | Time before remaining racers are DNF |
| `laps` | 1 | Laps copied onto a new arena. The finish line closes each lap |
| `results-seconds` | 5 | How long results stay up before the next queue |
| `off-track-grace-ticks` | 10 | Ticks off the ice before a boat is moved back |
| `recovery-cooldown-ticks` | 20 | Ticks after a recovery before it can happen again |
| `trail-samples` | 40 | Saved on-track positions used for recovery |
| `trail-interval-ticks` | 5 | How often a new trail position is saved |
| `boat-type` | OAK | Boat type spawned for racers |
| `ice-blocks` | ice types | Blocks that count as the track surface |

## Permissions

| Permission | Default | What it allows |
| --- | --- | --- |
| `mcvboatrace.join` | everyone | Join and leave |
| `mcvboatrace.admin` | operators | Create arenas, edit them, and start or stop races |

## Tests

```
mvn test
```

The tests cover checkpoint order, the off-track grace timer, and which saved position is chosen for a recovery.
