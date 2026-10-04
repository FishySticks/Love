# Alemanti Client — Fabric 1.21.1

Original Lunar-style PvP client. Developer: **svbx**.

## Build the jar (needs Java 21 + internet)

    gradle build            # or push to GitHub: Actions builds it for you
    -> build/libs/alemanti-client-1.1.0.jar

Fabric API is bundled inside the jar. Players only need **Fabric Loader 0.16.5+ for 1.21.1**;
drop the jar into `.minecraft/mods`.

## Keys
- Right Shift: module menu (left-click toggle, right-click settings)
- Right Ctrl: HUD editor (drag / right-click / scroll)
- C (hold): Zoom

## Mods
Visual: Nametags (own nametag + ping above the name), Motion Blur, Zoom, Fullbright, Low Fire, Time Changer
PvP: HurtCam (OFF/OLD/NEW + sensitivity)
Movement: Toggle Sprint
Misc: Nick Hider (nametag, tab list, game chat)
HUD: FPS, CPS, Ping, Coordinates, Armor, Potions, Keystrokes, Clock, Memory,
Direction, Speed, Server, Combo, Reach, Pot Counter, HUD Style (shadow/background)

Settings save to `config/alemanti-modules.json` and `config/alemanti-hud.json`.

## Not implemented yet
Saturation shader, wings/cape cosmetics, Discord RPC (needs a Discord Application ID).
