# ElytraCourse

A Paper plugin for creating time-trial Elytra courses

## Features
- **Start Plate**: Step on a pressure plate to auto-equip elytra and start the timer.
- **Strict Collision**: Any collision with a solid block (walls, floors) resets the run.
- **Boost Rings**: Fly through marked blocks to gain a speed boost. Visual particles indicate strength!
- **Leaderboards**: Track personal bests and top world records using PlaceholderAPI.
- **Tools**: Easy-to-use items for setting up the course without memorizing coordinates.

## Commands
* `/ec setstartplate` - Look at a pressure plate to set it as the start point.
* `/ec setstartloc` - Set where the player teleports to when the run begins.
* `/ec setresetloc` - Set where players respawn if they crash.
* `/ec setfinishloc` - Set the "Winner's Podium" location.
* `/ec deletecourse` - Completely wipe the course data for the current world.
* `/ec tools [player]` - Get the setup toolset (Mark/Unmark blocks).
* `/ec help` - Show a list of all commands.
* `/ec reload` - Reload the configuration file.

## Setup Guide
1. **Prepare the World**: Build your course!
2. **Set the Start**: 
   - Place a pressure plate.
   - Run `/ec setstartplate` while looking at it.
   - Fly to the exact spot you want players to launch from. Run `/ec setstartloc`.
3. **Set the Fail/Win Points**:
   - Go to your "Checkpoints" or respawn area. Run `/ec setresetloc`.
   - Go to the finish line area. Run `/ec setfinishloc`.
4. **Mark Special Blocks**:
   - Run `/ec tools`.
   - **Right-Click** blocks with the tools to mark them.
   - **Boost Blocks**: Left-click the air with the "Mark Boost" tool to cycle speed strength (Green = Slow, Red = Fast).
   - **Exception Blocks**: Use these on platforms you want players to be able to land on safely.
   - **Win Blocks**: Mark the end-gate. Hitting this stops the timer.

## Leaderboards (PlaceholderAPI)
You can use these placeholders in holograms (DecentHolograms) or chat.

| Placeholder | Description |
|---|---|
| `%elytracourse_last_time%` | The time of the player's most recent run |
| `%elytracourse_best_time%` | Personal best time for the current world |
| `%elytracourse_best_time_<world>%` | PB for a specific world |
| `%elytracourse_top_name_<rank>_<world>%` | Name of the player at #rank (e.g. #1) |
| `%elytracourse_top_time_<rank>_<world>%` | Time of the player at #rank |

**Example Hologram:**
```
Top Runners:
#1 %elytracourse_top_name_1_world% - %elytracourse_top_time_1_world%
#2 %elytracourse_top_name_2_world% - %elytracourse_top_time_2_world%
#3 %elytracourse_top_name_3_world% - %elytracourse_top_time_3_world%
```

