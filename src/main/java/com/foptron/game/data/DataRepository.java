package com.foptron.game.data;

import com.foptron.game.model.Arena;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class DataRepository {

    private final Map<String, CharacterDefinition> characters;
    private final Map<String, EnemyDefinition> enemies;
    private final Map<String, Arena> arenas;
    private final List<StoryChapter> story;
    private final Map<String, AchievementDefinition> achievements;

    public DataRepository() {
        this.characters = Collections.unmodifiableMap(loadCharacters());
        this.enemies = Collections.unmodifiableMap(loadEnemies());
        this.arenas = Collections.unmodifiableMap(loadArenas());
        this.story = List.copyOf(loadStory());
        this.achievements = Collections.unmodifiableMap(loadAchievements());
    }

    public Map<String, CharacterDefinition> characters() {
        return characters;
    }

    public Map<String, EnemyDefinition> enemies() {
        return enemies;
    }

    public Map<String, Arena> arenas() {
        return arenas;
    }

    public List<StoryChapter> story() {
        return story;
    }

    public Map<String, AchievementDefinition> achievements() {
        return achievements;
    }

    private Map<String, CharacterDefinition> loadCharacters() {
        List<String> lines = readResourceLines("data/characters.txt");
        Map<String, CharacterDefinition> map = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            if (line.startsWith("id,")) continue;
            String[] p = line.split(",");
            if (p.length < 8) continue;
            CharacterDefinition def = new CharacterDefinition(
                    p[0].trim(),
                    p[1].trim(),
                    p[2].trim(),
                    Double.parseDouble(p[3].trim()),
                    Double.parseDouble(p[4].trim()),
                    Double.parseDouble(p[5].trim()),
                    Integer.parseInt(p[6].trim()),
                    Long.parseLong(p[7].trim())
            );
            map.put(def.id().toUpperCase(), def);
        }
        return map;
    }

    private Map<String, EnemyDefinition> loadEnemies() {
        List<String> lines = readResourceLines("data/enemies.txt");
        Map<String, EnemyDefinition> map = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            if (line.startsWith("id,")) continue;
            String[] p = line.split(",");
            if (p.length < 9) continue;
            EnemyDefinition def = new EnemyDefinition(
                    p[0].trim(),
                    p[1].trim(),
                    p[2].trim(),
                    p[3].trim(),
                    Long.parseLong(p[4].trim()),
                    Double.parseDouble(p[5].trim()),
                    Double.parseDouble(p[6].trim()),
                    Double.parseDouble(p[7].trim()),
                    p[8].trim()
            );
            map.put(def.id().toUpperCase(), def);
        }
        return map;
    }

    private Map<String, Arena> loadArenas() {
        Map<String, Arena> map = new LinkedHashMap<>();
        map.put("ARENA1", ArenaLoader.load("ARENA1", "data/arenas/arena1.txt"));
        map.put("ARENA2", ArenaLoader.load("ARENA2", "data/arenas/arena2.txt"));
        map.put("ARENA3", ArenaLoader.load("ARENA3", "data/arenas/arena3.txt"));
        map.put("RANDOM", ArenaLoader.random("RANDOM", "Procedural Grid", false, new Random()));
        map.put("OPEN_RANDOM", ArenaLoader.random("OPEN_RANDOM", "Open Procedural Grid", true, new Random()));
        return map;
    }

    private List<StoryChapter> loadStory() {
        List<String> lines = readResourceLines("data/story.txt");
        List<StoryChapter> chapters = new ArrayList<>();

        String id = null;
        String title = null;
        int unlockLevel = 1;
        String unlockOnDefeat = null;
        StringBuilder text = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("[CHAPTER")) {
                id = attr(line, "id", "CH?" + chapters.size());
                title = attr(line, "title", id);
                unlockLevel = Integer.parseInt(attr(line, "unlockLevel", "1"));
                unlockOnDefeat = attr(line, "unlockOnDefeat", "").trim();
                if (unlockOnDefeat.isBlank()) unlockOnDefeat = null;
                text.setLength(0);
                continue;
            }
            if (line.startsWith("[/CHAPTER]")) {
                if (id != null) {
                    chapters.add(new StoryChapter(id, title == null ? id : title, unlockLevel, unlockOnDefeat, text.toString().trim()));
                }
                id = null;
                title = null;
                unlockLevel = 1;
                unlockOnDefeat = null;
                text.setLength(0);
                continue;
            }
            if (id != null) {
                text.append(line).append('\n');
            }
        }

        chapters.sort(Comparator.comparingInt(StoryChapter::unlockLevel));
        return chapters;
    }

    private Map<String, AchievementDefinition> loadAchievements() {
        List<String> lines = readResourceLines("data/achievements.txt");
        Map<String, AchievementDefinition> out = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            if (line.startsWith("id,")) continue;

            String[] p = line.split(",", -1);
            if (p.length < 2) continue;
            String id = p[0].trim();
            String title = p[1].trim();
            String desc = p.length >= 3 ? p[2].trim() : "";
            if (id.isBlank() || title.isBlank()) continue;
            out.put(id.toUpperCase(), new AchievementDefinition(id, title, desc));
        }
        return out;
    }

    private static String attr(String header, String key, String defaultValue) {
        String[] parts = header.replace("[CHAPTER", "").replace("]", "").trim().split(" ");
        for (String p : parts) {
            if (p.startsWith(key + "=")) {
                return p.substring((key + "=").length()).replace('"', ' ').trim();
            }
        }
        return defaultValue;
    }

    private static List<String> readResourceLines(String path) {
        try (var in = DataRepository.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return br.lines().toList();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed reading resource: " + path, e);
        }
    }
}
