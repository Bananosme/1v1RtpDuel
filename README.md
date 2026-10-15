# ⚔️ 1v1RtpDuel

> **Random arenas, zero building!**
> Two players queue with `/1v1`, get teleported to a random spot in a random world and fight. No arenas to build — the whole world is the arena.

---

## ✨ Features

- 🎲 **Random arena** — a random world is picked from the config for every fight
- 🧭 **Safe spawns** — players never appear in water, lava or inside blocks
- 📊 **Queue BossBar** — live queue position, colour and style configurable
- 🎯 **Nothing hangs** — if no spot is found, players get a message instead of waiting forever
- 🌍 **`/rtp` bonus** — random teleport with a customisable world-picker GUI
- 💚 **Fair start** — health, food, effects and XP reset before the fight, inventory untouched
- 💀 **Death = respawn** — losers respawn in `spawn-world`
- 🔗 **Kit agnostic** — kits, `/back` and `/spawn` stay the job of your other plugins
- 🎨 **Customization** — every message in the config
- 📦 **bStats** — anonymous statistics collection

---

## 🎮 Commands

| Command | Aliases | Permission | Default |
|---------|---------|------------|---------|
| `/1v1` | `/duel` | `rtpduel.1v1` | everyone |
| `/1v1_leave` | `/leave1v1`, `/duel_leave` | `rtpduel.1v1` | everyone |
| `/rtp` | `/randomtp` | `rtpduel.rtp` | everyone |

---

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `rtpduel.1v1` | Access to `/1v1` and `/1v1_leave` | `everyone` |
| `rtpduel.rtp` | Access to `/rtp` | `everyone` |

---

## ⚙️ Config (the important bits)

```yaml
# Defaults for worlds
default-border: 10000        # random coordinate radius (± blocks on X and Z)
default-spawn-distance: 8    # distance between the fighters

# Arena worlds: one is picked at random when a match starts
worlds:
  world_1v1:
    border: 10000            # this world only
    spawn-distance: 8
  world_1v1_2:
    border: 5000
    spawn-distance: 12

# Where players who die in an arena respawn
spawn-world: "world"

# Queue bossbar
bossbar-color: GREEN         # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
bossbar-style: SOLID         # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
```

### 🌍 /rtp GUI

```yaml
rtp:
  gui:
    title: "<green>Pick a world for RTP"
    size: 27
    worlds:
      world:
        slot: 11
        material: GRASS_BLOCK
        name: "<green>Overworld"
        lore:
          - "<gray>Click to teleport"
```

All messages live under `messages` and support the placeholders
`{position}`, `{opponent}`, `{worlds}`, `{permission}`, `{world}`, `{x}`, `{y}`, `{z}`.

> ⚠️ **`border` is not a WorldBorder.** It only defines the radius used to roll coordinates.
> To actually lock an arena down, use a separate plugin or the world's own border.

---

## 📋 Compatibility

- **Minecraft:** 1.21+ (tested on 1.21.11)
- **Servers:** Paper, Purpur (Paper forks)
- **Java:** 21+
- **Not supported:** Spigot/Bukkit (Paper API), Folia (global scheduler)
- **Optional:** kit plugin, `/back`, `/spawn` for post-fight logic

---

## 📦 bStats

The plugin collects anonymous statistics (server count, player count, plugin version, OS) — see [bstats.org](https://bstats.org/).
The data is fully anonymous and can be disabled with one line in `plugins/bStats/config.yml` (`enabled: false`).

---

## 📄 License

[GNU General Public License v3.0](LICENSE).

---

<details>
<summary>RU text</summary>

# ⚔️ 1v1RtpDuel

> **Случайные арены — строить ничего не надо!**
> Двое игроков встают в очередь по `/1v1`, их телепортирует на случайную точку в случайном мире, и они сражаются. Арены не нужны — ареной становится весь мир.

---

## ✨ Особенности

- 🎲 **Случайная арена** — для каждого боя выбирается случайный мир из конфига
- 🧭 **Безопасный спавн** — игроки не появляются в воде, лаве или внутри блоков
- 📊 **BossBar очереди** — позиция в очереди в реальном времени, цвет и стиль настраиваются
- 🎯 **Никаких зависаний** — если место не найдено, игрок получит сообщение, а не будет ждать вечно
- 🌍 **Бонус `/rtp`** — случайная телепортация с настраиваемым GUI выбора мира
- 💚 **Честный старт** — хп, голод, эффекты и опыт сбрасываются, инвентарь не трогается
- 💀 **Смерть = респавн** — проигравший возрождается в `spawn-world`
- 🔗 **Без системы китов** — киты, `/back` и `/spawn` остаются за другими плагинами
- 🎨 **Кастомизация** — все сообщения в конфиге
- 📦 **bStats** — сбор анонимной статистики

---

## 🎮 Команды

| Команда | Алиасы | Право | По умолчанию |
|---------|--------|-------|--------------|
| `/1v1` | `/duel` | `rtpduel.1v1` | всем |
| `/1v1_leave` | `/leave1v1`, `/duel_leave` | `rtpduel.1v1` | всем |
| `/rtp` | `/randomtp` | `rtpduel.rtp` | всем |

---

## 🔐 Права

| Право | Описание | По умолчанию |
|-------|----------|--------------|
| `rtpduel.1v1` | Доступ к `/1v1` и `/1v1_leave` | `всем` |
| `rtpduel.rtp` | Доступ к `/rtp` | `всем` |

---

## ⚙️ Конфиг (главное)

```yaml
# Значения по умолчанию для миров
default-border: 10000        # радиус случайных координат (± блоков по X и Z)
default-spawn-distance: 8    # расстояние между бойцами

# Миры-арены: при старте матча выбирается один случайный
worlds:
  world_1v1:
    border: 10000            # только для этого мира
    spawn-distance: 8
  world_1v1_2:
    border: 5000
    spawn-distance: 12

# Куда респавнятся умершие на арене
spawn-world: "world"

# Босбар очереди
bossbar-color: GREEN         # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
bossbar-style: SOLID         # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
```

### 🌍 GUI команды /rtp

```yaml
rtp:
  gui:
    title: "<green>Выбор мира для RTP"
    size: 27
    worlds:
      world:
        slot: 11
        material: GRASS_BLOCK
        name: "<green>Обычный мир"
        lore:
          - "<gray>Кликните, чтобы телепортироваться"
```

Все сообщения живут в секции `messages` и поддерживают плейсхолдеры
`{position}`, `{opponent}`, `{worlds}`, `{permission}`, `{world}`, `{x}`, `{y}`, `{z}`.

> ⚠️ **`border` — это не WorldBorder.** Это только радиус поиска координат.
> Чтобы реально закрыть арену, нужен отдельный плагин или граница самого мира.

---

## 📋 Совместимость

- **Minecraft:** 1.21+ (тестировалось на 1.21.11)
- **Серверы:** Paper, Purpur (форки Paper)
- **Java:** 21+
- **Не работает:** Spigot/Bukkit (Paper API), Folia (глобальный шедулер)
- **Опционально:** плагин китов, `/back`, `/spawn` для логики после боя

---

## 📦 bStats

Плагин собирает анонимную статистику (количество серверов, игроков, версия плагина, ОС) — см. [bstats.org](https://bstats.org/).
Данные полностью анонимны и отключаются одной строкой в `plugins/bStats/config.yml` (`enabled: false`).

---

## 📄 Лицензия

[GNU General Public License v3.0](LICENSE).

</details>
