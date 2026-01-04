package com.foptron.web.api;

import com.foptron.game.data.DataRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MetaController {

    private final DataRepository data;

    public MetaController(DataRepository data) {
        this.data = data;
    }

    @GetMapping("/api/meta")
    public Map<String, Object> meta() {
        List<Map<String, Object>> characters = data.characters().values().stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.id(),
                        "displayName", c.displayName(),
                        "color", c.color(),
                        "speed", c.speed(),
                        "handling", c.handling(),
                        "lives", c.lives(),
                        "discsOwned", c.discsOwned(),
                        "experiencePoints", c.experiencePoints()
                ))
                .toList();

        List<Map<String, Object>> arenas = data.arenas().values().stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.id(),
                        "name", a.name(),
                        "open", a.isOpen()
                ))
                .toList();

        arenas = new java.util.ArrayList<>(arenas);
        arenas.add(0, Map.of(
                "id", "AUTO",
                "name", "AUTO (Story Schedule)",
                "open", false
        ));

        return Map.of(
                "characters", characters,
                "arenas", arenas,
                "size", 40
        );
    }
}
