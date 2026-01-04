# FOP Tron (WIX1002)

A Tron-inspired 2D Light Cycle arena game built with **Java + Spring Boot + Thymeleaf + WebSockets**.

## What’s implemented (matches the assignment requirements)

- **40×40 arena grid** with **WASD** movement.
- **Jetwall trails** left behind by every cycle (impassable).
- **Collisions**
  - Jetwall / wall / boundary: **-0.5 lives**
  - Open arena fall-off: **lose all remaining lives**
  - Disc hit: **-1 life**
- **Playable characters loaded from file**: Tron and Kevin (see `src/main/resources/data/characters.txt`).
- **Leveling system** to level 99 with **custom per-character algorithms**.
  - +1 life every 10 levels
  - +1 disc slot every 15 levels
- **Enemies loaded from file** (see `src/main/resources/data/enemies.txt`) with 4 types and simple AI behavior.
- Each match spawns **7 enemies**, randomized positions.
- **Disc system**
  - Throw range up to **3 grid units**
  - Cooldown decreases as handling improves
  - Discs land and can be recaptured by same-color owners
- **Story progression** loaded from file (see `src/main/resources/data/story.txt`).
- **3 predefined arenas + random arena**
  - Predefined: `arena1.txt`, `arena2.txt`, `arena3.txt`
  - Random: `RANDOM` and `OPEN_RANDOM`
- **Save + Leaderboard** (File I/O)
  - Save JSON to `./saves/<player>.json`
  - Leaderboard CSV in `./leaderboard.csv` (Top 10 shown in UI)

## Run

### Prerequisites
- JDK **17+**
- Maven (`mvn`) available on PATH

### Start server
From the project root:

```bash
mvn spring-boot:run
```

If you don’t have Maven installed globally, you can use the locally downloaded Maven (Windows):

```bash
.\.tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Open:
- `http://localhost:8080`

## Deploy to Render (Docker)

This repo includes a [Dockerfile](Dockerfile) and a [render.yaml](render.yaml) blueprint.

- In Render: **New +** → **Blueprint** → select this repo.
- Or: **New +** → **Web Service** → pick repo → set **Environment** to **Docker**.

Notes:
- The app binds to Render’s port via `server.port=${PORT:8080}`.
- `./saves/` and `./leaderboard.csv` are created at runtime as needed, but on Render they are **ephemeral** unless you attach a persistent disk.

## Controls
- `W` `A` `S` `D` — move
- `Space` — throw disc
- `R` — restart round (starts a new match)

## Data files (edit these to customize the game)

- Characters: `src/main/resources/data/characters.txt`
- Enemies: `src/main/resources/data/enemies.txt`
- Story: `src/main/resources/data/story.txt`
- Arenas:
  - `src/main/resources/data/arenas/arena1.txt`
  - `src/main/resources/data/arenas/arena2.txt`
  - `src/main/resources/data/arenas/arena3.txt`

Arena symbols:
- `#` wall
- `X` obstacle
- `R` speed ramp
- `.` empty

## Code structure
- Server-side engine: `src/main/java/com/foptron/game`
- Web + WebSocket endpoints: `src/main/java/com/foptron/web`

## Notes
- This project is designed for clarity and marking criteria: file I/O, OOP structure, collisions, leveling, and a playable arena loop.
- If you want Swing/ASCII instead of web UI, the engine layer is already separated under `com.foptron.game`.
